package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.*;

import com.odontocare.agent.config.AgentProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import tools.jackson.databind.*;

class GroqLanguageModelClientTests {
  private HttpServer server;
  private GroqLanguageModelClient client;
  private final ObjectMapper mapper = new ObjectMapper();
  private final AtomicReference<JsonNode> payload = new AtomicReference<>();
  private int status = 200;
  private String response;
  private String retryAfter;

  @BeforeEach
  void setup() throws Exception {
    var config = new AgentProperties();
    config.setEnabled(true);
    config.setApiKey("gsk_unit_test_secret");
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/chat",
        exchange -> {
          assertThat(exchange.getRequestHeaders().getFirst("Authorization"))
              .isEqualTo("Bearer gsk_unit_test_secret");
          payload.set(
              mapper.readTree(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          if (retryAfter != null) exchange.getResponseHeaders().set("retry-after", retryAfter);
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    client =
        new GroqLanguageModelClient(
            config,
            mapper,
            URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat"));
  }

  @AfterEach
  void close() {
    server.stop(0);
  }

  @Test
  void sendsToolsWithoutReasoningAndParsesUsage() {
    response =
        """
        {"choices":[{"finish_reason":"tool_calls","message":{"content":null,"reasoning":"must not be retained","tool_calls":[{"id":"call1","type":"function","function":{"name":"consultar_servicios","arguments":"{\\\"search\\\":\\\"limpieza\\\"}"}}]}}],"usage":{"prompt_tokens":100,"completion_tokens":30}}
        """;
    var result =
        client.reply(
            List.of(Map.of("role", "user", "content", "Reserva limpieza")),
            new AgentToolDefinitions().all());
    assertThat(result.tools()).hasSize(1);
    assertThat(result.inputTokens()).isEqualTo(100);
    assertThat(result.outputTokens()).isEqualTo(30);
    assertThat(result.content()).doesNotContain("retained");
    assertThat(payload.get().path("model").asString()).isEqualTo("openai/gpt-oss-20b");
    assertThat(payload.get().path("include_reasoning").asBoolean()).isFalse();
    assertThat(payload.get().path("reasoning_effort").asString()).isEqualTo("low");
    assertThat(payload.get().path("parallel_tool_calls").asBoolean()).isFalse();
    assertThat(payload.get().has("api_key")).isFalse();
    assertThat(payload.get().has("response_format")).isFalse();
  }

  @Test
  void providerErrorsDoNotExposeSecretsOrRawBodies() {
    status = 401;
    response = "{\"message\":\"gsk_unit_test_secret private upstream detail\"}";
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOf(ModelFailure.class)
        .hasMessageNotContaining("gsk_")
        .hasMessageNotContaining("upstream");
  }

  @Test
  void rateLimitIsControlled() {
    status = 429;
    response = "{}";
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class, f -> assertThat(f.code()).isEqualTo("RATE_LIMIT"));
  }

  @Test
  void usesProviderRetryDelayWithoutExposingItsBody() {
    status = 429;
    response = "{\"message\":\"gsk_unit_test_secret upstream detail\"}";
    retryAfter = "7.5";
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class,
            f -> {
              assertThat(f.retryAfterSeconds()).isEqualTo(8);
              assertThat(f.getMessage()).doesNotContain("gsk_", "upstream");
            });
  }

  @Test
  void malformedRetryHeaderHasBoundedFallback() {
    status = 429;
    response = "{}";
    retryAfter = "invalid";
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class, f -> assertThat(f.retryAfterSeconds()).isEqualTo(30));
  }

  @Test
  void identifiesToolGenerationRejectionWithoutRetainingFailedGeneration() {
    status = 400;
    response =
        """
        {"error":{"code":"tool_use_failed","type":"invalid_request_error","message":"gsk_unit_test_secret private detail","failed_generation":"private reasoning and arguments"}}
        """;
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class,
            f -> {
              assertThat(f.code()).isEqualTo("TOOL_GENERATION");
              assertThat(f.diagnostics())
                  .containsEntry("http_status", 400)
                  .containsEntry("provider_code", "tool_use_failed");
              assertThat(f.diagnostics().toString())
                  .doesNotContain("gsk_", "private", "reasoning", "arguments");
            });
  }

  @Test
  void classifiesQuotaWithoutPersistingAccountOrBody() {
    status = 429;
    response =
        """
        {"error":{"code":"rate_limit_exceeded","type":"tokens","message":"Limit reached for account PRIVATE_ACCOUNT on tokens per day (TPD): Used 12345. gsk_unit_test_secret"}}
        """;
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class,
            f -> {
              assertThat(f.code()).isEqualTo("RATE_LIMIT");
              assertThat(f.diagnostics())
                  .containsEntry("limit_kind", "TOKENS_PER_DAY")
                  .containsEntry("http_status", 429);
              assertThat(f.diagnostics().toString())
                  .doesNotContain("PRIVATE_ACCOUNT", "12345", "gsk_");
            });
  }

  @Test
  void unknownProviderErrorRetainsOnlyStatusAndApprovedCategories() {
    status = 400;
    response =
        """
        {"error":{"code":"gsk_unit_test_secret","type":"private patient detail","message":"private"}}
        """;
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class,
            f -> {
              assertThat(f.code()).isEqualTo("PROVIDER_ERROR");
              assertThat(f.diagnostics())
                  .containsExactlyInAnyOrderEntriesOf(
                      Map.of("http_status", 400, "provider_code", "OTHER"));
            });
  }

  @Test
  void malformedUpstreamErrorStillRetainsHttpStatus() {
    status = 503;
    response = "private malformed body";
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class,
            f -> {
              assertThat(f.code()).isEqualTo("PROVIDER_UNAVAILABLE");
              assertThat(f.diagnostics()).containsEntry("http_status", 503);
              assertThat(f.diagnostics().toString()).doesNotContain("private");
            });
  }

  @Test
  void nonFiniteOrNonPositiveRetryHeaderUsesFallback() {
    status = 429;
    response = "{}";
    for (String header : List.of("NaN", "Infinity", "0", "-5")) {
      retryAfter = header;
      assertThatThrownBy(() -> client.reply(List.of(), List.of()))
          .isInstanceOfSatisfying(
              ModelFailure.class, f -> assertThat(f.retryAfterSeconds()).isEqualTo(30));
    }
  }

  @Test
  void truncatedOutputIsNotTreatedAsSuccess() {
    response =
        "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"Reserva"
            + " creada\"}}]}";
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class, f -> assertThat(f.code()).isEqualTo("OUTPUT_LIMIT"));
  }
}

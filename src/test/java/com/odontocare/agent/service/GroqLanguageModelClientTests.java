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
  void truncatedOutputIsNotTreatedAsSuccess() {
    response =
        "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"Reserva"
            + " creada\"}}]}";
    assertThatThrownBy(() -> client.reply(List.of(), List.of()))
        .isInstanceOfSatisfying(
            ModelFailure.class, f -> assertThat(f.code()).isEqualTo("OUTPUT_LIMIT"));
  }
}

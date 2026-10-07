package com.odontocare.kapso.service;

import static org.assertj.core.api.Assertions.*;

import com.odontocare.kapso.config.KapsoProperties;
import com.odontocare.whatsapp.model.WhatsAppMessage;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import tools.jackson.databind.*;

class KapsoSenderTests {
  HttpServer server;
  KapsoSender sender;
  final ObjectMapper mapper = new ObjectMapper();
  final AtomicReference<JsonNode> payload = new AtomicReference<>();
  int status = 200;
  String response;

  @BeforeEach
  void setup() throws Exception {
    var config = new KapsoProperties();
    config.setEnabled(true);
    config.setApiKey("kapso-private-test-key");
    config.setWebhookSecret("kapso-webhook-test-secret");
    config.setPhoneNumberId("123456789012345");
    config.setSender("+56920403095");
    config.setPublicBaseUrl("https://wa.example.test");
    config.setAllowedParticipants(List.of("+51999998888"));
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/123456789012345/messages",
        exchange -> {
          assertThat(exchange.getRequestMethod()).isEqualTo("POST");
          assertThat(exchange.getRequestHeaders().getFirst("X-API-Key"))
              .isEqualTo("kapso-private-test-key");
          assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isNull();
          payload.set(
              mapper.readTree(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
          byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    sender =
        new KapsoSender(
            config, mapper, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
  }

  @AfterEach
  void close() {
    server.stop(0);
  }

  WhatsAppMessage message() {
    return new WhatsAppMessage(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "OUTBOUND",
        "TEXT",
        "Hola mañana ñ 😀",
        null,
        "SENDING",
        Instant.now(),
        null,
        null,
        1,
        UUID.randomUUID(),
        null,
        "KAPSO");
  }

  @Test
  void sendsNativeTextPayloadAndKeepsAcceptanceSeparateFromDelivery() {
    response = "{\"messages\":[{\"id\":\"wamid.ABCDEF012345678901234567890123456789==\"}]}";
    var result = sender.send(message(), "+51999998888");
    assertThat(result.status()).isEqualTo("ACCEPTED");
    assertThat(result.reference()).startsWith("wamid.");
    assertThat(payload.get().path("to").asString()).isEqualTo("51999998888");
    assertThat(payload.get().path("text").path("body").asString()).isEqualTo("Hola mañana ñ 😀");
    assertThat(payload.get().path("type").asString()).isEqualTo("text");
    assertThat(payload.get().has("ContentSid")).isFalse();
  }

  @Test
  void authAndCreditFailuresDoNotExposeUpstreamBodies() {
    status = 401;
    response =
        "{\"error\":{\"code\":\"AUTH\",\"message\":\"kapso-private-test-key private details\"}}";
    var rejected = sender.send(message(), "+51999998888");
    assertThat(rejected.status()).isEqualTo("FAILED");
    assertThat(rejected.errorMessage()).doesNotContain("kapso-private-test-key", "private details");
    status = 402;
    response = "{\"code\":\"service_credits_exhausted\"}";
    assertThat(sender.send(message(), "+51999998888").errorCode())
        .isEqualTo("service_credits_exhausted");
  }

  @Test
  void rateLimitAndUncertainOutcomesNeverClaimDelivery() {
    status = 429;
    response = "{}";
    assertThat(sender.send(message(), "+51999998888").status()).isEqualTo("RATE_LIMIT");
    status = 500;
    assertThat(sender.send(message(), "+51999998888").status()).isEqualTo("UNKNOWN");
    status = 200;
    response = "{\"messages\":[{\"id\":\"invalid\"}]}";
    assertThat(sender.send(message(), "+51999998888").status()).isEqualTo("UNKNOWN");
  }
}

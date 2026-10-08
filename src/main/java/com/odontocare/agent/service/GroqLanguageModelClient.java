package com.odontocare.agent.service;

import com.odontocare.agent.config.AgentProperties;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class GroqLanguageModelClient implements LanguageModelClient {
  private final AgentProperties config;
  private final ObjectMapper mapper;
  private final URI endpoint;
  private final HttpClient client =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(4))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  @Autowired
  public GroqLanguageModelClient(AgentProperties config, ObjectMapper mapper) {
    this(config, mapper, URI.create("https://api.groq.com/openai/v1/chat/completions"));
  }

  GroqLanguageModelClient(AgentProperties config, ObjectMapper mapper, URI endpoint) {
    this.config = config;
    this.mapper = mapper;
    this.endpoint = endpoint;
  }

  public Reply reply(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
    if (!config.ready())
      throw new ModelFailure("CONFIGURATION", "Activa el agente y completa su clave privada.");
    var body = new LinkedHashMap<String, Object>();
    body.put("model", config.getModel());
    body.put("messages", messages);
    body.put("tools", tools);
    body.put("tool_choice", "auto");
    body.put("parallel_tool_calls", false);
    body.put("reasoning_effort", config.getReasoningEffort());
    body.put("temperature", 0);
    body.put("include_reasoning", false);
    body.put("max_completion_tokens", config.getMaxCompletionTokens());
    body.put("stream", false);
    var request =
        HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(config.getRequestTimeoutSeconds()))
            .header("Authorization", "Bearer " + config.getApiKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build();
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 401 || response.statusCode() == 403)
        throw new ModelFailure(
            "AUTHENTICATION",
            "Groq rechazó la clave o el permiso del modelo. Revisa la cuenta y reinicia el"
                + " backend.");
      if (response.statusCode() == 429)
        throw new ModelFailure(
            "RATE_LIMIT",
            "Groq limitó temporalmente las llamadas. Se conserva el análisis completado; revisa los"
                + " límites si persiste.",
            retryAfter(response));
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new ModelFailure(
            "PROVIDER_ERROR",
            "Groq rechazó la llamada. Comprueba el modelo y sus permisos; no se creó una cita por"
                + " esta respuesta.");
      var json = mapper.readTree(response.body());
      var choice = json.path("choices").path(0);
      if (choice.isMissingNode() || choice.path("finish_reason").asString("").equals("length"))
        throw new ModelFailure(
            "OUTPUT_LIMIT",
            "El modelo agotó la salida permitida. Revisa el límite de tokens y reintenta.");
      var message = choice.path("message");
      var calls = new ArrayList<ToolCall>();
      for (var call : message.path("tool_calls")) {
        if (!call.path("type").asString("").equals("function") || calls.size() >= 4)
          throw new ModelFailure(
              "INVALID_RESPONSE", "El modelo devolvió herramientas no admitidas.");
        calls.add(
            new ToolCall(
                call.path("id").asString(""),
                call.path("function").path("name").asString(""),
                call.path("function").path("arguments").asString("")));
      }
      String content = message.path("content").asString("");
      if (content.length() > 6000)
        throw new ModelFailure("INVALID_RESPONSE", "La respuesta excede el tamaño admitido.");
      return new Reply(
          content,
          List.copyOf(calls),
          Math.max(0, json.path("usage").path("prompt_tokens").asInt(0)),
          Math.max(0, json.path("usage").path("completion_tokens").asInt(0)));
    } catch (ModelFailure failure) {
      throw failure;
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new ModelFailure(
          "INTERRUPTED", "El procesamiento fue interrumpido; puede reintentarse.");
    } catch (java.net.http.HttpTimeoutException failure) {
      throw new ModelFailure(
          "TIMEOUT", "Groq no respondió a tiempo; puede reintentarse sin duplicar reservas.");
    } catch (Exception failure) {
      throw new ModelFailure(
          "CONNECTION",
          "No se pudo obtener una respuesta válida de Groq. Revisa conexión y configuración.");
    }
  }

  private long retryAfter(HttpResponse<?> response) {
    try {
      return (long)
          Math.ceil(Double.parseDouble(response.headers().firstValue("retry-after").orElse("30")));
    } catch (NumberFormatException ignored) {
      return 30;
    }
  }
}

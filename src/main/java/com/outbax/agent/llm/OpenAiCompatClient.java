package com.outbax.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class OpenAiCompatClient implements LlmClient {
  private static final ObjectMapper om = new ObjectMapper();

  private final HttpClient http;
  private final String baseUrl;
  private final String apiKey;
  private final String model;

  public OpenAiCompatClient(String baseUrl, String apiKey, String model, Duration timeout) {
    this.baseUrl = baseUrl.replaceAll("/+$", "");
    this.apiKey = apiKey;
    this.model = model;
    this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  @Override
  public String chat(List<ChatMessage> messages) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);

    List<Map<String, Object>> msg = new ArrayList<>();
    for (ChatMessage m : messages) {
      Map<String, Object> mm = new LinkedHashMap<>();
      mm.put("role", m.role());
      mm.put("content", m.content());
      if (m.name() != null) mm.put("name", m.name());
      msg.add(mm);
    }
    body.put("messages", msg);
    body.put("temperature", 0.4);

    String json = om.writeValueAsString(body);

    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/chat/completions"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json));

    if (apiKey != null && !apiKey.isBlank()) {
      b.header("Authorization", "Bearer " + apiKey.trim());
    }

    HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() / 100 != 2) {
      throw new RuntimeException("LLM HTTP " + resp.statusCode() + ": " + resp.body());
    }

    JsonNode root = om.readTree(resp.body());
    JsonNode content = root.at("/choices/0/message/content");
    if (content.isMissingNode()) {
      return resp.body();
    }
    return content.asText();
  }
}

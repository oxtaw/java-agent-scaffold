package com.outbax.agent.tools;

import com.outbax.agent.core.Tool;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

public class HttpFetchTool implements Tool {
  private final HttpClient http;

  public HttpFetchTool(Duration timeout) {
    this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  @Override public String name() { return "http.fetch"; }
  @Override public String description() { return "Fetch a URL (GET) and return first N chars."; }
  @Override public Map<String, String> schema() {
    return Map.of(
        "url", "string",
        "maxChars", "int (default 2000)"
    );
  }

  @Override
  public String run(Map<String, Object> args) throws Exception {
    String url = String.valueOf(args.get("url"));
    int max = 2000;
    if (args.get("maxChars") instanceof Number n) max = n.intValue();

    HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .header("User-Agent", "java-agent/0.1")
        .build();

    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
    String body = resp.body();
    if (body.length() > max) body = body.substring(0, max);
    return "HTTP " + resp.statusCode() + "\n" + body;
  }
}

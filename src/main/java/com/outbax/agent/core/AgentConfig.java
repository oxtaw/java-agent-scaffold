package com.outbax.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record AgentConfig(
    String baseUrl,
    String apiKey,
    String model,
    int timeoutSeconds,
    String memoryFile,
    int maxTurns
) {
  public static AgentConfig defaults() {
    return new AgentConfig(
        "http://127.0.0.1:8317/v1",
        "",
        "gpt-5.2",
        120,
        "memory.json",
        4
    );
  }

  public static AgentConfig load(Path path) throws IOException {
    ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    if (!Files.exists(path)) {
      AgentConfig d = defaults();
      Files.writeString(path, om.writeValueAsString(d));
      return d;
    }
    return om.readValue(Files.readString(path), AgentConfig.class);
  }
}

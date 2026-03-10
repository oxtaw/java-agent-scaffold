package com.outbax.agent.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public record ToolCall(String tool, Map<String, Object> args) {
  private static final ObjectMapper om = new ObjectMapper();

  public static ToolCall tryParse(String text) {
    // Expect exact JSON object in the whole output, or a fenced ```toolcall block.
    String trimmed = text.trim();
    String json = null;

    if (trimmed.startsWith("```")) {
      int i = trimmed.indexOf("\n");
      int j = trimmed.lastIndexOf("```");
      if (i > 0 && j > i) {
        String header = trimmed.substring(0, i).toLowerCase();
        if (header.contains("tool")) {
          json = trimmed.substring(i + 1, j).trim();
        }
      }
    }

    if (json == null && trimmed.startsWith("{") && trimmed.endsWith("}")) {
      json = trimmed;
    }

    if (json == null) return null;

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> m = om.readValue(json, Map.class);
      Object tool = m.get("tool");
      Object args = m.getOrDefault("args", Map.of());
      if (!(tool instanceof String)) return null;
      if (!(args instanceof Map)) return null;
      @SuppressWarnings("unchecked")
      Map<String, Object> a = (Map<String, Object>) args;
      return new ToolCall((String) tool, a);
    } catch (Exception e) {
      return null;
    }
  }
}

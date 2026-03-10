package com.outbax.agent.llm;

public record ChatMessage(String role, String content, String name) {
  public static ChatMessage user(String c) { return new ChatMessage("user", c, null); }
  public static ChatMessage assistant(String c) { return new ChatMessage("assistant", c, null); }
  public static ChatMessage tool(String name, String c) { return new ChatMessage("tool", c, name); }
}

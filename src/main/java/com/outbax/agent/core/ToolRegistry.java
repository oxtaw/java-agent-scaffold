package com.outbax.agent.core;

import java.util.*;

public class ToolRegistry {
  private final Map<String, Tool> tools = new LinkedHashMap<>();

  public void register(Tool t) {
    tools.put(t.name(), t);
  }

  public String toolsPrompt() {
    StringBuilder sb = new StringBuilder();
    sb.append("Available tools (call by emitting JSON: {\"tool\":\"name\",\"args\":{...}}):\n");
    for (Tool t : tools.values()) {
      sb.append("- ").append(t.name()).append(": ").append(t.description()).append("\n");
    }
    return sb.toString();
  }

  public ToolResult invoke(ToolCall call) throws Exception {
    Tool t = tools.get(call.tool());
    if (t == null) {
      return new ToolResult(call.tool(), "ERROR: unknown tool");
    }
    String out = t.run(call.args());
    return new ToolResult(t.name(), out);
  }
}

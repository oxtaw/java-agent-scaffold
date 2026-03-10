package com.outbax.agent.tools;

import com.outbax.agent.core.Tool;

import java.time.ZonedDateTime;
import java.util.Map;

public class TimeTool implements Tool {
  @Override public String name() { return "time.now"; }
  @Override public String description() { return "Get current datetime in ISO-8601."; }
  @Override public Map<String, String> schema() { return Map.of(); }

  @Override
  public String run(Map<String, Object> args) {
    return ZonedDateTime.now().toString();
  }
}

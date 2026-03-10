package com.outbax.agent.core;

import java.util.Map;

public interface Tool {
  String name();
  String description();
  Map<String, String> schema();
  String run(Map<String, Object> args) throws Exception;
}

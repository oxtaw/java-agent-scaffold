package com.outbax.agent.llm;

import java.util.List;

public interface LlmClient {
  String chat(List<ChatMessage> messages) throws Exception;
}

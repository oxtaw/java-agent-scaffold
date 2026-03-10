package com.outbax.agent.memory;

import com.outbax.agent.llm.ChatMessage;

import java.util.List;

public interface MemoryStore {
  List<ChatMessage> load() throws Exception;
  void save(List<ChatMessage> messages) throws Exception;
}

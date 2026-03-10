package com.outbax.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.outbax.agent.llm.ChatMessage;

import java.nio.file.*;
import java.util.*;

public class JsonMemoryStore implements MemoryStore {
  private static final ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  private final Path path;

  public JsonMemoryStore(Path path) {
    this.path = path;
  }

  @Override
  public List<ChatMessage> load() throws Exception {
    if (!Files.exists(path)) return new ArrayList<>();
    ChatMessage[] arr = om.readValue(Files.readString(path), ChatMessage[].class);
    return new ArrayList<>(Arrays.asList(arr));
  }

  @Override
  public void save(List<ChatMessage> messages) throws Exception {
    Files.writeString(path, om.writeValueAsString(messages), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }
}

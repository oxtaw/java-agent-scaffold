package com.outbax.agent.core;

import com.outbax.agent.llm.*;
import com.outbax.agent.memory.*;

import java.util.ArrayList;
import java.util.List;

public class Agent {
  private final LlmClient llm;
  private final ToolRegistry tools;
  private final MemoryStore memory;
  private final AgentConfig cfg;

  public Agent(LlmClient llm, ToolRegistry tools, MemoryStore memory, AgentConfig cfg) {
    this.llm = llm;
    this.tools = tools;
    this.memory = memory;
    this.cfg = cfg;
  }

  /**
   * 处理一次用户输入。
   *
   * 当前实现是“极简 ReAct-like 循环”：
   * - 把 memory.json 里的历史加载进来
   * - 追加 user message
   * - 调用 LLM 生成
   * - 如果 LLM 输出的是 toolcall JSON，则执行工具，把工具结果再喂回对话
   * - 循环最多 maxTurns 次，避免死循环
   */
  public String respond(String userInput) throws Exception {
    List<ChatMessage> history = new ArrayList<>(memory.load());
    history.add(ChatMessage.user(userInput));

    for (int turn = 0; turn < cfg.maxTurns(); turn++) {
      String completion = llm.chat(history);

      // 约定：若模型输出纯 JSON（或 fenced tool block），则视为工具调用。
      ToolCall call = ToolCall.tryParse(completion);
      if (call == null) {
        history.add(ChatMessage.assistant(completion));
        memory.save(history);
        return completion;
      }

      ToolResult result = tools.invoke(call);

      // 这里用一个轻量标记，便于回放/排查：模型什么时候触发了什么工具。
      history.add(ChatMessage.assistant("TOOL_CALL " + call.tool()));
      history.add(ChatMessage.tool(result.tool(), result.output()));
    }

    String fallback = "I couldn't finish within maxTurns=" + cfg.maxTurns();
    memory.save(history);
    return fallback;
  }
}

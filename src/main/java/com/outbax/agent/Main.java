package com.outbax.agent;

import com.outbax.agent.core.*;
import com.outbax.agent.llm.*;
import com.outbax.agent.memory.*;
import com.outbax.agent.tools.*;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Scanner;

public class Main {
  /**
   * CLI 入口。
   *
   * 设计目标：
   * - 尽量少依赖框架（先跑通最小链路）
   * - 配置从 agent.json 读取（不存在则自动生成默认配置）
   * - 对话历史落盘到 memory.json（可配置）
   * - 默认对接本机 OpenAI 兼容网关： http://127.0.0.1:8317/v1
   */
  public static void main(String[] args) throws Exception {
    // 1) 加载配置（首次运行会创建 agent.json）
    AgentConfig cfg = AgentConfig.load(Path.of("agent.json"));

    // 2) 注册工具（后续可扩展更多 Tool）
    ToolRegistry tools = new ToolRegistry();
    tools.register(new TimeTool());
    tools.register(new HttpFetchTool(Duration.ofSeconds(20)));

    // 3) 初始化“记忆”（对话历史存 JSON 文件）
    MemoryStore memory = new JsonMemoryStore(Path.of(cfg.memoryFile()));

    // 4) 初始化 LLM 客户端（OpenAI 兼容 /v1/chat/completions）
    LlmClient llm = new OpenAiCompatClient(
        cfg.baseUrl(),
        cfg.apiKey(),
        cfg.model(),
        Duration.ofSeconds(cfg.timeoutSeconds())
    );

    // 5) Agent 核心：负责把用户输入 + memory + tool result 串起来
    Agent agent = new Agent(llm, tools, memory, cfg);

    System.out.println("Java Agent ready. Type 'exit' to quit.");
    System.out.println("LLM baseUrl=" + cfg.baseUrl() + " model=" + cfg.model());

    // 6) CLI 交互循环
    try (Scanner sc = new Scanner(System.in)) {
      while (true) {
        System.out.print("\nYou> ");
        String input = sc.nextLine();
        if (input == null) break;
        input = input.trim();
        if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) break;
        if (input.isEmpty()) continue;

        try {
          String out = agent.respond(input);
          System.out.println("\nAgent> " + out);
        } catch (Exception e) {
          System.err.println();
          System.err.println(formatError(e, cfg.baseUrl()));
          if (Boolean.getBoolean("agent.debugStacktrace")) {
            e.printStackTrace(System.err);
          }
        }
      }
    }
  }

  private static String formatError(Exception e, String baseUrl) {
    if (hasCause(e, ConnectException.class)) {
      return "[Error] Cannot reach LLM endpoint at " + baseUrl + ". Ensure the gateway is running or update baseUrl in agent.json. (" + rootSummary(e) + ")";
    }
    if (hasCause(e, HttpTimeoutException.class)) {
      return "[Error] LLM request to " + baseUrl + " timed out. Consider checking network connectivity or increasing timeoutSeconds in agent.json. (" + rootSummary(e) + ")";
    }
    return "[Error] Agent failed: " + rootSummary(e);
  }

  private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
    while (t != null) {
      if (type.isInstance(t)) return true;
      t = t.getCause();
    }
    return false;
  }

  private static String rootSummary(Throwable t) {
    Throwable root = t;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    String msg = root.getMessage();
    return root.getClass().getSimpleName() + (msg == null || msg.isBlank() ? "" : (": " + msg));
  }
}

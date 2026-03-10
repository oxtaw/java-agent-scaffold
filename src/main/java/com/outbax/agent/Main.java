package com.outbax.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.outbax.agent.cli.CliArgs;
import com.outbax.agent.core.*;
import com.outbax.agent.llm.*;
import com.outbax.agent.memory.*;
import com.outbax.agent.tools.*;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

public class Main {
  private static final ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

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
    CliArgs cli = CliArgs.parse(args);
    if (cli.has("-h") || cli.has("--help")) {
      System.out.println(CliArgs.usage());
      return;
    }

    // 1) 加载配置（首次运行会创建 agent.json）
    Path cfgPath = Path.of(cli.getOrDefault("--config", "agent.json"));
    AgentConfig fileCfg = AgentConfig.load(cfgPath);

    // 2) 叠加环境变量/CLI 覆盖（CLI > env > file）
    AgentConfig cfg = applyOverrides(fileCfg, cli);

    if (cli.has("--print-config")) {
      System.out.println(om.writeValueAsString(cfg));
      return;
    }

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
    String systemPrompt = buildSystemPrompt(tools, cli.get("--system-prompt-file"));
    Agent agent = new Agent(llm, tools, memory, cfg, systemPrompt);

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

        if (input.equals(":reset")) {
          memory.save(List.of());
          System.out.println("\n[Memory cleared] " + cfg.memoryFile());
          continue;
        }

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

  private static AgentConfig applyOverrides(AgentConfig fileCfg, CliArgs cli) {
    String baseUrl = firstNonBlank(cli.get("--base-url"), System.getenv("AGENT_BASE_URL"), fileCfg.baseUrl());
    String model = firstNonBlank(cli.get("--model"), System.getenv("AGENT_MODEL"), fileCfg.model());
    String apiKey = firstNonBlank(cli.get("--api-key"), System.getenv("AGENT_API_KEY"), fileCfg.apiKey());
    String memoryFile = firstNonBlank(cli.get("--memory-file"), System.getenv("AGENT_MEMORY_FILE"), fileCfg.memoryFile());

    int timeoutSeconds = firstInt(cli.get("--timeout-seconds"), System.getenv("AGENT_TIMEOUT_SECONDS"), fileCfg.timeoutSeconds());
    int maxTurns = firstInt(cli.get("--max-turns"), System.getenv("AGENT_MAX_TURNS"), fileCfg.maxTurns());

    return new AgentConfig(baseUrl, apiKey, model, timeoutSeconds, memoryFile, maxTurns);
  }

  private static String buildSystemPrompt(ToolRegistry tools, String systemPromptFile) throws Exception {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an assistant running inside a Java CLI agent.\n");
    sb.append("If you need to use a tool, you MUST output ONLY a JSON object like {\"tool\":\"time.now\",\"args\":{}} (no extra text).\n");
    sb.append("After a tool result is provided, continue the conversation and give the final answer in natural language.\n");
    sb.append("\n");
    sb.append(tools.toolsPrompt());

    if (systemPromptFile != null && !systemPromptFile.isBlank()) {
      Path p = Path.of(systemPromptFile);
      if (Files.exists(p)) {
        sb.append("\n\n[Extra instructions]\n");
        sb.append(Files.readString(p));
      } else {
        sb.append("\n\n[Note] systemPromptFile not found: ").append(p).append("\n");
      }
    }

    return sb.toString();
  }

  private static String firstNonBlank(String a, String b, String c) {
    if (a != null && !a.isBlank()) return a;
    if (b != null && !b.isBlank()) return b;
    return c;
  }

  private static int firstInt(String a, String b, int fallback) {
    String v = null;
    if (a != null && !a.isBlank()) v = a;
    else if (b != null && !b.isBlank()) v = b;
    if (v == null) return fallback;
    try {
      return Integer.parseInt(v.trim());
    } catch (Exception e) {
      return fallback;
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

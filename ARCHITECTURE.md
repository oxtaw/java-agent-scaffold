# java-agent-scaffold 设计说明

## 设计初衷：先让链路跑通
- **目标聚焦**：项目定位成“最小可跑通”的 Java Agent，优先把 *CLI 输入 → LLM → 工具调用 → 记忆落盘* 整条链路稳定跑通，方便在此基础上迭代 Planner、Router 等高级能力。
- **可移植性**：不依赖复杂框架，全部用 JDK 17 自带能力（`java.net.http`, `java.nio.file` 等），本地开箱即用，也便于嵌入到更大的系统。
- **透明可调**：所有运行参数（baseUrl、model、timeout、记忆文件等）集中在根目录 `agent.json`，默认首次运行自动生成，便于直接修改或纳入版本管理。
- **原子组件**：LLM 客户端、记忆、工具系统都通过接口拆开，后续替换（比如换 SDK、接入数据库、引入更多工具）无需重写主循环。

## 模块分层

```
Main (CLI) ──┐
             │  Agent (core loop) ─→ ToolRegistry ─→ Tool 实现
Config ──────┘            │
                          ├→ LlmClient (OpenAI 兼容 HTTP)
                          └→ MemoryStore (JSON 记忆)
```

### CLI & 配置 (`src/main/java/com/outbax/agent/Main.java`)
- 负责 CLI 交互、提示语和异常兜底，运行时先 `AgentConfig.load`（见 `core/AgentConfig.java`）从 `agent.json` 拉取配置，没有则写入默认值。
- 把 Time/HTTP 等工具注册进 `ToolRegistry`，构造 `JsonMemoryStore` 与 `OpenAiCompatClient`，最后创建 `Agent`。
- CLI 循环中，每次用户输入都交给 `agent.respond`，如果出现网络/超时等异常，会给出友好的诊断（`formatError`），并支持通过 `-Dagent.debugStacktrace=true` 打印完整堆栈。

### Agent 核心循环 (`core/Agent.java`)
- 读取 `MemoryStore` 历史消息，追加当前 user message，然后调用 `LlmClient.chat`。
- 如果模型输出纯文本，直接视为最终答复，写回记忆并返回。
- 如果输出 JSON/```tool``` 块，交给 `ToolCall.tryParse` 解析成 `ToolCall`，通过 `ToolRegistry.invoke` 执行，再把 `TOOL_CALL ...` 以及工具返回结果写回对话，继续下一轮，最多 `cfg.maxTurns` 次避免死循环。
- 这样就形成了极简 ReAct 流程，便于后来插入更复杂的 ReAct 状态机或 Planner。

### LLM 层 (`llm/*`)
- `LlmClient` 是最小接口：`chat(List<ChatMessage>) -> String`。
- `OpenAiCompatClient` 用 `java.net.http.HttpClient` 直接对接 OpenAI 兼容 `/v1/chat/completions`，body 中把 `ChatMessage(role, content, name)` 原样传过去，默认 `temperature=0.4`。
- 由于用标准 HTTP 客户端，替换为任意 REST 服务或添加重试、流式解析都很容易。

### 记忆模块 (`memory/*`)
- `MemoryStore` 抽象读写对话历史，当前实现 `JsonMemoryStore` 使用 `memory.json`（可配置）。
- 存储格式是 `ChatMessage` 数组，方便调试回放，也便于未来换成 SQLite/Redis 时只需替换实现。

### 工具系统 (`core/Tool.java` & `tools/*`)
- `Tool` 接口定义 name/description/schema/run，`ToolRegistry` 统一注册、生成 “工具列表 prompt”，以及在运行期根据 JSON 调用对应工具。
- 内置两个示例：
  - `TimeTool`：返回当前 ISO-8601 时间。
  - `HttpFetchTool`：发起 GET 请求并返还指定长度的正文，演示带参数工具。
- 通过接口解耦，可以逐步扩展到更复杂的 I/O 工具，甚至桥接外部系统。

### 配置文件 (`agent.json`)
- 字段：`baseUrl`、`apiKey`、`model`、`timeoutSeconds`、`memoryFile`、`maxTurns`。匹配 `AgentConfig` record，方便被 Jackson 自动序列化/反序列化。
- 这样的 JSON 配置与 CLI 打包后的 JAR 一起分发时，很容易用不同环境的 `agent.json` 做差异化部署。

## 关键设计取舍

| 需求 | 取舍 |
| --- | --- |
| **快速迭代** | 纯 Java + 极简依赖，`mvn package` 就能打包；主流程集中在 `Agent`，方便调试。 |
| **可扩展性** | 把 LLM、记忆、工具都抽象成接口，未来新增实现只需替换注入。 |
| **可观测性** | 记忆落盘 JSON，CLI 输出工具轨迹（`TOOL_CALL ...`），方便排查。 |
| **容错** | CLI 层捕捉连接/超时，提示用户检查 `agent.json` 或打开网关，避免直接崩溃。 |

## 后续扩展建议
1. **更多工具**：在 `tools/` 新增文件，注册进 `ToolRegistry` 即可；也可以读取 `Tool.schema()` 自动生成更结构化的提示词。
2. **多轮策略**：在 `Agent.respond` 中加入 Planner/Router，或基于 `ToolResult` 决策下一步行为。
3. **Streaming**：扩展 `LlmClient` 支持 SSE/WS，改善长回答体验。
4. **更强记忆**：实现新的 `MemoryStore`（如 SQLite、向量库）并透过配置切换。

有了本架构，任何一个模块都能独立替换，而主循环保持稳定，既满足“最小可跑通”，也为后续增强预留清晰接入点。

# java-agent-scaffold（Java Agent 脚手架）

一个**最小可跑通**的 Java Agent 脚手架：
- Maven + CLI（命令行交互）
- 对接 OpenAI 兼容接口（默认你本机：`http://127.0.0.1:8317/v1`）
- JSON Memory（对话历史落盘到 `memory.json`）
- 支持“工具调用”（prototype 版）
- 版本迭代：每个版本都新增功能（见 `CHANGELOG.md` / git tags）
- 版本说明：`docs/releases/`（每个版本包含 What/Why/Problem Solved）

> 目标：先把“能跑、能连 8317、能调用工具、能存记忆”这条链路跑通，再逐步扩展成更强的 Agent（Planner/Router/多工具/HTTP API 等）。

## 运行环境
- **Java 17**（已从 21 降到 17，IDEA 更容易直接运行）
- Maven 3.9+

## 快速开始

```bash
cd /Users/cccc/Documents/java-agent-scaffold
mvn -q -DskipTests package
java -jar target/java-agent-0.3.0.jar
```

首次运行会在当前目录自动生成：
- `agent.json`：运行配置（baseUrl/model/apiKey/超时等）
- `memory.json`：对话历史（JSON 数组）

## 配置说明（agent.json）
- `baseUrl`：默认 `http://127.0.0.1:8317/v1`
- `apiKey`：如果你的网关要求鉴权，填这里（会以 `Authorization: Bearer ...` 发出）
- `model`：默认 `gpt-5.2`
- `timeoutSeconds`：请求超时
- `memoryFile`：记忆文件名（默认 `memory.json`）
- `maxTurns`：每次用户输入最多允许工具循环几轮（防止死循环）

## CLI 参数（覆盖配置）
优先级：CLI > 环境变量 > agent.json

```bash
java -jar target/java-agent-0.3.0.jar \
  --base-url http://127.0.0.1:8317/v1 \
  --model gpt-5.2 \
  --memory-file memory.json
```

支持环境变量：
- `AGENT_BASE_URL`
- `AGENT_MODEL`
- `AGENT_API_KEY`
- `AGENT_TIMEOUT_SECONDS`
- `AGENT_MEMORY_FILE`
- `AGENT_MAX_TURNS`

CLI 小命令：
- `:reset` 清空 `memory.json`

## IDEA 运行提示
如果遇到 “不支持发行版本 21”，说明你的项目/模块 JDK 版本太低。
本项目已使用 Java 17：
- Project SDK：选 17
- Maven JDK：也选 17

## 工具调用（prototype）
当前工具调用协议非常简单：
- 如果模型输出**纯 JSON**（或者 fenced block），形如：

```tool
{"tool":"time.now","args":{}}
```

Agent 会执行对应工具，并把结果作为 `tool` 消息写回对话，继续让模型生成最终回答。

内置工具：
- `time.now`：返回当前时间（ISO-8601）
- `http.fetch`：GET 抓取 URL 并截断返回前 N 个字符
- `fs.ls`：列出本地目录（相对路径）
- `fs.read`：读取本地文本文件（相对路径）

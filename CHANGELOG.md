# Changelog

## v0.3.0
- Add safe local filesystem tools: `fs.ls` and `fs.read` (relative-path only).

## v0.2.0
- Add CLI flags to override config (`--base-url`, `--model`, `--api-key`, `--memory-file`, `--max-turns`, `--timeout-seconds`, `--config`).
- Always include a `system` prompt that teaches the model how to do tool-calls and lists available tools.
- Add CLI command `:reset` to clear persisted memory.

## v0.1.0
- Minimal runnable Java agent scaffold (CLI + OpenAI-compatible `/v1/chat/completions` + JSON memory + prototype tool-calls).

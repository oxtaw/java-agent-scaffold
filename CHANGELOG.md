# Changelog

## v0.3.0
- What: Add safe local filesystem tools: `fs.ls` and `fs.read` (relative-path only).
- Why: Let the agent inspect project files/dirs for debugging and summarization without copy/paste.
- Problem solved: Enables local introspection while blocking absolute paths and path traversal.
- Notes: `docs/releases/v0.3.0.md`

## v0.2.0
- What: Add CLI flags to override config (`--base-url`, `--model`, `--api-key`, `--memory-file`, `--max-turns`, `--timeout-seconds`, `--config`).
- Why: Make the jar portable across environments without editing `agent.json`.
- Problem solved: Faster setup + fewer config mistakes; plus more reliable tool usage via a system prompt with tool schema.
- Notes: `docs/releases/v0.2.0.md`

## v0.1.0
- What: Minimal runnable Java agent scaffold (CLI + OpenAI-compatible `/v1/chat/completions` + JSON memory + prototype tool-calls).
- Why: Prove the end-to-end loop before adding frameworks/complexity.
- Problem solved: Provides a stable base to iterate on tools/planning/routing.
- Notes: `docs/releases/v0.1.0.md`

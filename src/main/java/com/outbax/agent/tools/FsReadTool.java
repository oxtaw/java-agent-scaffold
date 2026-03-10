package com.outbax.agent.tools;

import com.outbax.agent.core.Tool;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;

public class FsReadTool implements Tool {
  @Override public String name() { return "fs.read"; }
  @Override public String description() { return "Read a local text file (relative path) and return first N chars."; }
  @Override public Map<String, String> schema() {
    return Map.of(
        "path", "string (relative to current working directory)",
        "maxChars", "int (default 4000, max 20000)"
    );
  }

  @Override
  public String run(Map<String, Object> args) throws Exception {
    String raw = args.get("path") == null ? "" : String.valueOf(args.get("path")).trim();
    if (raw.isBlank()) return "ERROR: missing args.path";

    int maxChars = 4000;
    if (args.get("maxChars") instanceof Number n) maxChars = n.intValue();
    if (maxChars <= 0) maxChars = 4000;
    if (maxChars > 20000) maxChars = 20000;

    Path root = Path.of(".").toAbsolutePath().normalize();
    Path rel = Path.of(raw);
    if (rel.isAbsolute()) return "ERROR: absolute paths are not allowed";
    Path target = root.resolve(rel).normalize();
    if (!target.startsWith(root)) return "ERROR: path escapes working directory";
    if (!Files.exists(target)) return "ERROR: not found: " + rel;
    if (Files.isDirectory(target)) return "ERROR: is a directory: " + rel;

    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = Files.newBufferedReader(target, StandardCharsets.UTF_8)) {
      char[] buf = new char[2048];
      while (sb.length() < maxChars) {
        int n = br.read(buf, 0, Math.min(buf.length, maxChars - sb.length()));
        if (n < 0) break;
        sb.append(buf, 0, n);
      }
    }

    return sb.toString();
  }
}


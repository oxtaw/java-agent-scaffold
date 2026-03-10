package com.outbax.agent.tools;

import com.outbax.agent.core.Tool;

import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class FsListTool implements Tool {
  @Override public String name() { return "fs.ls"; }
  @Override public String description() { return "List entries in a local directory (relative path)."; }
  @Override public Map<String, String> schema() {
    return Map.of(
        "path", "string (default \".\")",
        "maxEntries", "int (default 200, max 1000)"
    );
  }

  @Override
  public String run(Map<String, Object> args) throws Exception {
    String raw = args.get("path") == null ? "." : String.valueOf(args.get("path")).trim();
    if (raw.isBlank()) raw = ".";

    int maxEntries = 200;
    if (args.get("maxEntries") instanceof Number n) maxEntries = n.intValue();
    if (maxEntries <= 0) maxEntries = 200;
    if (maxEntries > 1000) maxEntries = 1000;

    Path root = Path.of(".").toAbsolutePath().normalize();
    Path rel = Path.of(raw);
    if (rel.isAbsolute()) return "ERROR: absolute paths are not allowed";
    Path target = root.resolve(rel).normalize();
    if (!target.startsWith(root)) return "ERROR: path escapes working directory";
    if (!Files.exists(target)) return "ERROR: not found: " + rel;
    if (!Files.isDirectory(target)) return "ERROR: not a directory: " + rel;

    List<String> entries = new ArrayList<>();
    try (Stream<Path> s = Files.list(target)) {
      s.limit(maxEntries).forEach(p -> {
        String name = p.getFileName().toString();
        try {
          if (Files.isDirectory(p)) name = name + "/";
        } catch (Exception ignored) {}
        entries.add(name);
      });
    }
    Collections.sort(entries);

    StringBuilder sb = new StringBuilder();
    sb.append("ls ").append(rel.normalize()).append("\n");
    for (String e : entries) sb.append(e).append("\n");
    if (entries.isEmpty()) sb.append("(empty)\n");
    return sb.toString();
  }
}


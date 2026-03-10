package com.outbax.agent.cli;

import java.util.*;

public final class CliArgs {
  private final Map<String, String> options;
  private final Set<String> flags;
  private final List<String> positionals;

  private CliArgs(Map<String, String> options, Set<String> flags, List<String> positionals) {
    this.options = options;
    this.flags = flags;
    this.positionals = positionals;
  }

  public static CliArgs parse(String[] args) {
    Map<String, String> options = new LinkedHashMap<>();
    Set<String> flags = new LinkedHashSet<>();
    List<String> positionals = new ArrayList<>();

    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if (!a.startsWith("-") || a.equals("-")) {
        positionals.add(a);
        continue;
      }

      String key = a;
      String value = null;

      int eq = a.indexOf('=');
      if (eq > 0) {
        key = a.substring(0, eq);
        value = a.substring(eq + 1);
      } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
        value = args[++i];
      }

      if (value == null) {
        flags.add(key);
      } else {
        options.put(key, value);
      }
    }

    return new CliArgs(options, flags, positionals);
  }

  public boolean has(String name) {
    return flags.contains(name) || options.containsKey(name);
  }

  public String get(String name) {
    return options.get(name);
  }

  public String getOrDefault(String name, String defaultValue) {
    String v = get(name);
    return v == null ? defaultValue : v;
  }

  public List<String> positionals() {
    return Collections.unmodifiableList(positionals);
  }

  public static String usage() {
    return String.join("\n",
        "java-agent (CLI)",
        "",
        "Usage:",
        "  java -jar target/java-agent-<version>.jar [options]",
        "",
        "Options:",
        "  --config <file>              Config JSON file (default: agent.json)",
        "  --base-url <url>             Override baseUrl",
        "  --model <name>               Override model",
        "  --api-key <key>              Override apiKey",
        "  --timeout-seconds <n>        Override timeoutSeconds",
        "  --memory-file <file>         Override memoryFile",
        "  --max-turns <n>              Override maxTurns",
        "  --system-prompt-file <file>  Append extra system prompt text",
        "  --print-config               Print effective config and exit",
        "  -h, --help                   Show help",
        "",
        "CLI commands:",
        "  :reset                       Clear persisted memory",
        "  exit | quit                  Exit"
    );
  }
}


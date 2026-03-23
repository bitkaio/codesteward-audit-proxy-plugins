<p align="center">
  <img src="assets/codesteward-logo.png" alt="Codesteward" width="400" />
</p>

# Codesteward Audit Proxy — IDE Plugins

Companion plugins for **VSCode** and **JetBrains** IDEs that configure the Codesteward audit proxy, inject identity metadata, and provide observability into what headers are being forwarded to AI coding agents.

## Why?

The [Codesteward audit proxy](https://github.com/bitkaio/codesteward) intercepts LLM API traffic and records structured audit events. But in a centrally-hosted deployment, the proxy doesn't know **who** is making the request, **which repo** they're in, or **which branch** they're on.

These plugins solve that by automatically injecting `X-Audit-*` headers with identity metadata into every request your AI coding agents make.

## Features

- **Proxy configuration** — Set the proxy URL once; the plugin configures all your AI agents automatically
- **Identity injection** — Auto-detects your git email, project name, and branch
- **Header inspector** — Panel showing exactly what headers are active and how each agent is configured
- **Proxy health monitoring** — Periodic health checks with status bar indicator
- **Branch tracking** — Automatically updates headers when you switch git branches
- **Team config** — Commit a `.codesteward.json` to your repo so the whole team gets the same settings

## Supported Agents

| Agent | VSCode | JetBrains |
|-------|--------|-----------|
| Claude Code CLI | `ANTHROPIC_BASE_URL` + `ANTHROPIC_CUSTOM_HEADERS` | `LocalTerminalCustomizer` |
| Codex CLI | `OPENAI_BASE_URL` | `LocalTerminalCustomizer` |
| Gemini CLI | `GEMINI_API_BASE_URL` + `GEMINI_CLI_CUSTOM_HEADERS` | `LocalTerminalCustomizer` |
| Aider | `ANTHROPIC_BASE_URL` / `OPENAI_BASE_URL` + `AIDER_EXTRA_HEADERS` | `LocalTerminalCustomizer` |
| Cline | `process.env` mutation (in-process) | N/A (JVM limitation) |
| Continue | `process.env` mutation (in-process) | N/A (JVM limitation) |

## Getting Started

### 1. Install the plugin

- **VSCode**: Install from the Extensions marketplace or `cd vscode && npm run package`
- **JetBrains**: Install from the Plugin marketplace or `cd jetbrains && ./gradlew buildPlugin`

### 2. Configure the proxy

On first launch, a notification will prompt you to set the proxy URL. Or add a `.codesteward.json` to your repository root:

```json
{
  "proxy": {
    "url": "https://audit.corp.internal:8443",
    "enabled": true
  },
  "identity": {
    "team": "platform-engineering",
    "project": "payment-service"
  }
}
```

### 3. Open a new terminal

Your AI agents will now route through the audit proxy with identity headers attached.

## Project Structure

```
.
├── vscode/                # VSCode extension (TypeScript)
│   ├── src/
│   │   ├── extension.ts       # Activation, lifecycle, commands
│   │   ├── config.ts          # Settings resolution
│   │   ├── identity.ts        # Git auto-detection
│   │   ├── agents.ts          # Per-agent env var mapping
│   │   ├── health.ts          # Proxy health polling
│   │   ├── sessionManager.ts  # Session ID lifecycle
│   │   ├── statusBar.ts       # Status bar item
│   │   └── headerInspector.ts # Header inspector panel
│   └── package.json
│
├── jetbrains/             # JetBrains plugin (Kotlin)
│   ├── src/main/kotlin/com/codesteward/
│   │   ├── CodestewardPlugin.kt
│   │   ├── settings/          # PersistentStateComponent + UI
│   │   ├── identity/          # Git detection via Git4Idea
│   │   ├── terminal/          # LocalTerminalCustomizer
│   │   ├── health/            # Health polling
│   │   ├── session/           # Session ID
│   │   ├── config/            # .codesteward.json reader
│   │   └── ui/                # Status bar + tool window
│   └── build.gradle.kts
│
├── assets/                # Shared branding (logo, icons)
└── ide-plugins-design.md  # Full design specification
```

## Configuration

### VSCode settings (`codesteward.*` namespace)

| Setting | Default | Description |
|---------|---------|-------------|
| `proxy.url` | `http://localhost:8080` | Proxy base URL |
| `proxy.enabled` | `false` | Enable/disable the proxy |
| `identity.user` | *(auto-detect)* | User identity override |
| `identity.project` | *(auto-detect)* | Project name override |
| `identity.branch` | *(auto-detect)* | Branch name override |
| `identity.team` | *(empty)* | Team identifier |
| `headers` | `{}` | Additional custom headers |
| `agents.*` | `true` | Per-agent enable/disable |
| `healthCheck.enabled` | `true` | Enable health checks |
| `healthCheck.intervalSeconds` | `30` | Health check interval |

### JetBrains settings

Settings > Tools > Codesteward Audit Proxy. Same fields as VSCode, stored in `.idea/codesteward.xml`.

### Settings priority

1. Manual overrides in IDE settings
2. Workspace settings
3. `.codesteward.json` in repo root
4. Auto-detected values (git user, repo name, branch)
5. Plugin defaults

## Status Bar

| State | Display | Meaning |
|-------|---------|---------|
| Enabled, healthy | `Codesteward` | Proxy reachable, headers injected |
| Enabled, unreachable | `Codesteward (!)` | Proxy configured but not responding |
| Disabled | `Codesteward (off)` | Plugin disabled |

Click to toggle on/off.

## Development

### VSCode

```sh
cd vscode
npm install
npm run compile    # or npm run watch
# Press F5 to launch extension development host
```

### JetBrains

```sh
cd jetbrains
./gradlew buildPlugin
# Run with: ./gradlew runIde
```

## License

MIT

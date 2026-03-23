# Codesteward Audit Proxy — JetBrains Plugin

Configure the Codesteward audit proxy, inject identity metadata, and inspect what headers are being forwarded to AI coding agents — from any JetBrains IDE.

Targets **IntelliJ IDEA 2024.1+** and all JetBrains IDEs built on the IntelliJ Platform (WebStorm, PyCharm, GoLand, etc.).

## Features

- **Terminal env injection** — Automatically sets agent-specific env vars in every new terminal via `LocalTerminalCustomizer`
- **Automatic identity detection** — Reads your git email, project name, and branch via Git4Idea
- **Header inspector tool window** — Tree view with platform icons showing active headers, agents, and connection status
- **Proxy health monitoring** — Periodic health checks with status bar widget
- **Settings UI** — Dedicated settings panel under Tools > Codesteward Audit Proxy
- **Team config** — Reads `.codesteward.json` from the project root for shared team settings
- **Double-click to copy** — Copy any header value from the inspector

## Supported Agents

| Agent | Base URL | Custom Headers |
|-------|----------|----------------|
| Claude Code CLI | `ANTHROPIC_BASE_URL` | `ANTHROPIC_CUSTOM_HEADERS` (newline-delimited) |
| Codex CLI | `OPENAI_BASE_URL` | — |
| Gemini CLI | `GEMINI_API_BASE_URL` | `GEMINI_CLI_CUSTOM_HEADERS` (comma-delimited) |
| Aider | `ANTHROPIC_BASE_URL` / `OPENAI_BASE_URL` | `AIDER_EXTRA_HEADERS` (JSON) |

> **Note:** JetBrains plugins run in a shared JVM where `System.getenv()` is immutable. In-process agents (JetBrains AI Assistant, Kotlin/Java-based tools) cannot be redirected via env vars. Only terminal-based agents are supported.

## Getting Started

1. Install the plugin from the JetBrains Marketplace (or build locally)
2. Open **Settings > Tools > Codesteward Audit Proxy**
3. Enter the proxy URL and check **Enable proxy**
4. Or add a `.codesteward.json` to your project root:

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

5. Open a new terminal — agent traffic will route through the audit proxy

## Injected Headers

Every request through the proxy includes these `X-Audit-*` headers:

| Header | Source | Example |
|--------|--------|---------|
| `X-Audit-User` | `git config user.email` or manual override | `alice@corp.com` |
| `X-Audit-Project` | Project directory name or manual override | `payment-service` |
| `X-Audit-Branch` | Git4Idea current branch | `feature/checkout` |
| `X-Audit-Session-ID` | Generated UUIDv4 per IDE window | `a1b2c3d4-e5f6-...` |
| `X-Audit-Team` | `.codesteward.json` or manual override | `platform-engineering` |

## Settings

All settings are stored in `.idea/codesteward.xml` (project-level) via `PersistentStateComponent`:

| Setting | Default | Description |
|---------|---------|-------------|
| Proxy URL | `http://localhost:8080` | Proxy base URL |
| Enabled | `false` | Enable/disable the proxy |
| User | *(auto-detect)* | User identity override |
| Project | *(auto-detect)* | Project name override |
| Branch | *(auto-detect)* | Branch name override |
| Team | *(empty)* | Team identifier |
| Agent toggles | `true` | Per-agent enable/disable (Claude, Codex, Gemini, Aider) |
| Health check enabled | `true` | Enable periodic health checks |
| Health check interval | `30` | Interval in seconds |

Settings priority: **IDE settings > `.codesteward.json` > auto-detected values > defaults**.

## Actions

Available from the main menu and search everywhere:

| Action | Description |
|--------|-------------|
| Toggle Proxy | Enable or disable the proxy |
| Open Settings | Open the Codesteward settings panel |
| Show Header Inspector | Open the header inspector tool window |
| Copy Session ID | Copy the current session ID to clipboard |
| Refresh Identity | Re-detect git identity and update headers |
| Check Proxy Health | Run a manual health check with notification |

## Status Bar Widget

| State | Display | Meaning |
|-------|---------|---------|
| Enabled, healthy | `Codesteward` | Proxy reachable, headers injected |
| Enabled, unreachable | `Codesteward (!)` | Proxy configured but not responding |
| Disabled | `Codesteward (off)` | Plugin disabled, no env vars injected |

Click to toggle on/off. Hover for connection details.

## Source Layout

| File | Purpose |
|------|---------|
| `CodestewardPlugin.kt` | Startup activity, action implementations |
| `settings/CodestewardSettings.kt` | `PersistentStateComponent` for project settings |
| `settings/CodestewardConfigurable.kt` | Settings UI panel (Tools > Codesteward) |
| `identity/IdentityDetector.kt` | Git identity detection via Git4Idea + process exec |
| `terminal/AuditTerminalCustomizer.kt` | `LocalTerminalCustomizer` that injects env vars |
| `health/HealthChecker.kt` | Proxy health polling with listener pattern |
| `session/SessionManager.kt` | UUIDv4 session ID, persisted in workspace file |
| `config/RepoConfigReader.kt` | `.codesteward.json` reader |
| `ui/CodestewardStatusBarFactory.kt` | Status bar widget |
| `ui/HeaderInspectorToolWindowFactory.kt` | Tool window with styled tree view |

## Development

### Prerequisites

- JDK 17+
- Gradle (for initial wrapper generation)
- IntelliJ IDEA (for running/debugging)

### First-time setup

Generate the Gradle wrapper (only needed once after cloning):

```sh
gradle wrapper
```

This creates the `gradlew` script used by all subsequent commands.

### Build

```sh
./gradlew buildPlugin
```

The built plugin ZIP will be in `build/distributions/`.

### Run in IDE

```sh
./gradlew runIde
```

This launches a sandboxed IntelliJ instance with the plugin installed.

### Install from local build

1. Build with `./gradlew buildPlugin`
2. In your IDE: Settings > Plugins > gear icon > Install Plugin from Disk
3. Select the ZIP from `build/distributions/`

## License

MIT

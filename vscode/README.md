<p align="center">
  <img src="assets/codesteward-icon.png" alt="Codesteward" width="128" />
</p>

# Codesteward Audit Proxy — VSCode Extension

Configure the Codesteward audit proxy, inject identity metadata, and inspect what headers are being forwarded to AI coding agents — all from within VSCode.

## Features

- **One-click proxy toggle** — Enable/disable from the status bar or command palette
- **Automatic identity detection** — Reads your git email, project name, and branch without any manual setup
- **Header inspector panel** — Sidebar tree view with icons showing exactly what headers are active, per agent
- **Proxy health monitoring** — Periodic health checks with a color-coded status bar indicator
- **Branch tracking** — Automatically updates headers when you switch git branches
- **Team config** — Commit a `.codesteward.json` to your repo so the whole team gets the same proxy settings
- **Welcome view** — Guided setup when no proxy is configured yet
- **Copy on click** — Click any header value in the inspector to copy it to your clipboard

## Supported Agents

| Agent | Base URL | Custom Headers |
|-------|----------|----------------|
| Claude Code CLI | `ANTHROPIC_BASE_URL` | `ANTHROPIC_CUSTOM_HEADERS` (newline-delimited) |
| Claude Code (VSCode ext) | `ANTHROPIC_BASE_URL` | `ANTHROPIC_CUSTOM_HEADERS` (newline-delimited) |
| Codex CLI | `OPENAI_BASE_URL` | — |
| Gemini CLI | `GEMINI_API_BASE_URL` | `GEMINI_CLI_CUSTOM_HEADERS` (comma-delimited) |
| Aider | `ANTHROPIC_BASE_URL` / `OPENAI_BASE_URL` | `AIDER_EXTRA_HEADERS` (JSON) |
| Cline | `process.env` mutation (in-process) | `ANTHROPIC_CUSTOM_HEADERS` (newline-delimited) |
| Continue | `process.env` mutation (in-process) | — |

## Getting Started

1. Install the extension from the VSCode Marketplace (or build locally)
2. On first launch, a notification prompts you to configure the proxy URL
3. Or add a `.codesteward.json` to your repository root:

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

4. Open a new terminal — your AI agents will now route through the audit proxy with identity headers attached

## Injected Headers

Every request through the proxy includes these `X-Audit-*` headers:

| Header | Source | Example |
|--------|--------|---------|
| `X-Audit-User` | `git config user.email` or manual override | `alice@corp.com` |
| `X-Audit-Project` | Workspace folder name or manual override | `payment-service` |
| `X-Audit-Branch` | `git rev-parse --abbrev-ref HEAD` | `feature/checkout` |
| `X-Audit-Session-ID` | Generated UUIDv4 per IDE window | `a1b2c3d4-e5f6-...` |
| `X-Audit-Team` | `.codesteward.json` or manual override | `platform-engineering` |

## Configuration

All settings live under the `codesteward.*` namespace:

| Setting | Default | Description |
|---------|---------|-------------|
| `proxy.url` | `http://localhost:8080` | Proxy base URL |
| `proxy.enabled` | `false` | Enable/disable the proxy |
| `identity.user` | *(auto-detect)* | User identity override |
| `identity.project` | *(auto-detect)* | Project name override |
| `identity.branch` | *(auto-detect)* | Branch name override |
| `identity.team` | *(empty)* | Team identifier |
| `headers` | `{}` | Additional custom headers (key-value pairs) |
| `agents.claude` | `true` | Configure Claude Code |
| `agents.codex` | `true` | Configure Codex CLI |
| `agents.gemini` | `true` | Configure Gemini CLI |
| `agents.cline` | `true` | Configure Cline |
| `agents.aider` | `true` | Configure Aider |
| `agents.continue` | `true` | Configure Continue |
| `healthCheck.enabled` | `true` | Enable health checks |
| `healthCheck.intervalSeconds` | `30` | Health check interval |

Settings priority: **IDE settings > `.codesteward.json` > auto-detected values > defaults**.

## Commands

| Command | Description |
|---------|-------------|
| `Codesteward: Toggle Proxy` | Enable or disable the proxy |
| `Codesteward: Open Settings` | Open extension settings |
| `Codesteward: Show Header Inspector` | Focus the header inspector panel |
| `Codesteward: Copy Session ID` | Copy the current session ID to clipboard |
| `Codesteward: Refresh Identity` | Re-detect git identity and update headers |
| `Codesteward: Check Proxy Health` | Run a manual health check |

## Status Bar

| State | Display | Meaning |
|-------|---------|---------|
| Enabled, healthy | `$(shield) Codesteward` | Proxy reachable, headers injected |
| Enabled, unreachable | `$(shield) Codesteward $(warning)` | Proxy configured but not responding |
| Disabled | `$(shield) Codesteward $(circle-slash)` | Plugin disabled, no env vars injected |

Click to toggle. Hover for a rich markdown tooltip with connection details.

## Source Layout

| File | Purpose |
|------|---------|
| `src/extension.ts` | Activation, lifecycle, commands, onboarding |
| `src/config.ts` | Settings resolution (IDE + `.codesteward.json` + auto-detect) |
| `src/identity.ts` | Git user/project/branch detection, `.git/HEAD` watcher |
| `src/agents.ts` | Per-agent env var mapping and header formatting |
| `src/health.ts` | Proxy health polling via `GET /healthz` |
| `src/sessionManager.ts` | UUIDv4 session ID, persisted in workspace state |
| `src/statusBar.ts` | Status bar item with markdown tooltips |
| `src/headerInspector.ts` | TreeDataProvider for the sidebar panel |

## Development

```sh
npm install
npm run compile    # or npm run watch for live reload
```

Press **F5** in VSCode to launch the Extension Development Host.

### Packaging

```sh
npm run package    # produces a .vsix file
```

## License

MIT

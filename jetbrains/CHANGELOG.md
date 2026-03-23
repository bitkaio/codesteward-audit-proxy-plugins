# Changelog

## 1.0.0 — 2026-03-23

Initial release of the Codesteward Audit Proxy plugin for JetBrains IDEs (IntelliJ IDEA, PhpStorm, WebStorm, PyCharm, etc.). Requires 2024.1+.

### Identity & Session Management

- Auto-detect user email via `git config`, project name from workspace, branch via Git4Idea
- UUIDv4 session ID persisted in workspace file
- Support identity overrides via IDE settings (Tools > Codesteward Audit Proxy)
- Read `.codesteward.json` from project root for shared team configuration
- Config priority: IDE settings > repo config > auto-detected > defaults

### Agent Support

- **Claude Code CLI** — `ANTHROPIC_BASE_URL`, `ANTHROPIC_CUSTOM_HEADERS`
- **Codex CLI** — `OPENAI_BASE_URL`
- **Gemini CLI** — `GEMINI_API_BASE_URL`, `GEMINI_CLI_CUSTOM_HEADERS`
- **Aider** — `ANTHROPIC_BASE_URL`, `OPENAI_BASE_URL`, `AIDER_EXTRA_HEADERS`
- Per-agent enable/disable toggles in settings
- Environment variables injected into every new terminal session via `LocalTerminalCustomizer`

### Proxy Health Monitoring

- Periodic health checks via `GET /healthz` with configurable interval
- IDE notification on first connectivity failure with "Open Settings" action
- Single initial connectivity check when periodic health checks are disabled
- Auto-recovery detection when proxy comes back online
- Diagnostic logging via IntelliJ `Logger`

### UI

- **Status bar widget** — Shows connected/unreachable/disabled/checking state; click to toggle proxy on/off
- **Header Inspector tool window** (right sidebar) — Tree view showing:
  - Connection status with proxy version
  - Active identity headers (User, Project, Branch, Session, Team)
  - Per-agent environment variable configuration
  - Custom headers from settings and repo config
  - Double-click to copy values
- Refresh and health check toolbar actions (heart icon for health)
- Brand icon in plugin list and tool window

### Actions

- `Toggle Proxy` — Enable/disable the proxy
- `Open Settings` — Jump to plugin settings
- `Show Header Inspector` — Open the inspector panel
- `Copy Session ID` — Copy current session ID to clipboard
- `Refresh Identity` — Re-detect git identity
- `Check Proxy Health` — Manual connectivity check with result notification

### Onboarding

- Startup notification when proxy is not yet configured, with "Configure" action to open settings

### Settings (Tools > Codesteward Audit Proxy)

- Proxy URL and enable/disable toggle
- Identity fields: User, Project, Branch, Team
- Per-agent checkboxes: Claude Code, Codex, Gemini, Cline, Aider, Continue
- Health check enable/disable and interval (5–300 seconds)

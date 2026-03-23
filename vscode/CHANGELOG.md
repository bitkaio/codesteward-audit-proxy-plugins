# Changelog

## 1.0.0 — 2026-03-23

Initial release of the Codesteward Audit Proxy extension for VSCode.

### Identity & Session Management

- Auto-detect user email, project name, and branch from git
- Watch `.git/HEAD` for branch changes and refresh automatically
- UUIDv4 session ID persisted per workspace
- Support identity overrides via IDE settings
- Read `.codesteward.json` from repo root for shared team configuration
- Config priority: IDE settings > repo config > auto-detected > defaults

### Agent Support

- **Claude Code CLI** — `ANTHROPIC_BASE_URL`, `ANTHROPIC_CUSTOM_HEADERS`
- **Codex CLI** — `OPENAI_BASE_URL`
- **Gemini CLI** — `GEMINI_API_BASE_URL`, `GEMINI_CLI_CUSTOM_HEADERS`
- **Aider** — `ANTHROPIC_BASE_URL`, `OPENAI_BASE_URL`, `AIDER_EXTRA_HEADERS`
- **Cline** — In-process `process.env` mutation for `ANTHROPIC_BASE_URL`, `OPENAI_BASE_URL`, `ANTHROPIC_CUSTOM_HEADERS`
- **Continue** — In-process `process.env` mutation for `ANTHROPIC_BASE_URL`, `OPENAI_BASE_URL`
- Per-agent enable/disable toggles in settings
- Environment variables injected via `EnvironmentVariableCollection` API (persisted across terminal sessions)

### Proxy Health Monitoring

- Periodic health checks via `GET /healthz` with configurable interval
- Warning notification on first connectivity failure
- Auto-recovery detection when proxy comes back online

### UI

- **Status bar widget** — Shows connected/unreachable/disabled state with rich markdown tooltips; click to toggle
- **Header Inspector panel** — Activity bar sidebar with tree view showing:
  - Connection status with proxy version
  - Active identity headers (User, Project, Branch, Session, Team)
  - Per-agent environment variable configuration
  - Custom headers from settings and repo config
  - Click-to-copy on all values
- Refresh and health check toolbar actions
- Welcome view with configuration prompt when proxy is not set up

### Commands

- `Codesteward: Toggle Proxy` — Enable/disable the proxy
- `Codesteward: Open Settings` — Jump to extension settings
- `Codesteward: Show Header Inspector` — Open the inspector panel
- `Codesteward: Copy Session ID` — Copy current session ID to clipboard
- `Codesteward: Refresh Identity` — Re-detect git identity
- `Codesteward: Check Proxy Health` — Manual connectivity check

### Onboarding

- First-run guided setup: proxy URL input, identity confirmation, workspace/global scope selection
- Automatic prompt when no proxy URL is configured

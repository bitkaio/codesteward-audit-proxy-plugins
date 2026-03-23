import * as vscode from 'vscode';
import { ResolvedConfig } from './config';

export interface AuditHeaders {
  'X-Audit-User': string;
  'X-Audit-Project': string;
  'X-Audit-Branch': string;
  'X-Audit-Session-ID': string;
  'X-Audit-Team': string;
  [key: string]: string;
}

export function buildAuditHeaders(
  config: ResolvedConfig,
  sessionId: string,
): AuditHeaders {
  const headers: AuditHeaders = {
    'X-Audit-User': config.user,
    'X-Audit-Project': config.project,
    'X-Audit-Branch': config.branch,
    'X-Audit-Session-ID': sessionId,
    'X-Audit-Team': config.team,
  };

  for (const [key, value] of Object.entries(config.customHeaders)) {
    headers[key] = value;
  }

  return headers;
}

function formatHeadersNewline(headers: AuditHeaders): string {
  return Object.entries(headers)
    .map(([k, v]) => `${k}: ${v}`)
    .join('\n');
}

function formatHeadersComma(headers: AuditHeaders): string {
  return Object.entries(headers)
    .map(([k, v]) => `${k}: ${v}`)
    .join(',');
}

export function applyToEnvironment(
  envCollection: vscode.EnvironmentVariableCollection,
  config: ResolvedConfig,
  sessionId: string,
): void {
  // Clear all previously set variables
  envCollection.clear();

  if (!config.enabled) {
    return;
  }

  const headers = buildAuditHeaders(config, sessionId);
  const newlineHeaders = formatHeadersNewline(headers);
  const commaHeaders = formatHeadersComma(headers);

  // Claude Code CLI / Claude Code VSCode extension
  if (config.agents.claude) {
    envCollection.replace('ANTHROPIC_BASE_URL', config.proxyUrl);
    envCollection.replace('ANTHROPIC_CUSTOM_HEADERS', newlineHeaders);
  }

  // Codex CLI
  if (config.agents.codex) {
    envCollection.replace('OPENAI_BASE_URL', config.proxyUrl);
  }

  // Gemini CLI
  if (config.agents.gemini) {
    envCollection.replace('GEMINI_API_BASE_URL', config.proxyUrl);
    envCollection.replace('GEMINI_CLI_CUSTOM_HEADERS', commaHeaders);
  }

  // Aider (supports both Anthropic and OpenAI base URLs)
  if (config.agents.aider) {
    // Aider uses ANTHROPIC_BASE_URL and OPENAI_BASE_URL which are already set above
    // Also set the extra headers env var for Aider
    envCollection.replace(
      'AIDER_EXTRA_HEADERS',
      JSON.stringify(Object.fromEntries(Object.entries(headers))),
    );
  }

  // Cline - uses process.env mutation for in-process extension
  if (config.agents.cline) {
    applyClineProcessEnv(config.proxyUrl, headers);
  }
}

function applyClineProcessEnv(
  proxyUrl: string,
  headers: AuditHeaders,
): void {
  // Cline runs in the same Node.js process as VSCode extensions.
  // Setting process.env directly allows it to pick up the proxy configuration.
  process.env['OPENAI_BASE_URL'] = proxyUrl;
  process.env['ANTHROPIC_BASE_URL'] = proxyUrl;

  // Cline reads custom headers from environment (newline-delimited like Anthropic SDK)
  const headerString = Object.entries(headers)
    .map(([k, v]) => `${k}: ${v}`)
    .join('\n');
  process.env['ANTHROPIC_CUSTOM_HEADERS'] = headerString;
}

export function clearProcessEnv(): void {
  delete process.env['OPENAI_BASE_URL'];
  delete process.env['ANTHROPIC_BASE_URL'];
  delete process.env['ANTHROPIC_CUSTOM_HEADERS'];
  delete process.env['AIDER_EXTRA_HEADERS'];
  delete process.env['GEMINI_API_BASE_URL'];
  delete process.env['GEMINI_CLI_CUSTOM_HEADERS'];
}

export interface AgentEnvInfo {
  name: string;
  envVars: Record<string, string>;
}

export function getAgentEnvInfo(
  config: ResolvedConfig,
  sessionId: string,
): AgentEnvInfo[] {
  if (!config.enabled) {
    return [];
  }

  const headers = buildAuditHeaders(config, sessionId);
  const newlineHeaders = formatHeadersNewline(headers);
  const commaHeaders = formatHeadersComma(headers);
  const jsonHeaders = JSON.stringify(Object.fromEntries(Object.entries(headers)));
  const agents: AgentEnvInfo[] = [];

  if (config.agents.claude) {
    agents.push({
      name: 'Claude Code CLI',
      envVars: {
        ANTHROPIC_BASE_URL: config.proxyUrl,
        ANTHROPIC_CUSTOM_HEADERS: newlineHeaders,
      },
    });
  }

  if (config.agents.codex) {
    agents.push({
      name: 'Codex CLI',
      envVars: {
        OPENAI_BASE_URL: config.proxyUrl,
      },
    });
  }

  if (config.agents.gemini) {
    agents.push({
      name: 'Gemini CLI',
      envVars: {
        GEMINI_API_BASE_URL: config.proxyUrl,
        GEMINI_CLI_CUSTOM_HEADERS: commaHeaders,
      },
    });
  }

  if (config.agents.aider) {
    agents.push({
      name: 'Aider',
      envVars: {
        ANTHROPIC_BASE_URL: config.proxyUrl,
        OPENAI_BASE_URL: config.proxyUrl,
        AIDER_EXTRA_HEADERS: jsonHeaders,
      },
    });
  }

  if (config.agents.cline) {
    agents.push({
      name: 'Cline (in-process)',
      envVars: {
        OPENAI_BASE_URL: config.proxyUrl,
        ANTHROPIC_BASE_URL: config.proxyUrl,
        ANTHROPIC_CUSTOM_HEADERS: newlineHeaders,
      },
    });
  }

  if (config.agents.continue) {
    agents.push({
      name: 'Continue (in-process)',
      envVars: {
        OPENAI_BASE_URL: config.proxyUrl,
        ANTHROPIC_BASE_URL: config.proxyUrl,
      },
    });
  }

  return agents;
}

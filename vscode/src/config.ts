import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';

export interface RepoConfig {
  proxy?: {
    url?: string;
    enabled?: boolean;
  };
  identity?: {
    team?: string;
    project?: string;
    user?: string;
  };
  headers?: Record<string, string>;
}

export interface ResolvedConfig {
  proxyUrl: string;
  enabled: boolean;
  user: string;
  project: string;
  branch: string;
  team: string;
  customHeaders: Record<string, string>;
  agents: {
    claude: boolean;
    codex: boolean;
    gemini: boolean;
    cline: boolean;
    aider: boolean;
    continue: boolean;
  };
  healthCheck: {
    enabled: boolean;
    intervalSeconds: number;
  };
}

const CONFIG_FILE_NAME = '.codesteward.json';

export function readRepoConfig(): RepoConfig | undefined {
  const workspaceFolders = vscode.workspace.workspaceFolders;
  if (!workspaceFolders || workspaceFolders.length === 0) {
    return undefined;
  }

  const configPath = path.join(workspaceFolders[0].uri.fsPath, CONFIG_FILE_NAME);
  try {
    const content = fs.readFileSync(configPath, 'utf-8');
    return JSON.parse(content) as RepoConfig;
  } catch {
    return undefined;
  }
}

export function getConfig(autoDetected: {
  user: string;
  project: string;
  branch: string;
}): ResolvedConfig {
  const settings = vscode.workspace.getConfiguration('codesteward');
  const repoConfig = readRepoConfig();

  // Priority: IDE settings > repo config > auto-detected > defaults
  const proxyUrl =
    settings.get<string>('proxy.url') ||
    repoConfig?.proxy?.url ||
    'http://localhost:8080';

  const enabled =
    settings.get<boolean>('proxy.enabled') ??
    repoConfig?.proxy?.enabled ??
    false;

  const user =
    settings.get<string>('identity.user') ||
    repoConfig?.identity?.user ||
    autoDetected.user;

  const project =
    settings.get<string>('identity.project') ||
    repoConfig?.identity?.project ||
    autoDetected.project;

  const branch =
    settings.get<string>('identity.branch') ||
    autoDetected.branch;

  const team =
    settings.get<string>('identity.team') ||
    repoConfig?.identity?.team ||
    '';

  const ideHeaders = settings.get<Record<string, string>>('headers') || {};
  const repoHeaders = repoConfig?.headers || {};
  const customHeaders = { ...repoHeaders, ...ideHeaders };

  return {
    proxyUrl,
    enabled,
    user,
    project,
    branch,
    team,
    customHeaders,
    agents: {
      claude: settings.get<boolean>('agents.claude') ?? true,
      codex: settings.get<boolean>('agents.codex') ?? true,
      gemini: settings.get<boolean>('agents.gemini') ?? true,
      cline: settings.get<boolean>('agents.cline') ?? true,
      aider: settings.get<boolean>('agents.aider') ?? true,
      continue: settings.get<boolean>('agents.continue') ?? true,
    },
    healthCheck: {
      enabled: settings.get<boolean>('healthCheck.enabled') ?? true,
      intervalSeconds: settings.get<number>('healthCheck.intervalSeconds') ?? 30,
    },
  };
}

export function hasProxyConfigured(): boolean {
  const settings = vscode.workspace.getConfiguration('codesteward');
  const repoConfig = readRepoConfig();

  // Check if the user has explicitly set a proxy URL (not just the default)
  const inspection = settings.inspect<string>('proxy.url');
  const hasExplicitUrl =
    !!inspection?.workspaceValue ||
    !!inspection?.workspaceFolderValue ||
    !!inspection?.globalValue;

  const hasRepoUrl = !!repoConfig?.proxy?.url;

  return hasExplicitUrl || hasRepoUrl;
}

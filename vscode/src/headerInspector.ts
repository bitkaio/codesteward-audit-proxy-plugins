import * as vscode from 'vscode';
import { ResolvedConfig } from './config';
import { buildAuditHeaders, getAgentEnvInfo } from './agents';
import { HealthStatus } from './health';

class HeaderTreeItem extends vscode.TreeItem {
  constructor(
    label: string,
    public readonly children?: HeaderTreeItem[],
    options?: {
      description?: string;
      icon?: string;
      iconColor?: vscode.ThemeColor;
      copyValue?: string;
      collapsed?: boolean;
      contextValue?: string;
    },
  ) {
    super(
      label,
      children
        ? options?.collapsed
          ? vscode.TreeItemCollapsibleState.Collapsed
          : vscode.TreeItemCollapsibleState.Expanded
        : vscode.TreeItemCollapsibleState.None,
    );

    if (options?.description) {
      this.description = options.description;
    }

    if (options?.icon) {
      this.iconPath = new vscode.ThemeIcon(
        options.icon,
        options.iconColor,
      );
    }

    if (options?.copyValue) {
      this.tooltip = new vscode.MarkdownString(
        `\`${options.copyValue}\`\n\n$(clippy) Click to copy`,
      );
      this.tooltip.supportThemeIcons = true;
      this.command = {
        command: 'codesteward.copyValue',
        title: 'Copy Value',
        arguments: [options.copyValue],
      };
      this.contextValue = 'copyable';
    }

    if (options?.contextValue) {
      this.contextValue = options.contextValue;
    }
  }
}

export class HeaderInspectorProvider
  implements vscode.TreeDataProvider<HeaderTreeItem>
{
  private _onDidChangeTreeData = new vscode.EventEmitter<
    HeaderTreeItem | undefined
  >();
  readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

  private config: ResolvedConfig | undefined;
  private sessionId = '';
  private healthStatus: HealthStatus = { reachable: false };

  update(
    config: ResolvedConfig,
    sessionId: string,
    healthStatus: HealthStatus,
  ): void {
    this.config = config;
    this.sessionId = sessionId;
    this.healthStatus = healthStatus;
    this._onDidChangeTreeData.fire(undefined);
  }

  getTreeItem(element: HeaderTreeItem): vscode.TreeItem {
    return element;
  }

  getChildren(element?: HeaderTreeItem): HeaderTreeItem[] {
    if (element) {
      return element.children || [];
    }

    if (!this.config) {
      return [
        new HeaderTreeItem('No proxy configured', undefined, {
          icon: 'info',
          description: 'Run "Codesteward: Toggle Proxy" to get started',
        }),
      ];
    }

    if (!this.config.enabled) {
      return [
        new HeaderTreeItem('Proxy disabled', undefined, {
          icon: 'circle-slash',
          description: 'Click the status bar or run Toggle Proxy',
        }),
      ];
    }

    return [
      this.buildStatusItem(),
      this.buildIdentitySection(),
      this.buildAgentsSection(),
      ...this.buildCustomHeadersSection(),
    ];
  }

  private buildStatusItem(): HeaderTreeItem {
    if (this.healthStatus.reachable) {
      const version = this.healthStatus.version
        ? `v${this.healthStatus.version}`
        : '';
      return new HeaderTreeItem('Connected', undefined, {
        description: [this.config!.proxyUrl, version]
          .filter(Boolean)
          .join(' \u2014 '),
        icon: 'pass-filled',
        iconColor: new vscode.ThemeColor('testing.iconPassed'),
      });
    }
    return new HeaderTreeItem('Unreachable', undefined, {
      description: this.healthStatus.error || this.config!.proxyUrl,
      icon: 'error',
      iconColor: new vscode.ThemeColor('testing.iconFailed'),
    });
  }

  private buildIdentitySection(): HeaderTreeItem {
    const headers = buildAuditHeaders(this.config!, this.sessionId);

    const headerIconMap: Record<string, string> = {
      'X-Audit-User': 'account',
      'X-Audit-Project': 'repo',
      'X-Audit-Branch': 'git-branch',
      'X-Audit-Session-ID': 'key',
      'X-Audit-Team': 'organization',
    };

    const items = Object.entries(headers).map(([k, v]) => {
      const icon = headerIconMap[k] || 'symbol-field';
      const displayValue = k === 'X-Audit-Session-ID'
        ? `${v.substring(0, 8)}...`
        : v || '(empty)';
      return new HeaderTreeItem(k.replace('X-Audit-', ''), undefined, {
        description: displayValue,
        icon,
        copyValue: v,
      });
    });

    return new HeaderTreeItem('Identity', items, {
      icon: 'shield',
      description: `${Object.values(headers).filter(Boolean).length} headers`,
    });
  }

  private buildAgentsSection(): HeaderTreeItem {
    const agentInfos = getAgentEnvInfo(this.config!, this.sessionId);

    const agentIconMap: Record<string, string> = {
      'Claude Code CLI': 'terminal',
      'Codex CLI': 'terminal',
      'Gemini CLI': 'terminal',
      Aider: 'terminal',
      'Cline (in-process)': 'extensions',
      'Continue (in-process)': 'extensions',
    };

    const agentItems = agentInfos.map((agent) => {
      const envItems = Object.entries(agent.envVars).map(([k, v]) => {
        const isUrl = k.includes('BASE_URL');
        const display =
          v.length > 50 ? `${v.substring(0, 50)}...` : v;
        return new HeaderTreeItem(k, undefined, {
          description: display,
          icon: isUrl ? 'globe' : 'list-flat',
          copyValue: v,
        });
      });
      return new HeaderTreeItem(agent.name, envItems, {
        icon: agentIconMap[agent.name] || 'terminal',
        description: `${envItems.length} vars`,
        collapsed: true,
      });
    });

    return new HeaderTreeItem('Agents', agentItems, {
      icon: 'hubot',
      description: `${agentItems.length} configured`,
    });
  }

  private buildCustomHeadersSection(): HeaderTreeItem[] {
    const customEntries = Object.entries(
      this.config!.customHeaders,
    );
    if (customEntries.length === 0) {
      return [];
    }

    const customItems = customEntries.map(
      ([k, v]) =>
        new HeaderTreeItem(k, undefined, {
          description: v,
          icon: 'symbol-field',
          copyValue: v,
        }),
    );

    return [
      new HeaderTreeItem('Custom Headers', customItems, {
        icon: 'settings-gear',
        description: `${customItems.length} headers`,
        collapsed: true,
      }),
    ];
  }

  dispose(): void {
    this._onDidChangeTreeData.dispose();
  }
}

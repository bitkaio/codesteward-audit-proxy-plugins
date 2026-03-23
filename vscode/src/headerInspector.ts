import * as vscode from 'vscode';
import { ResolvedConfig } from './config';
import { buildAuditHeaders, getAgentEnvInfo } from './agents';
import { HealthStatus } from './health';

type TreeItemType = 'category' | 'entry';

class HeaderTreeItem extends vscode.TreeItem {
  constructor(
    public readonly label: string,
    public readonly itemType: TreeItemType,
    public readonly children?: HeaderTreeItem[],
    public readonly copyValue?: string,
  ) {
    super(
      label,
      children
        ? vscode.TreeItemCollapsibleState.Expanded
        : vscode.TreeItemCollapsibleState.None,
    );

    if (itemType === 'entry' && copyValue) {
      this.tooltip = `Click to copy: ${copyValue}`;
      this.command = {
        command: 'codesteward.copyValue',
        title: 'Copy Value',
        arguments: [copyValue],
      };
    }

    if (itemType === 'category') {
      this.iconPath = new vscode.ThemeIcon('folder');
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
      return [new HeaderTreeItem('Not configured', 'entry')];
    }

    if (!this.config.enabled) {
      return [new HeaderTreeItem('Proxy disabled', 'entry')];
    }

    const items: HeaderTreeItem[] = [];

    // Status
    const statusText = this.healthStatus.reachable
      ? `Connected${this.healthStatus.version ? ` (v${this.healthStatus.version})` : ''}`
      : `Unreachable — ${this.healthStatus.error || 'unknown'}`;
    items.push(new HeaderTreeItem(`Status: ${statusText}`, 'entry'));

    // Session
    items.push(
      new HeaderTreeItem(
        `Session: ${this.sessionId.substring(0, 8)}...`,
        'entry',
        undefined,
        this.sessionId,
      ),
    );

    // Injected Headers
    const headers = buildAuditHeaders(this.config, this.sessionId);
    const headerItems = Object.entries(headers)
      .filter(([, v]) => v !== '')
      .map(
        ([k, v]) =>
          new HeaderTreeItem(`${k}: ${v}`, 'entry', undefined, v),
      );
    items.push(
      new HeaderTreeItem('Injected Headers', 'category', headerItems),
    );

    // Agent Configuration
    const agentInfos = getAgentEnvInfo(this.config, this.sessionId);
    const agentItems = agentInfos.map((agent) => {
      const envItems = Object.entries(agent.envVars).map(
        ([k, v]) => {
          const display = v.length > 60 ? `${v.substring(0, 60)}...` : v;
          return new HeaderTreeItem(
            `${k}: ${display}`,
            'entry',
            undefined,
            v,
          );
        },
      );
      return new HeaderTreeItem(agent.name, 'category', envItems);
    });
    if (agentItems.length > 0) {
      items.push(
        new HeaderTreeItem('Agent Configuration', 'category', agentItems),
      );
    }

    // Custom Headers
    const customEntries = Object.entries(this.config.customHeaders);
    if (customEntries.length > 0) {
      const customItems = customEntries.map(
        ([k, v]) =>
          new HeaderTreeItem(`${k}: ${v}`, 'entry', undefined, v),
      );
      items.push(
        new HeaderTreeItem('Custom Headers', 'category', customItems),
      );
    }

    return items;
  }

  dispose(): void {
    this._onDidChangeTreeData.dispose();
  }
}

import * as vscode from 'vscode';
import { HealthStatus } from './health';

export class StatusBar implements vscode.Disposable {
  private readonly item: vscode.StatusBarItem;

  constructor() {
    this.item = vscode.window.createStatusBarItem(
      vscode.StatusBarAlignment.Left,
      100,
    );
    this.item.command = 'codesteward.toggle';
    this.setDisabled();
    this.item.show();
  }

  setEnabled(health: HealthStatus): void {
    if (health.reachable) {
      this.item.text = '$(shield) Codesteward';
      this.item.tooltip = `Codesteward Audit Proxy: Connected${health.version ? ` (v${health.version})` : ''}`;
      this.item.backgroundColor = undefined;
    } else {
      this.item.text = '$(shield) Codesteward (!)';
      this.item.tooltip = `Codesteward Audit Proxy: Unreachable — ${health.error || 'unknown error'}`;
      this.item.backgroundColor = new vscode.ThemeColor(
        'statusBarItem.warningBackground',
      );
    }
  }

  setDisabled(): void {
    this.item.text = '$(shield) Codesteward (off)';
    this.item.tooltip = 'Codesteward Audit Proxy: Disabled — click to enable';
    this.item.backgroundColor = undefined;
  }

  dispose(): void {
    this.item.dispose();
  }
}

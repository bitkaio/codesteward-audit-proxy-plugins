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
    this.item.name = 'Codesteward Audit Proxy';
    this.setDisabled();
    this.item.show();
  }

  setEnabled(health: HealthStatus): void {
    if (health.reachable) {
      this.item.text = '$(shield) Codesteward';
      this.item.backgroundColor = undefined;

      const tip = new vscode.MarkdownString('', true);
      tip.supportThemeIcons = true;
      tip.appendMarkdown('### $(shield) Codesteward Audit Proxy\n\n');
      tip.appendMarkdown(`$(pass-filled) **Connected**`);
      if (health.version) {
        tip.appendMarkdown(` \u2014 v${health.version}`);
      }
      tip.appendMarkdown('\n\n---\n\n');
      tip.appendMarkdown('$(arrow-swap) Click to disable proxy');
      this.item.tooltip = tip;
    } else {
      this.item.text = '$(shield) Codesteward $(warning)';
      this.item.backgroundColor = new vscode.ThemeColor(
        'statusBarItem.warningBackground',
      );

      const tip = new vscode.MarkdownString('', true);
      tip.supportThemeIcons = true;
      tip.appendMarkdown('### $(shield) Codesteward Audit Proxy\n\n');
      tip.appendMarkdown(
        `$(error) **Unreachable** \u2014 ${health.error || 'unknown error'}\n\n`,
      );
      tip.appendMarkdown(
        'The proxy is not responding. Terminals will still set env vars, but agent requests may fail.\n\n',
      );
      tip.appendMarkdown('---\n\n');
      tip.appendMarkdown('$(arrow-swap) Click to disable proxy');
      this.item.tooltip = tip;
    }
  }

  setDisabled(): void {
    this.item.text = '$(shield) Codesteward $(circle-slash)';
    this.item.backgroundColor = undefined;

    const tip = new vscode.MarkdownString('', true);
    tip.supportThemeIcons = true;
    tip.appendMarkdown('### $(shield) Codesteward Audit Proxy\n\n');
    tip.appendMarkdown('$(circle-slash) **Disabled**\n\n');
    tip.appendMarkdown(
      'AI agents will connect directly to LLM APIs without audit logging.\n\n',
    );
    tip.appendMarkdown('---\n\n');
    tip.appendMarkdown('$(plug) Click to enable proxy');
    this.item.tooltip = tip;
  }

  dispose(): void {
    this.item.dispose();
  }
}

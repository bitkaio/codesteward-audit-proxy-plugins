import * as vscode from 'vscode';
import { getConfig, hasProxyConfigured } from './config';
import { detectAll, createBranchWatcher } from './identity';
import { SessionManager } from './sessionManager';
import { applyToEnvironment, clearProcessEnv } from './agents';
import { HealthChecker } from './health';
import { StatusBar } from './statusBar';
import { HeaderInspectorProvider } from './headerInspector';

let healthChecker: HealthChecker;
let statusBar: StatusBar;
let headerInspector: HeaderInspectorProvider;
let sessionManager: SessionManager;

function refreshAll(context: vscode.ExtensionContext): void {
  const identity = detectAll();
  const config = getConfig(identity);
  const sessionId = sessionManager.getSessionId();

  // Set context key for welcome view visibility
  vscode.commands.executeCommand(
    'setContext',
    'codesteward.configured',
    config.enabled,
  );

  const envCollection = context.environmentVariableCollection;
  applyToEnvironment(envCollection, config, sessionId);

  if (config.enabled) {
    if (config.healthCheck.enabled) {
      healthChecker.start(config.proxyUrl, config.healthCheck.intervalSeconds);
    } else {
      healthChecker.stop();
    }
    statusBar.setEnabled(healthChecker.getLastStatus());
  } else {
    healthChecker.stop();
    clearProcessEnv();
    statusBar.setDisabled();
  }

  headerInspector.update(config, sessionId, healthChecker.getLastStatus());
}

async function runOnboarding(): Promise<void> {
  const action = await vscode.window.showInformationMessage(
    'Codesteward: Configure audit proxy?',
    'Configure',
    'Dismiss',
  );

  if (action !== 'Configure') {
    return;
  }

  const proxyUrl = await vscode.window.showInputBox({
    prompt: 'Enter proxy URL',
    value: 'http://localhost:8080',
    placeHolder: 'http://localhost:8080',
  });

  if (!proxyUrl) {
    return;
  }

  const identity = detectAll();
  const confirmUser = await vscode.window.showQuickPick(['Yes', 'Change'], {
    placeHolder: `Detected identity: ${identity.user} — correct?`,
  });

  let user = '';
  if (confirmUser === 'Change') {
    const customUser = await vscode.window.showInputBox({
      prompt: 'Enter your identity (email or username)',
      value: identity.user,
    });
    user = customUser || '';
  }

  const scope = await vscode.window.showQuickPick(
    ['Yes, workspace only', 'No, globally'],
    { placeHolder: 'Enable for this workspace?' },
  );

  const target =
    scope === 'Yes, workspace only'
      ? vscode.ConfigurationTarget.Workspace
      : vscode.ConfigurationTarget.Global;

  const settings = vscode.workspace.getConfiguration('codesteward');
  await settings.update('proxy.url', proxyUrl, target);
  await settings.update('proxy.enabled', true, target);
  if (user) {
    await settings.update('identity.user', user, target);
  }
}

export function activate(context: vscode.ExtensionContext): void {
  sessionManager = new SessionManager(context);
  healthChecker = new HealthChecker();
  statusBar = new StatusBar();
  headerInspector = new HeaderInspectorProvider();

  context.subscriptions.push(healthChecker, statusBar, headerInspector);

  // Register tree view
  const treeView = vscode.window.createTreeView(
    'codesteward.headerInspector',
    { treeDataProvider: headerInspector },
  );
  context.subscriptions.push(treeView);

  // Register commands
  context.subscriptions.push(
    vscode.commands.registerCommand('codesteward.toggle', async () => {
      const settings = vscode.workspace.getConfiguration('codesteward');
      const current = settings.get<boolean>('proxy.enabled') ?? false;
      await settings.update(
        'proxy.enabled',
        !current,
        vscode.ConfigurationTarget.Workspace,
      );
      if (!current) {
        vscode.window.showInformationMessage(
          'Codesteward proxy enabled. New terminals will route through the proxy.',
        );
      } else {
        vscode.window.showInformationMessage(
          'Codesteward proxy disabled. Open new terminals to use direct API access.',
        );
      }
    }),
  );

  context.subscriptions.push(
    vscode.commands.registerCommand('codesteward.openSettings', () => {
      vscode.commands.executeCommand(
        'workbench.action.openSettings',
        'codesteward',
      );
    }),
  );

  context.subscriptions.push(
    vscode.commands.registerCommand('codesteward.showHeaders', () => {
      vscode.commands.executeCommand('codesteward.headerInspector.focus');
    }),
  );

  context.subscriptions.push(
    vscode.commands.registerCommand('codesteward.copySessionId', () => {
      const id = sessionManager.getSessionId();
      vscode.env.clipboard.writeText(id);
      vscode.window.showInformationMessage(`Session ID copied: ${id}`);
    }),
  );

  context.subscriptions.push(
    vscode.commands.registerCommand('codesteward.refreshIdentity', () => {
      refreshAll(context);
      vscode.window.showInformationMessage(
        'Codesteward: Identity refreshed.',
      );
    }),
  );

  context.subscriptions.push(
    vscode.commands.registerCommand('codesteward.checkHealth', async () => {
      const config = getConfig(detectAll());
      if (!config.proxyUrl) {
        vscode.window.showWarningMessage(
          'Codesteward: No proxy URL configured.',
        );
        return;
      }
      const { checkHealth } = await import('./health');
      const status = await checkHealth(config.proxyUrl);
      if (status.reachable) {
        vscode.window.showInformationMessage(
          `Codesteward: Proxy healthy${status.version ? ` (v${status.version})` : ''}.`,
        );
      } else {
        vscode.window.showWarningMessage(
          `Codesteward: Proxy unreachable — ${status.error}`,
        );
      }
    }),
  );

  // Copy value helper for header inspector
  context.subscriptions.push(
    vscode.commands.registerCommand(
      'codesteward.copyValue',
      (value: string) => {
        vscode.env.clipboard.writeText(value);
        vscode.window.showInformationMessage('Copied to clipboard.');
      },
    ),
  );

  // Watch for health status changes
  healthChecker.onStatusChange((status) => {
    const config = getConfig(detectAll());
    if (config.enabled) {
      statusBar.setEnabled(status);
    }
    headerInspector.update(
      config,
      sessionManager.getSessionId(),
      status,
    );
  });

  // Watch for settings changes
  context.subscriptions.push(
    vscode.workspace.onDidChangeConfiguration((e) => {
      if (e.affectsConfiguration('codesteward')) {
        refreshAll(context);
      }
    }),
  );

  // Watch for branch changes
  const branchWatcher = createBranchWatcher(() => {
    refreshAll(context);
  });
  context.subscriptions.push(branchWatcher);

  // Initial setup
  refreshAll(context);

  // Onboarding check
  if (!hasProxyConfigured()) {
    runOnboarding();
  }
}

export function deactivate(): void {
  clearProcessEnv();
}

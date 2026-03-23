import * as vscode from 'vscode';
import * as crypto from 'crypto';

const SESSION_KEY = 'codesteward.sessionId';

export class SessionManager {
  private sessionId: string;

  constructor(private readonly context: vscode.ExtensionContext) {
    const stored = context.workspaceState.get<string>(SESSION_KEY);
    if (stored) {
      this.sessionId = stored;
    } else {
      this.sessionId = crypto.randomUUID();
      context.workspaceState.update(SESSION_KEY, this.sessionId);
    }
  }

  getSessionId(): string {
    return this.sessionId;
  }

  regenerate(): string {
    this.sessionId = crypto.randomUUID();
    this.context.workspaceState.update(SESSION_KEY, this.sessionId);
    return this.sessionId;
  }
}

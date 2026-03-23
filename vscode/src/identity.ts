import * as vscode from 'vscode';
import * as cp from 'child_process';
import * as path from 'path';
import * as os from 'os';

export interface DetectedIdentity {
  user: string;
  project: string;
  branch: string;
}

function execGit(args: string[], cwd: string): string | undefined {
  try {
    const result = cp.execFileSync('git', args, {
      cwd,
      encoding: 'utf-8',
      timeout: 5000,
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    return result.trim();
  } catch {
    return undefined;
  }
}

function getWorkspaceCwd(): string | undefined {
  const folders = vscode.workspace.workspaceFolders;
  if (!folders || folders.length === 0) {
    return undefined;
  }
  return folders[0].uri.fsPath;
}

export function detectUser(): string {
  const cwd = getWorkspaceCwd();
  if (cwd) {
    const email = execGit(['config', 'user.email'], cwd);
    if (email) {
      return email;
    }
  }
  return os.userInfo().username;
}

export function detectProject(): string {
  const folders = vscode.workspace.workspaceFolders;
  if (!folders || folders.length === 0) {
    return '';
  }
  return path.basename(folders[0].uri.fsPath);
}

export function detectBranch(): string {
  const cwd = getWorkspaceCwd();
  if (!cwd) {
    return 'unknown';
  }
  return execGit(['rev-parse', '--abbrev-ref', 'HEAD'], cwd) || 'unknown';
}

export function detectAll(): DetectedIdentity {
  return {
    user: detectUser(),
    project: detectProject(),
    branch: detectBranch(),
  };
}

export function createBranchWatcher(
  onBranchChange: (newBranch: string) => void,
): vscode.Disposable {
  const disposables: vscode.Disposable[] = [];
  const cwd = getWorkspaceCwd();
  if (!cwd) {
    return vscode.Disposable.from(...disposables);
  }

  const gitHeadPath = path.join(cwd, '.git', 'HEAD');
  const pattern = new vscode.RelativePattern(
    vscode.Uri.file(path.dirname(gitHeadPath)),
    'HEAD',
  );

  const watcher = vscode.workspace.createFileSystemWatcher(pattern);

  const handleChange = () => {
    const branch = detectBranch();
    onBranchChange(branch);
  };

  disposables.push(watcher.onDidChange(handleChange));
  disposables.push(watcher.onDidCreate(handleChange));
  disposables.push(watcher);

  return vscode.Disposable.from(...disposables);
}

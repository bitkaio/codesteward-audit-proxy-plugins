import * as vscode from 'vscode';
import * as http from 'http';
import * as https from 'https';

export interface HealthStatus {
  reachable: boolean;
  version?: string;
  error?: string;
}

export async function checkHealth(proxyUrl: string): Promise<HealthStatus> {
  const url = new URL('/healthz', proxyUrl);

  return new Promise((resolve) => {
    const client = url.protocol === 'https:' ? https : http;
    const req = client.get(url.toString(), { timeout: 5000 }, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        if (res.statusCode === 200) {
          try {
            const body = JSON.parse(data);
            resolve({
              reachable: true,
              version: body.version,
            });
          } catch {
            resolve({ reachable: true });
          }
        } else {
          resolve({
            reachable: false,
            error: `HTTP ${res.statusCode}`,
          });
        }
      });
    });

    req.on('error', (err) => {
      resolve({
        reachable: false,
        error: err.message,
      });
    });

    req.on('timeout', () => {
      req.destroy();
      resolve({
        reachable: false,
        error: 'Connection timed out',
      });
    });
  });
}

export class HealthChecker implements vscode.Disposable {
  private timer: ReturnType<typeof setInterval> | undefined;
  private lastStatus: HealthStatus = { reachable: false };
  private hasNotifiedFailure = false;

  private readonly _onStatusChange = new vscode.EventEmitter<HealthStatus>();
  readonly onStatusChange = this._onStatusChange.event;

  start(proxyUrl: string, intervalSeconds: number): void {
    this.stop();
    this.hasNotifiedFailure = false;

    const poll = async () => {
      const status = await checkHealth(proxyUrl);
      const changed =
        status.reachable !== this.lastStatus.reachable ||
        status.version !== this.lastStatus.version;

      this.lastStatus = status;

      if (changed) {
        this._onStatusChange.fire(status);
      }

      if (!status.reachable && !this.hasNotifiedFailure) {
        this.hasNotifiedFailure = true;
        vscode.window.showWarningMessage(
          `Codesteward: Proxy unreachable at ${proxyUrl} — ${status.error}`,
        );
      }

      if (status.reachable) {
        this.hasNotifiedFailure = false;
      }
    };

    poll();
    this.timer = setInterval(poll, intervalSeconds * 1000);
  }

  stop(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = undefined;
    }
  }

  getLastStatus(): HealthStatus {
    return this.lastStatus;
  }

  dispose(): void {
    this.stop();
    this._onStatusChange.dispose();
  }
}

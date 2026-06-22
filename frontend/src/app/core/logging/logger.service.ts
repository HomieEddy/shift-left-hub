import { Injectable } from '@angular/core';

/**
 * Centralised application logger.
 *
 * <p>Single injection point for the 9 production-path console.error /
 * console.warn sites (audit L-1). Today it forwards to console unchanged
 * so existing behaviour is preserved. A future Sentry / backend-log
 * endpoint can be wired in here without touching the 9 call sites.
 *
 * <p>Use error() for unhandled UI failures (HTTP errors, action
 * rejections) and warn() for recoverable issues (a poll that failed
 * but will retry). Do not use this for routine logging or debug output.
 */
@Injectable({ providedIn: 'root' })
export class LoggerService {
  error(message: string, context?: unknown): void {
    if (context !== undefined) {
      console.error(message, context);
    } else {
      console.error(message);
    }
  }

  warn(message: string, context?: unknown): void {
    if (context !== undefined) {
      console.warn(message, context);
    } else {
      console.warn(message);
    }
  }
}
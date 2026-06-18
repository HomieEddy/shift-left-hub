import { Injectable, signal } from '@angular/core';

/** Holds the current access token in memory only. */
@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  private readonly accessTokenSignal = signal<string | null>(null);

  accessToken(): string | null {
    return this.accessTokenSignal();
  }

  setAccessToken(token: string): void {
    this.accessTokenSignal.set(token);
  }

  clear(): void {
    this.accessTokenSignal.set(null);
  }
}

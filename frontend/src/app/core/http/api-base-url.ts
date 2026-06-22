export interface RuntimeEnv {
  apiBaseUrl?: string;
}

declare global {
  interface Window {
    __env?: RuntimeEnv;
  }
}

export function readApiBaseUrl(): string {
  const env = window.__env;
  return env?.apiBaseUrl?.replace(/\/+$/, '') ?? '';
}

export function resolveApiUrl(path: string): string {
  const base = readApiBaseUrl();
  return base ? `${base}${path}` : path;
}

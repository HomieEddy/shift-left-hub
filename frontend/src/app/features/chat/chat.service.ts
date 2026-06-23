import { Injectable, inject } from '@angular/core';
import { Subject, firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { resolveApiUrl } from '../../core/http/api-base-url';

export interface ChatMessage {
  id?: string;
  role: 'user' | 'assistant';
  content: string;
  sources?: SourceRef[];
}

export interface SourceRef {
  articleId: string;
  title: string;
  slug: string;
  score: number;
  filename?: string;
  excerpt?: string;
}

export interface StreamEvent {
  type: 'token' | 'done' | 'fallback' | 'error';
  content: string;
  sources?: SourceRef[];
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private authService = inject(AuthService);

  sendMessage(
    message: string,
    history: ChatMessage[],
  ): { events: Subject<StreamEvent>; abort: () => void } {
    const subject = new Subject<StreamEvent>();
    const controller = new AbortController();

    const buildHeaders = (): HeadersInit => {
      const token = this.authService.accessToken();
      return token !== null
        ? { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
        : { 'Content-Type': 'application/json' };
    };

    const doFetch = () => fetch(resolveApiUrl('/api/ai/chat'), {
      method: 'POST',
      headers: buildHeaders(),
      credentials: 'include',
      body: JSON.stringify({ message, history }),
      signal: controller.signal,
    });

    const handleResponse = async (response: Response) => {
      if (!response.ok) {
        subject.next({
          type: 'error',
          content: `HTTP ${response.status}: ${response.statusText}`,
        });
        subject.complete();
        return;
      }

      if (!response.body) {
        subject.next({ type: 'error', content: 'Empty response from server' });
        subject.complete();
        return;
      }
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      let currentData = '';

      const tryParseAndEmit = (data: string): boolean => {
        if (!data) return false;
        try {
          const event = JSON.parse(data) as StreamEvent;
          subject.next(event);
          if (event.type === 'done' || event.type === 'fallback' || event.type === 'error') {
            subject.complete();
            return true;
          }
        } catch {
          // skip malformed JSON
        }
        return false;
      };

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() ?? '';

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const payload = line.slice(5).trimStart();
            if (currentData && !payload.startsWith('{')) {
              currentData += '\n' + payload;
            } else {
              if (tryParseAndEmit(currentData)) return;
              currentData = payload;
            }
          } else if (line.trim() === '' && currentData) {
            if (tryParseAndEmit(currentData)) return;
            currentData = '';
          }
        }
      }
      if (tryParseAndEmit(currentData)) return;
      subject.complete();
    };

    doFetch().then(async (response) => {
      if (response.status === 401 && this.authService.isAuthenticated()) {
        try {
          await firstValueFrom(this.authService.refresh());
          // Abort the first controller before retrying. The first reader
          // (response.body.getReader()) may still be pushing tokens through
          // subject.next(); without aborting, those tokens can interleave
          // with the retry stream and garble the assistant message.
          controller.abort();
          const freshController = new AbortController();
          const retryFetch = () =>
            fetch(resolveApiUrl('/api/ai/chat'), {
              method: 'POST',
              headers: buildHeaders(),
              credentials: 'include',
              body: JSON.stringify({ message, history }),
              signal: freshController.signal,
            });
          void handleResponse(await retryFetch());
          return;
        } catch {
          // refresh failed, fall through to error display
        }
      }
      void handleResponse(response);
    }).catch((err) => {
      if ((err as Error).name !== 'AbortError') {
        subject.next({ type: 'error', content: 'Network error. Please check your connection.' });
        subject.complete();
      }
    });

    return {
      events: subject,
      abort: () => controller.abort(),
    };
  }
}

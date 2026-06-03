import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface StreamEvent {
  type: 'token' | 'done' | 'fallback' | 'error';
  content: string;
  sources?: { articleId: string; title: string; slug: string; score: number }[];
}

@Injectable({ providedIn: 'root' })
export class ChatService {

  sendMessage(message: string, history: ChatMessage[]): { events: Subject<StreamEvent>; abort: () => void } {
    const subject = new Subject<StreamEvent>();
    const controller = new AbortController();

    fetch('/api/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ message, history }),
      signal: controller.signal,
    }).then(async (response) => {
      if (!response.ok) {
        subject.next({ type: 'error', content: `HTTP ${response.status}: ${response.statusText}` });
        subject.complete();
        return;
      }

      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      let currentData = '';

      const tryParseAndEmit = (data: string): boolean => {
        if (!data) return false;
        try {
          const event: StreamEvent = JSON.parse(data);
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
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const payload = line.slice(5).trimStart();
            if (currentData && !payload.startsWith('{')) {
              // Continuation of multi-line data field (SSE spec)
              currentData += '\n' + payload;
            } else {
              // Try to parse previous accumulated data
              if (tryParseAndEmit(currentData)) return;
              currentData = payload;
            }
          } else if (line.trim() === '' && currentData) {
            // Empty line marks end of SSE event
            if (tryParseAndEmit(currentData)) return;
            currentData = '';
          }
        }
      }
      // Try to parse remaining data at end of stream
      if (tryParseAndEmit(currentData)) return;
      subject.complete();
    }).catch(err => {
      if (err.name !== 'AbortError') {
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

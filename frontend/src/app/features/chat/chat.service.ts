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

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            try {
              const event: StreamEvent = JSON.parse(line.slice(6));
              subject.next(event);
              if (event.type === 'done' || event.type === 'fallback' || event.type === 'error') {
                subject.complete();
                return;
              }
            } catch {
              // skip malformed JSON
            }
          }
        }
      }
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

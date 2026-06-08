import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ChatService, StreamEvent } from './chat.service';

describe('ChatService', () => {
  let service: ChatService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChatService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function mockFetchStream(chunks: string[], status = 200) {
    const encoder = new TextEncoder();
    const stream = new ReadableStream({
      start(controller) {
        for (const chunk of chunks) {
          controller.enqueue(encoder.encode(chunk));
        }
        controller.close();
      },
    });

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: status >= 200 && status < 300,
      status,
      statusText: status === 500 ? 'Internal Server Error' : 'OK',
      body: stream,
    });
  }

  function mockFetchError(error: Error) {
    globalThis.fetch = vi.fn().mockRejectedValue(error);
  }

  describe('SSE stream parsing', () => {
    it('should parse token events from SSE stream', async () => {
      mockFetchStream([
        'data: {"type":"token","content":"Hello"}\n\n',
        'data: {"type":"token","content":" world"}\n\n',
        'data: {"type":"done","content":""}\n\n',
      ]);

      const { events } = service.sendMessage('test', []);
      const tokens: string[] = [];

      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            if (event.type === 'token') tokens.push(event.content);
            if (event.type === 'done') resolve();
          },
        });
      });

      expect(tokens).toEqual(['Hello', ' world']);
    });

    it('should emit fallback event with sources', async () => {
      mockFetchStream([
        'data: {"type":"fallback","content":"No results found","sources":[]}\n\n',
      ]);

      const { events } = service.sendMessage('query', []);
      const received: StreamEvent[] = [];

      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            received.push(event);
            resolve();
          },
        });
      });

      expect(received.length).toBe(1);
      expect(received[0].type).toBe('fallback');
      expect(received[0].content).toBe('No results found');
    });

    it('should emit error on HTTP failure', async () => {
      mockFetchStream([], 500);

      const { events } = service.sendMessage('test', []);
      const received: StreamEvent[] = [];

      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            received.push(event);
            if (event.type === 'error') resolve();
          },
        });
      });

      expect(received.length).toBe(1);
      expect(received[0].type).toBe('error');
      expect(received[0].content).toContain('HTTP 500');
    });

    it('should complete stream after done event', async () => {
      mockFetchStream([
        'data: {"type":"token","content":"only"}\n\n',
        'data: {"type":"done","content":""}\n\n',
      ]);

      const { events } = service.sendMessage('test', []);
      let nextCount = 0;
      let completeCount = 0;

      await new Promise<void>(resolve => {
        events.subscribe({
          next: () => { nextCount++; },
          complete: () => { completeCount++; resolve(); },
        });
      });

      expect(nextCount).toBe(2); // token + done
      expect(completeCount).toBe(1);
    });

    it('should complete stream after fallback event', async () => {
      mockFetchStream([
        'data: {"type":"fallback","content":"Sorry","sources":[]}\n\n',
      ]);

      const { events } = service.sendMessage('test', []);
      let completeCount = 0;

      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            if (event.type === 'fallback') {
              // After fallback, stream should complete
            }
          },
          complete: () => { completeCount++; resolve(); },
        });
      });

      expect(completeCount).toBe(1);
    });
  });

  describe('abort functionality', () => {
    it('should abort the fetch request when abort() is called', () => {
      mockFetchStream(['data: {"type":"token","content":"streaming"}\n\n']);
      const { abort } = service.sendMessage('test', []);

      const fetchCall = vi.mocked(globalThis.fetch).mock.calls[0];
      const options = fetchCall[1] as RequestInit;
      const signal = options.signal as AbortSignal;

      abort();

      expect(signal.aborted).toBe(true);
    });

    it('should not emit error on AbortError', async () => {
      const abortError = new Error('The operation was aborted');
      abortError.name = 'AbortError';
      mockFetchError(abortError);

      const { events } = service.sendMessage('test', []);
      const received: StreamEvent[] = [];

      // Give a microtask for the catch block to execute
      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            received.push(event);
          },
          complete: () => { resolve(); },
        });
        // The fetch rejection is async, so wait a tick
        setTimeout(resolve, 50);
      });

      expect(received.length).toBe(0);
    });
  });

  describe('request payload', () => {
    it('should include message and history in request body', () => {
      mockFetchStream(['data: {"type":"done","content":""}\n\n']);
      const history = [{ role: 'user' as const, content: 'Hi' }];

      service.sendMessage('test-message', history);

      const fetchCall = vi.mocked(globalThis.fetch).mock.calls[0];
      expect(fetchCall[0]).toBe('/api/ai/chat');

      const options = fetchCall[1] as RequestInit;
      expect(options.method).toBe('POST');
      expect(options.credentials).toBe('include');

      const body = JSON.parse(options.body as string) as { message: string; history: typeof history };
      expect(body.message).toBe('test-message');
      expect(body.history).toEqual(history);
    });
  });

  describe('error handling', () => {
    it('should emit error event on network failure', async () => {
      mockFetchError(new Error('Network request failed'));

      const { events } = service.sendMessage('test', []);
      const received: StreamEvent[] = [];

      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            received.push(event);
            if (event.type === 'error') resolve();
          },
        });
        // Fallback timeout in case error doesn't fire
        setTimeout(() => resolve(), 100);
      });

      expect(received.length).toBe(1);
      expect(received[0].type).toBe('error');
      expect(received[0].content).toContain('Network error');
    });

    it('should handle empty body from server', async () => {
      globalThis.fetch = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
        body: null,
      });

      const { events } = service.sendMessage('test', []);
      const received: StreamEvent[] = [];

      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            received.push(event);
            if (event.type === 'error') resolve();
          },
        });
        setTimeout(() => resolve(), 50);
      });

      expect(received.length).toBe(1);
      expect(received[0].type).toBe('error');
    });
  });

  describe('SSE multi-line data', () => {
    it('should handle SSE data split across multiple chunks (buffer boundary)', async () => {
      mockFetchStream([
        'data: {"type":"token","con',
        'tent":"Hello"}\n\n',
        'data: {"type":"done","content":""}\n\n',
      ]);

      const { events } = service.sendMessage('test', []);
      const tokens: string[] = [];

      await new Promise<void>(resolve => {
        events.subscribe({
          next: (event) => {
            if (event.type === 'token') tokens.push(event.content);
            if (event.type === 'done') resolve();
          },
        });
      });

      expect(tokens).toEqual(['Hello']);
    });
  });
});

import { Component, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgFor, NgIf, NgClass } from '@angular/common';
import { MarkdownModule } from 'ngx-markdown';
import { ChatService, ChatMessage, StreamEvent } from './chat.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [FormsModule, NgFor, NgIf, NgClass, MarkdownModule],
  templateUrl: './chat.component.html',
})
export class ChatComponent implements AfterViewChecked {
  private chatService = inject(ChatService);

  @ViewChild('chatContainer') private chatContainer!: ElementRef;

  messages = signal<ChatMessage[]>([]);
  currentInput = '';
  isStreaming = signal(false);
  showFeedback = signal(false);
  lastAiMessage = signal('');
  showFallback = signal(false);
  escalationPayload = signal<any>(null);
  errorMessage = signal<string | null>(null);

  private streamSub: Subscription | null = null;
  private abortStream: (() => void) | null = null;

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  sendMessage() {
    const text = this.currentInput.trim();
    if (!text || this.isStreaming()) return;

    this.currentInput = '';
    this.errorMessage.set(null);
    this.showFeedback.set(false);
    this.showFallback.set(false);

    const userMsg: ChatMessage = { role: 'user', content: text };
    this.messages.update(m => [...m, userMsg]);

    if (this.abortStream) this.abortStream();

    const history = this.messages().slice(-10).map(m => ({ role: m.role, content: m.content }));

    this.isStreaming.set(true);

    const assistantMsg: ChatMessage = { role: 'assistant', content: '' };
    this.messages.update(m => [...m, assistantMsg]);

    const { events, abort } = this.chatService.sendMessage(text, history);
    this.abortStream = abort;

    this.streamSub = events.subscribe({
      next: (event) => {
        if (event.type === 'token') {
          assistantMsg.content += event.content;
          this.messages.update(m => [...m]);
        } else if (event.type === 'done') {
          this.isStreaming.set(false);
          this.lastAiMessage.set(assistantMsg.content);
          this.showFeedback.set(true);
          this.setEscalationPayload(event);
        } else if (event.type === 'fallback') {
          this.isStreaming.set(false);
          assistantMsg.content = event.content;
          this.showFallback.set(true);
          this.setEscalationPayload(event);
        } else if (event.type === 'error') {
          this.isStreaming.set(false);
          this.errorMessage.set(event.content);
          this.messages.update(m => m.slice(0, -1));
        }
      },
      error: () => {
        this.isStreaming.set(false);
        this.errorMessage.set('Connection error. Please try again.');
      },
      complete: () => {
        this.isStreaming.set(false);
      },
    });
  }

  private setEscalationPayload(event: StreamEvent) {
    this.escalationPayload.set({
      issue: this.messages().find(m => m.role === 'user')?.content || '',
      transcript: this.messages(),
      sources: event.sources || [],
    });
  }

  handleFeedback(yes: boolean) {
    this.showFeedback.set(false);
    if (!yes) {
      const followUp = this.messages().find(m => m.role === 'user')?.content || '';
      this.currentInput = "The user indicated this did not solve their problem. Original issue: " + followUp;
      this.sendMessage();
    }
  }

  retry() {
    this.errorMessage.set(null);
    const lastUserMsg = [...this.messages()].reverse().find(m => m.role === 'user');
    if (lastUserMsg) {
      this.currentInput = lastUserMsg.content;
      const lastIdx = this.messages().lastIndexOf(lastUserMsg);
      this.messages.update(m => m.slice(0, lastIdx));
      this.sendMessage();
    }
  }

  private scrollToBottom() {
    try {
      this.chatContainer?.nativeElement.scrollTo({
        top: this.chatContainer.nativeElement.scrollHeight,
        behavior: 'smooth',
      });
    } catch { }
  }

  trackByFn(index: number, _msg: ChatMessage) {
    return index;
  }
}

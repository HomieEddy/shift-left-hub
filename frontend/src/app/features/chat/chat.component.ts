import { Component, inject, signal, effect, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgFor, NgIf } from '@angular/common';
import { MarkdownModule } from 'ngx-markdown';
import { ChatService, ChatMessage, StreamEvent } from './chat.service';
import { Subscription } from 'rxjs';

interface EscalationPayload {
  issue: string;
  transcript: ChatMessage[];
  sources: { articleId: string; title: string; slug: string; score: number }[];
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [FormsModule, NgFor, NgIf, MarkdownModule],
  templateUrl: './chat.component.html',
})
export class ChatComponent {
  private chatService = inject(ChatService);

  @ViewChild('chatContainer') private chatContainer!: ElementRef;

  messages = signal<ChatMessage[]>([]);
  currentInput = '';
  isStreaming = signal(false);
  showFeedback = signal(false);
  showFollowUp = signal(false);
  showCloseModal = signal(false);
  showFallback = signal(false);
  escalationPayload = signal<EscalationPayload | null>(null);
  errorMessage = signal<string | null>(null);

  private nextId = 0;
  private streamSub: Subscription | null = null;
  private abortStream: (() => void) | null = null;

  private scrollEffect = effect(() => {
    this.messages();
    this.scrollToBottom();
  });

  sendMessage() {
    const text = this.currentInput.trim();
    if (!text || this.isStreaming()) return;

    this.currentInput = '';
    this.errorMessage.set(null);
    this.escalationPayload.set(null);
    this.showFeedback.set(false);
    this.showFollowUp.set(false);
    this.showCloseModal.set(false);
    this.showFallback.set(false);

    const userMsg: ChatMessage = { id: `msg-${++this.nextId}`, role: 'user', content: text };
    this.messages.update(m => [...m, userMsg]);

    if (this.abortStream) this.abortStream();

    const history = this.messages().slice(-10).map(m => ({ role: m.role, content: m.content }));

    this.isStreaming.set(true);

    const assistantMsg: ChatMessage = { id: `msg-${++this.nextId}`, role: 'assistant', content: '' };
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
    const userMessages = this.messages().filter(m => m.role === 'user');
    const lastUserContent = userMessages.length > 0
      ? userMessages[userMessages.length - 1].content
      : '';
    this.escalationPayload.set({
      issue: lastUserContent,
      transcript: this.messages(),
      sources: event.sources || [],
    });
  }

  handleFeedback(yes: boolean) {
    this.showFeedback.set(false);
    if (yes) {
      this.showFollowUp.set(true);
    } else {
      // TODO: When wiring up AI context in Phase 4, include the full conversation transcript
      // as a system instruction rather than injecting a fabricated user message.
      const userMessages = this.messages().filter(m => m.role === 'user');
      const lastUserContent = userMessages.length > 0
        ? userMessages[userMessages.length - 1].content
        : '';
      this.currentInput = "The user indicated this did not solve their problem. Original issue: " + lastUserContent;
      this.sendMessage();
    }
  }

  handleFollowUp(yes: boolean) {
    this.showFollowUp.set(false);
    if (yes) {
      this.currentInput = '';
    } else {
      this.showCloseModal.set(true);
    }
  }

  closeModal() {
    this.showCloseModal.set(false);
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
    } catch (e) {
        console.warn('Scroll failed:', e);
      }
  }

  trackByFn(_index: number, msg: ChatMessage) {
    return msg.id || _index;
  }
}

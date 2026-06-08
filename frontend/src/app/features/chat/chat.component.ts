import { Component, inject, signal, effect, output, DestroyRef, HostListener, ViewChild, ElementRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MarkdownModule } from 'ngx-markdown';
import { EscalationFormComponent } from '../tickets/escalation-form/escalation-form.component';
import { ChatService, ChatMessage, StreamEvent } from './chat.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [FormsModule, RouterLink, MarkdownModule, EscalationFormComponent],
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
  showEscalationForm = signal(false);
  escalationPayload = signal<{ issue: string; transcript: ChatMessage[]; sources: { articleId: string; title: string; slug: string; score: number }[] } | null>(null);
  showTicketConfirmation = signal(false);
  createdTicketNumber = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  readonly escalate = output<{ issue: string; transcript: ChatMessage[]; sources: { articleId: string; title: string; slug: string; score: number }[] }>();

  private destroyRef = inject(DestroyRef);
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

    const history = this.messages().slice(-10).map(m => ({ role: m.role, content: m.content }));

    const userMsg: ChatMessage = { id: `msg-${++this.nextId}`, role: 'user', content: text };
    this.messages.update(m => [...m, userMsg]);

    if (this.abortStream) this.abortStream();

    this.isStreaming.set(true);

    const assistantMsg: ChatMessage = { id: `msg-${++this.nextId}`, role: 'assistant', content: '' };
    this.messages.update(m => [...m, assistantMsg]);

    const { events, abort } = this.chatService.sendMessage(text, history);
    this.abortStream = abort;

    this.streamSub?.unsubscribe();
    this.streamSub = events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (event) => {
        if (event.type === 'token') {
          this.messages.update(m => {
            const updated = [...m];
            const lastIdx = updated.length - 1;
            updated[lastIdx] = { ...updated[lastIdx], content: updated[lastIdx].content + event.content };
            return updated;
          });
        } else if (event.type === 'done') {
          this.isStreaming.set(false);
          this.showFeedback.set(true);
          this.setEscalationPayload(event);
        } else if (event.type === 'fallback') {
          this.isStreaming.set(false);
          this.messages.update(m => {
            const updated = [...m];
            const lastIdx = updated.length - 1;
            updated[lastIdx] = { ...updated[lastIdx], content: event.content };
            return updated;
          });
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

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.showCloseModal()) {
      this.closeModal();
    }
    if (this.showTicketConfirmation()) {
      this.closeTicketConfirmation();
    }
  }

  escalateToHumanAgent() {
    this.showEscalationForm.set(true);
  }

  closeEscalationForm() {
    this.showEscalationForm.set(false);
  }

  onTicketCreated(ticketNumber: string) {
    this.showEscalationForm.set(false);
    this.showFallback.set(false);
    this.createdTicketNumber.set(ticketNumber);
    this.showTicketConfirmation.set(true);
  }

  startNewChat() {
    this.showTicketConfirmation.set(false);
    this.createdTicketNumber.set(null);
    this.messages.set([]);
  }

  closeTicketConfirmation() {
    this.showTicketConfirmation.set(false);
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
      const el = this.chatContainer?.nativeElement as HTMLElement | undefined;
      if (el != null) {
        el.scrollTo({
          top: el.scrollHeight,
          behavior: 'smooth',
        });
      }
    } catch (e) {
        console.warn('Scroll failed:', e);
      }
  }

  trackByFn(_index: number, msg: ChatMessage) {
    return msg.id ?? _index;
  }
}

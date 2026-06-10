import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { vi } from 'vitest';
import { ChatService, StreamEvent } from './chat.service';
import { ChatComponent } from './chat.component';

describe('ChatComponent', () => {
  let component: ChatComponent;
  let fixture: ComponentFixture<ChatComponent>;
  let chatService: { sendMessage: ReturnType<typeof vi.fn> };
  let eventsSubject: Subject<StreamEvent>;

  const mockStreamEvent = (type: StreamEvent['type'], content = '', sources: { articleId: string; title: string; slug: string; score: number }[] = []): StreamEvent => ({
    type, content, sources,
  });

  beforeEach(async () => {
    eventsSubject = new Subject<StreamEvent>();
    chatService = {
      sendMessage: vi.fn().mockReturnValue({
        events: eventsSubject.asObservable(),
        abort: vi.fn(),
      }),
    };

    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [
        { provide: ChatService, useValue: chatService },
        provideHttpClient(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call chatService.sendMessage on sendMessage()', () => {
    component.currentInput = 'How do I reset my password?';
    component.sendMessage();

    expect(chatService.sendMessage).toHaveBeenCalledWith(
      'How do I reset my password?',
      expect.any(Array),
    );
    expect(component.isStreaming()).toBe(true);
  });

  it('should append user message and create assistant message slot', () => {
    component.currentInput = 'Hello';
    component.sendMessage();

    const msgs = component.messages();
    expect(msgs.length).toBe(2);
    expect(msgs[0].role).toBe('user');
    expect(msgs[0].content).toBe('Hello');
    expect(msgs[1].role).toBe('assistant');
    expect(msgs[1].content).toBe('');
  });

  it('should append token events to assistant message', () => {
    component.currentInput = 'Hello';
    component.sendMessage();

    eventsSubject.next(mockStreamEvent('token', 'Step 1: '));
    eventsSubject.next(mockStreamEvent('token', 'restart your computer.'));

    const msgs = component.messages();
    expect(msgs[msgs.length - 1].content).toBe('Step 1: restart your computer.');
  });

  it('should set showFeedback on done event', () => {
    component.currentInput = 'Hello';
    component.sendMessage();

    eventsSubject.next(mockStreamEvent('done', '', [{ articleId: '1', title: 'Guide', slug: 'guide', score: 0.9 }]));

    expect(component.isStreaming()).toBe(false);
    expect(component.showFeedback()).toBe(true);
    expect(component.escalationPayload()).not.toBeNull();
  });

  it('should show fallback on fallback event', () => {
    component.currentInput = 'unknown issue';
    component.sendMessage();

    eventsSubject.next(mockStreamEvent('fallback', 'No results found'));

    expect(component.showFallback()).toBe(true);
    expect(component.isStreaming()).toBe(false);
  });

  it('should show error on error event', () => {
    component.currentInput = 'Hello';
    component.sendMessage();

    eventsSubject.next(mockStreamEvent('error', 'Server error'));

    expect(component.errorMessage()).toBe('Server error');
    expect(component.messages().length).toBe(1);
  });

  it('should not send empty messages', () => {
    component.currentInput = '   ';
    component.sendMessage();

    expect(chatService.sendMessage).not.toHaveBeenCalled();
  });

  it('should not send while streaming', () => {
    component.currentInput = 'msg1';
    component.sendMessage();

    component.currentInput = 'msg2';
    component.sendMessage();

    expect(chatService.sendMessage).toHaveBeenCalledTimes(1);
  });

  it('should open escalation form', () => {
    component.escalateToHumanAgent();
    expect(component.showEscalationForm()).toBe(true);
  });
});

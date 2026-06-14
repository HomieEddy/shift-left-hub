import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { TicketService } from '../ticket.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { EscalationFormComponent } from './escalation-form.component';

describe('EscalationFormComponent', () => {
  let component: EscalationFormComponent;
  let fixture: ComponentFixture<EscalationFormComponent>;
  let ticketService: { createTicket: ReturnType<typeof vi.fn> };
  let translationService: { translate: ReturnType<typeof vi.fn> };

  const mockTicket = {
    id: 't1',
    ticketNumber: 'TKT-0042',
    status: 'NEW' as const,
    category: 'NETWORK' as const,
    urgency: 'LOW' as const,
    issue: 'Cannot connect to VPN',
    userId: 'u1',
    userDisplayName: 'User',
    shiftLeftContext: undefined,
    resolvedAt: null,
    cancelledAt: null,
    cancelReason: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    ticketService = {
      createTicket: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => {
        const map: Record<string, string> = {
          'tickets.category.network': 'Network',
          'tickets.category.hardware': 'Hardware',
          'tickets.category.software': 'Software',
          'tickets.category.access': 'Access',
          'tickets.category.peripherals': 'Peripherals',
          'agent.urgency.low': 'Low',
          'agent.urgency.medium': 'Medium',
          'agent.urgency.high': 'High',
          'escalation.error.create': 'Failed to create ticket',
        };
        return map[key] ?? key;
      }),
    };

    await TestBed.configureTestingModule({
      imports: [EscalationFormComponent],
      providers: [
        { provide: TicketService, useValue: ticketService },
        { provide: TranslationService, useValue: translationService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EscalationFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not submit when issue is empty', () => {
    component.issue.set('');
    component.category.set('NETWORK');
    component.urgency.set('LOW');

    component.submit();

    expect(ticketService.createTicket).not.toHaveBeenCalled();
  });

  it('should not submit when issue is whitespace only', () => {
    component.issue.set('   ');
    component.category.set('NETWORK');
    component.urgency.set('LOW');

    component.submit();

    expect(ticketService.createTicket).not.toHaveBeenCalled();
  });

  it('should submit escalation with required fields', () => {
    ticketService.createTicket.mockReturnValue(of(mockTicket));
    component.issue.set('Cannot connect to VPN');

    component.submit();

    expect(ticketService.createTicket).toHaveBeenCalledWith({
      issue: 'Cannot connect to VPN',
      category: 'NETWORK',
      urgency: 'LOW',
      shiftLeftContext: undefined,
    });
    expect(component.isSubmitting()).toBe(false);
    expect(component.successTicketNumber()).toBe('TKT-0042');
  });

  it('should submit escalation with AI context', () => {
    ticketService.createTicket.mockReturnValue(of(mockTicket));

    const escalationPayload = {
      issue: 'VPN issue',
      transcript: [{ role: 'user', content: 'VPN is down' }],
      sources: [{ articleId: 'a1', title: 'VPN Setup', slug: 'vpn-setup', score: 0.85 }],
    };
    fixture.componentRef.setInput('escalationPayload', escalationPayload);

    component.issue.set('VPN still broken');

    component.submit();

    expect(ticketService.createTicket).toHaveBeenCalled();
    const callArg: Record<string, unknown> = ticketService.createTicket.mock.calls[0][0] as Record<string, unknown>;
    expect(callArg['issue']).toBe('VPN still broken');
    expect(callArg['shiftLeftContext']).toBeDefined();

    const parsed: Record<string, unknown> = JSON.parse(callArg['shiftLeftContext'] as string) as Record<string, unknown>;
    expect(parsed['issue']).toBe('VPN issue');
    expect(parsed['transcript']).toEqual([{ role: 'user', content: 'VPN is down' }]);
  });

  it('should show submitting state during submit', () => {
    const pendingSubject = new Subject<unknown>();
    ticketService.createTicket.mockReturnValue(pendingSubject.asObservable());

    component.issue.set('Cannot connect');
    component.submit();

    expect(component.isSubmitting()).toBe(true);
  });

  it('should handle submit error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    ticketService.createTicket.mockReturnValue(errorSubject.asObservable());

    component.issue.set('Cannot connect to VPN');
    component.submit();

    errorSubject.error(new Error('API error'));

    expect(component.isSubmitting()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to create ticket');
    expect(component.successTicketNumber()).toBeNull();
  });

  it('should emit ticketCreated after successful submit', () => {
    const emitted = vi.fn();
    component.ticketCreated.subscribe(emitted);

    ticketService.createTicket.mockReturnValue(of(mockTicket));
    component.issue.set('Cannot connect');

    component.submit();

    expect(emitted).toHaveBeenCalledWith('TKT-0042');
  });

  it('should translate labels', () => {
    const cats = component.categories();
    expect(cats.length).toBe(5);
    expect(cats[0].label).toBe('Network');

    const urgs = component.urgencies();
    expect(urgs.length).toBe(3);
    expect(urgs[0].label).toBe('Low');
  });
});

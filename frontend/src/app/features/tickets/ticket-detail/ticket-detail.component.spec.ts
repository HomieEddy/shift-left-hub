import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { MarkdownModule } from 'ngx-markdown';
import { TicketService } from '../ticket.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { TicketDetailComponent } from './ticket-detail.component';

describe('TicketDetailComponent', () => {
  let component: TicketDetailComponent;
  let fixture: ComponentFixture<TicketDetailComponent>;
  let ticketService: {
    getTicket: ReturnType<typeof vi.fn>;
    cancelTicket: ReturnType<typeof vi.fn>;
  };
  let translationService: {
    translate: ReturnType<typeof vi.fn>;
    currentLang: ReturnType<typeof vi.fn>;
  };
  let router: { navigate: ReturnType<typeof vi.fn> };

  const mockTicket = {
    id: 't1',
    ticketNumber: 'TKT-0042',
    status: 'NEW' as const,
    category: 'NETWORK' as const,
    urgency: 'HIGH' as const,
    issue: 'Cannot connect to VPN from office',
    userId: 'u1',
    userDisplayName: 'John Doe',
    shiftLeftContext: JSON.stringify({
      issue: 'VPN issue',
      category: 'NETWORK',
      urgency: 'HIGH',
      transcript: [{ role: 'user', content: 'VPN is down' }],
      sources: [{ articleId: 'a1', title: 'VPN Setup', slug: 'vpn-setup', score: 0.85 }],
      aiSummary: 'User cannot connect to VPN',
      confidenceScore: 0.3,
    }),
    resolvedAt: null,
    cancelledAt: null,
    cancelReason: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    ticketService = {
      getTicket: vi.fn(),
      cancelTicket: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => {
        const map: Record<string, string> = {
          'tickets.status.NEW': 'New',
          'tickets.category.NETWORK': 'Network',
          'tickets.urgency.HIGH': 'High',
          'tickets.detail.error.load': 'Failed to load ticket',
          'tickets.detail.error.cancel': 'Failed to cancel ticket',
        };
        return map[key] ?? key;
      }),
      currentLang: vi.fn().mockReturnValue('en'),
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [TicketDetailComponent, MarkdownModule],
      providers: [
        { provide: TicketService, useValue: ticketService },
        { provide: TranslationService, useValue: translationService },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', 't1']]) } } },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketDetailComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    ticketService.getTicket.mockReturnValue(of(mockTicket));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load ticket on init', () => {
    ticketService.getTicket.mockReturnValue(of(mockTicket));
    fixture.detectChanges();

    expect(ticketService.getTicket).toHaveBeenCalledWith('t1');
    expect(component.ticket()?.ticketNumber).toBe('TKT-0042');
    expect(component.isLoading()).toBe(false);
  });

  it('should show loading and loaded states', () => {
    const pendingSubject = new Subject<unknown>();
    ticketService.getTicket.mockReturnValue(pendingSubject.asObservable());

    fixture.detectChanges();

    expect(component.isLoading()).toBe(true);

    pendingSubject.next(mockTicket);
    pendingSubject.complete();

    expect(component.isLoading()).toBe(false);
    expect(component.ticket()).toEqual(mockTicket);
  });

  it('should display ticket details', () => {
    ticketService.getTicket.mockReturnValue(of(mockTicket));
    fixture.detectChanges();

    expect(component.statusLabel('NEW')).toBe('New');
    expect(component.categoryLabel('NETWORK')).toBe('Network');
    expect(component.urgencyLabel('HIGH')).toBe('High');
    expect(component.parsedContext()?.transcript.length).toBe(1);
    expect(component.transcriptMessages().length).toBe(1);
    expect(component.kbSources().length).toBe(1);
  });

  it('should cancel ticket', () => {
    ticketService.getTicket.mockReturnValue(of(mockTicket));
    fixture.detectChanges();

    ticketService.cancelTicket.mockReturnValue(of({ ...mockTicket, status: 'CANCELLED' }));

    component.cancelTicket();

    expect(ticketService.cancelTicket).toHaveBeenCalledWith('t1');
    expect(router.navigate).toHaveBeenCalledWith(['/tickets']);
  });

  it('should handle cancel error gracefully', () => {
    ticketService.getTicket.mockReturnValue(of(mockTicket));
    fixture.detectChanges();

    const errorSubject = new Subject<unknown>();
    ticketService.cancelTicket.mockReturnValue(errorSubject.asObservable());

    component.cancelTicket();
    errorSubject.error(new Error('Cancel failed'));

    expect(component.errorMessage()).toBe('Failed to cancel ticket');
  });

  it('should handle ticket load error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    ticketService.getTicket.mockReturnValue(errorSubject.asObservable());

    fixture.detectChanges();
    errorSubject.error(new Error('Network error'));

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Failed to load ticket');
    expect(component.ticket()).toBeNull();
  });

  it('should handle ticket not found', () => {
    const errorSubject = new Subject<unknown>();
    ticketService.getTicket.mockReturnValue(errorSubject.asObservable());

    fixture.detectChanges();
    errorSubject.error(new Error('404'));

    expect(component.errorMessage()).toBe('Failed to load ticket');
    expect(component.ticket()).toBeNull();
  });

  it('should translate labels', () => {
    ticketService.getTicket.mockReturnValue(of(mockTicket));
    fixture.detectChanges();

    expect(component.statusLabel('NEW')).toBe('New');
    expect(component.categoryLabel('NETWORK')).toBe('Network');
    expect(component.urgencyLabel('HIGH')).toBe('High');
  });
});

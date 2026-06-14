import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AgentTicketDetailComponent } from './agent-ticket-detail.component';
import { AgentTicketService } from '../agent-ticket.service';
import { TranslationService } from '../../../core/i18n/translation.service';

const mockTicket = {
  id: 'ticket-123',
  ticketNumber: 'TKT-0042',
  status: 'NEW' as const,
  category: 'NETWORK',
  urgency: 'HIGH',
  issue: 'Cannot connect to VPN',
  shiftLeftContext: JSON.stringify({
    issue: 'VPN timeout',
    category: 'NETWORK',
    urgency: 'HIGH',
    transcript: [{ role: 'user', content: 'VPN not working' }],
    sources: [{ articleId: 'a1', title: 'VPN Guide', slug: 'vpn-guide', score: 0.85 }],
    aiSummary: 'User is unable to connect to VPN.',
    confidenceScore: 0.75,
  }),
  userId: 'u1',
  userDisplayName: 'Test User',
  userEmail: 'test@example.com',
  assignedToId: null,
  assignedToDisplayName: null,
  resolvedById: null,
  resolvedByDisplayName: null,
  resolutionNotes: null,
  isKnowledgeGap: false,
  resolvedAt: null,
  cancelledAt: null,
  cancelReason: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const mockNote = {
  id: 'n1',
  authorDisplayName: 'Agent Smith',
  content: 'Checking logs...',
  createdAt: '2026-01-02T00:00:00Z',
};

describe('AgentTicketDetailComponent', () => {
  let component: AgentTicketDetailComponent;
  let fixture: ComponentFixture<AgentTicketDetailComponent>;
  let agentTicketService: {
    getTicket: ReturnType<typeof vi.fn>;
    getWorkNotes: ReturnType<typeof vi.fn>;
    addWorkNote: ReturnType<typeof vi.fn>;
    claimTicket: ReturnType<typeof vi.fn>;
    resolveTicket: ReturnType<typeof vi.fn>;
  };
  let translationService: { translate: ReturnType<typeof vi.fn> };
  let activatedRoute: { snapshot: { paramMap: { get: ReturnType<typeof vi.fn> } } };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    agentTicketService = {
      getTicket: vi.fn(),
      getWorkNotes: vi.fn(),
      addWorkNote: vi.fn(),
      claimTicket: vi.fn(),
      resolveTicket: vi.fn(),
    };
    translationService = { translate: vi.fn(() => 'translated') };
    activatedRoute = {
      snapshot: {
        paramMap: { get: vi.fn().mockReturnValue('ticket-123') },
      },
    };
    router = { navigate: vi.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [AgentTicketDetailComponent],
      providers: [
        { provide: AgentTicketService, useValue: agentTicketService },
        { provide: TranslationService, useValue: translationService },
        { provide: ActivatedRoute, useValue: activatedRoute },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    agentTicketService.getTicket.mockReturnValue(of(mockTicket));
    agentTicketService.getWorkNotes.mockReturnValue(of([mockNote]));

    fixture = TestBed.createComponent(AgentTicketDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load ticket and work notes on init', () => {
    expect(agentTicketService.getTicket).toHaveBeenCalledWith('ticket-123');
    expect(agentTicketService.getWorkNotes).toHaveBeenCalledWith('ticket-123');
    expect(component.ticket()?.id).toBe('ticket-123');
    expect(component.workNotes().length).toBe(1);
    expect(component.isLoading()).toBe(false);
  });

  it('should show loading and loaded states', () => {
    const pendingTicket = new Subject<unknown>();
    agentTicketService.getTicket.mockReturnValue(pendingTicket.asObservable());
    agentTicketService.getWorkNotes.mockReturnValue(of([]));

    component.loadTicket('ticket-123');

    expect(component.isLoading()).toBe(true);

    pendingTicket.next(mockTicket);
    pendingTicket.complete();

    expect(component.isLoading()).toBe(false);
    expect(component.ticket()?.id).toBe('ticket-123');
  });

  it('should handle ticket load error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    agentTicketService.getTicket.mockReturnValue(errorSubject.asObservable());

    component.loadTicket('ticket-123');
    errorSubject.error(new Error('Failed'));

    expect(component.isLoading()).toBe(false);
    expect(component.isError()).toBe(true);
  });

  it('should handle work notes load error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    agentTicketService.getWorkNotes.mockReturnValue(errorSubject.asObservable());

    component.loadWorkNotes('ticket-123');
    errorSubject.error(new Error('Failed'));

    expect(component.workNoteError()).toBe('Failed to load work notes.');
  });

  it('should call claimTicket on claim', () => {
    agentTicketService.claimTicket.mockReturnValue(of({ ...mockTicket, status: 'IN_PROGRESS' }));

    component.claimTicket();

    expect(agentTicketService.claimTicket).toHaveBeenCalledWith('ticket-123');
    expect(component.ticket()?.status).toBe('IN_PROGRESS');
    expect(component.isClaiming()).toBe(false);
  });

  it('should handle claim error gracefully', () => {
    agentTicketService.claimTicket.mockReturnValue(of(mockTicket));

    component.claimTicket();

    expect(component.isClaiming()).toBe(false);
  });

  it('should add work note with content', () => {
    component.newWorkNote.set('Checking connectivity...');
    agentTicketService.addWorkNote.mockReturnValue(of({}));
    agentTicketService.getWorkNotes.mockReturnValue(of([mockNote]));
    // set ticket to IN_PROGRESS so the work note section is relevant
    const ticketSubject = new Subject<unknown>();
    agentTicketService.getTicket.mockReturnValue(ticketSubject.asObservable());
    component.loadTicket('ticket-123');
    ticketSubject.next({ ...mockTicket, status: 'IN_PROGRESS' });

    component.addWorkNote();

    expect(agentTicketService.addWorkNote).toHaveBeenCalledWith('ticket-123', 'Checking connectivity...');
    expect(component.newWorkNote()).toBe('');
    expect(component.isSubmittingNote()).toBe(false);
  });

  it('should not add work note with empty content', () => {
    component.newWorkNote.set('   ');
    component.addWorkNote();
    expect(agentTicketService.addWorkNote).not.toHaveBeenCalled();
  });

  it('should open and confirm resolve dialog', () => {
    // set ticket to IN_PROGRESS
    const ticketSubject = new Subject<unknown>();
    agentTicketService.getTicket.mockReturnValue(ticketSubject.asObservable());
    component.loadTicket('ticket-123');
    ticketSubject.next({ ...mockTicket, status: 'IN_PROGRESS' });

    component.openResolveConfirm();
    expect(component.resolveConfirmOpen()).toBe(true);

    component.resolutionNotes.set('Fixed by restarting VPN service');
    agentTicketService.resolveTicket.mockReturnValue(of({ ...mockTicket, status: 'RESOLVED' }));

    component.confirmResolve();

    expect(agentTicketService.resolveTicket).toHaveBeenCalledWith('ticket-123', {
      resolutionNotes: 'Fixed by restarting VPN service',
      isKnowledgeGap: false,
    });
    expect(component.ticket()?.status).toBe('RESOLVED');
    expect(component.resolveConfirmOpen()).toBe(false);
  });

  it('should cancel resolve dialog without resolving', () => {
    component.openResolveConfirm();
    expect(component.resolveConfirmOpen()).toBe(true);

    component.cancelResolveConfirm();
    expect(component.resolveConfirmOpen()).toBe(false);
    expect(agentTicketService.resolveTicket).not.toHaveBeenCalled();
  });

  it('should parse shiftLeftContext JSON', () => {
    expect(component.parsedContext()).not.toBeNull();
    expect(component.parsedContext()?.issue).toBe('VPN timeout');
    expect(component.transcriptMessages().length).toBe(1);
    expect(component.transcriptMessages()[0].content).toBe('VPN not working');
  });

  it('should return null parsedContext when shiftLeftContext is absent', () => {
    agentTicketService.getTicket.mockReturnValue(of({ ...mockTicket, shiftLeftContext: undefined }));
    component.loadTicket('ticket-123');
    expect(component.parsedContext()).toBeNull();
  });

  it('should translate labels via TranslationService', () => {
    expect(component.loadingLabel()).toBe('translated');
    expect(translationService.translate).toHaveBeenCalledWith('agent.detail.loading');
    expect(component.claimTicketLabel()).toBe('translated');
    expect(translationService.translate).toHaveBeenCalledWith('agent.detail.claimTicket');
  });
});

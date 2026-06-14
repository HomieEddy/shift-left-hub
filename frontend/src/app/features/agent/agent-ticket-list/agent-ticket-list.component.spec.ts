import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AgentTicketListComponent } from './agent-ticket-list.component';
import { AgentTicketService } from '../agent-ticket.service';
import { TranslationService } from '../../../core/i18n/translation.service';

const mockTickets = [
  {
    id: '1',
    ticketNumber: 'TKT-0001',
    status: 'NEW' as const,
    category: 'NETWORK',
    urgency: 'HIGH',
    issue: 'Cannot connect to VPN',
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
  },
  {
    id: '2',
    ticketNumber: 'TKT-0002',
    status: 'IN_PROGRESS' as const,
    category: 'SOFTWARE',
    urgency: 'MEDIUM',
    issue: 'Excel crash on save',
    userId: 'u2',
    userDisplayName: 'User 2',
    userEmail: 'user2@example.com',
    assignedToId: 'agent-1',
    assignedToDisplayName: 'Agent One',
    resolvedById: null,
    resolvedByDisplayName: null,
    resolutionNotes: null,
    isKnowledgeGap: false,
    resolvedAt: null,
    cancelledAt: null,
    cancelReason: null,
    createdAt: '2026-01-02T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
  },
  {
    id: '3',
    ticketNumber: 'TKT-0003',
    status: 'RESOLVED' as const,
    category: 'HARDWARE',
    urgency: 'LOW',
    issue: 'Monitor flickering',
    userId: 'u1',
    userDisplayName: 'Test User',
    userEmail: 'test@example.com',
    assignedToId: 'agent-1',
    assignedToDisplayName: 'Agent One',
    resolvedById: 'agent-1',
    resolvedByDisplayName: 'Agent One',
    resolutionNotes: 'Replaced cable',
    isKnowledgeGap: false,
    resolvedAt: '2026-01-04T00:00:00Z',
    cancelledAt: null,
    cancelReason: null,
    createdAt: '2026-01-03T00:00:00Z',
    updatedAt: '2026-01-03T00:00:00Z',
  },
];

describe('AgentTicketListComponent', () => {
  let component: AgentTicketListComponent;
  let fixture: ComponentFixture<AgentTicketListComponent>;
  let agentTicketService: {
    getTickets: ReturnType<typeof vi.fn>;
    claimTicket: ReturnType<typeof vi.fn>;
  };
  let translationService: { translate: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    agentTicketService = {
      getTickets: vi.fn(),
      claimTicket: vi.fn(),
    };
    translationService = { translate: vi.fn(() => 'translated') };

    await TestBed.configureTestingModule({
      imports: [AgentTicketListComponent],
      providers: [
        { provide: AgentTicketService, useValue: agentTicketService },
        { provide: TranslationService, useValue: translationService },
        provideRouter([]),
      ],
    }).compileComponents();

    agentTicketService.getTickets.mockReturnValue(of(mockTickets));

    fixture = TestBed.createComponent(AgentTicketListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load tickets on init', () => {
    expect(agentTicketService.getTickets).toHaveBeenCalled();
    expect(component.tickets().length).toBe(3);
    expect(component.filteredTickets().length).toBe(3);
    expect(component.isLoading()).toBe(false);
  });

  it('should filter by status tab', () => {
    component.changeStatusTab('NEW');
    expect(agentTicketService.getTickets).toHaveBeenCalledWith(
      expect.objectContaining({ status: 'NEW' }),
    );
    expect(component.activeStatus()).toBe('NEW');
  });

  it('should change category filter and reload', () => {
    component.selectedCategory.set('NETWORK');
    component.onCategoryChange();
    expect(agentTicketService.getTickets).toHaveBeenCalledWith(
      expect.objectContaining({ category: 'NETWORK' }),
    );
  });

  it('should change urgency filter and reload', () => {
    component.selectedUrgency.set('HIGH');
    component.onUrgencyChange();
    expect(agentTicketService.getTickets).toHaveBeenCalledWith(
      expect.objectContaining({ urgency: 'HIGH' }),
    );
  });

  it('should debounce search input — onSearchInput fires with debounce', () => {
    vi.useFakeTimers();
    component.onSearchInput('vpn issue');
    expect(component.searchQuery()).toBe('');
    vi.advanceTimersByTime(300);
    expect(component.searchQuery()).toBe('vpn issue');
    expect(agentTicketService.getTickets).toHaveBeenCalledWith(
      expect.objectContaining({ search: 'vpn issue' }),
    );
    vi.useRealTimers();
  });

  it('should retry load after error', () => {
    const errorSubject = new Subject<unknown>();
    agentTicketService.getTickets.mockReturnValue(errorSubject.asObservable());

    component.loadTickets();
    errorSubject.error(new Error('Failed'));

    expect(component.isError()).toBe(true);
    expect(component.isLoading()).toBe(false);

    agentTicketService.getTickets.mockReturnValue(of(mockTickets));
    component.loadTickets();
    expect(component.isError()).toBe(false);
    expect(component.isLoading()).toBe(false);
    expect(component.tickets().length).toBe(3);
  });

  it('should open claim confirmation dialog', () => {
    component.openClaimConfirm('1');
    expect(component.claimConfirmOpen()).toBe(true);
    expect(component.claimingTicketId()).toBe('1');
  });

  it('should confirm claim and navigate to detail', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    agentTicketService.claimTicket.mockReturnValue(of(undefined));

    component.openClaimConfirm('1');
    component.confirmClaim();

    expect(agentTicketService.claimTicket).toHaveBeenCalledWith('1');
    expect(navigateSpy).toHaveBeenCalledWith(['/agent/tickets', '1']);
    expect(component.claimConfirmOpen()).toBe(false);
    expect(component.claimingTicketId()).toBeNull();
  });

  it('should cancel claim without navigating', () => {
    component.openClaimConfirm('1');
    component.cancelClaim();

    expect(component.claimConfirmOpen()).toBe(false);
    expect(component.claimingTicketId()).toBeNull();
    expect(agentTicketService.claimTicket).not.toHaveBeenCalled();
  });

  it('should handle claim error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    agentTicketService.claimTicket.mockReturnValue(errorSubject.asObservable());

    component.openClaimConfirm('1');
    component.confirmClaim();
    errorSubject.error(new Error('Failed'));

    expect(component.claimError()).toBe('Failed to claim ticket. Please try again.');
  });

  it('should handle ticket load error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    agentTicketService.getTickets.mockReturnValue(errorSubject.asObservable());

    component.loadTickets();
    errorSubject.error(new Error('Failed'));

    expect(component.isLoading()).toBe(false);
    expect(component.isError()).toBe(true);
    expect(component.tickets().length).toBe(3);
  });

  it('should show empty state when no tickets', () => {
    agentTicketService.getTickets.mockReturnValue(of([]));

    component.loadTickets();

    expect(component.tickets().length).toBe(0);
    expect(component.filteredTickets().length).toBe(0);
    expect(component.isLoading()).toBe(false);
  });

  it('should translate labels', () => {
    expect(component.allLabel()).toBe('translated');
    expect(translationService.translate).toHaveBeenCalledWith('agent.filter.all');
    expect(component.loadingLabel()).toBe('translated');
    expect(translationService.translate).toHaveBeenCalledWith('agent.loading');
    expect(component.emptyLabel()).toBe('translated');
    expect(translationService.translate).toHaveBeenCalledWith('agent.empty');
  });
});

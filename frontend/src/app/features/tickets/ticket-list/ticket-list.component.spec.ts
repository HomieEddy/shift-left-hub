import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { TicketService } from '../ticket.service';
import { TicketListComponent } from './ticket-list.component';

describe('TicketListComponent', () => {
  let component: TicketListComponent;
  let fixture: ComponentFixture<TicketListComponent>;
  let ticketService: { getTickets: ReturnType<typeof vi.fn> };

  const mockTickets = [
    { id: '1', ticketNumber: 'TKT-0001', status: 'NEW', category: 'NETWORK', urgency: 'HIGH', issue: 'Cannot connect to VPN', userId: 'u1', userDisplayName: 'Test User', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', resolvedAt: null, cancelledAt: null, cancelReason: null, shiftLeftContext: undefined },
    { id: '2', ticketNumber: 'TKT-0002', status: 'IN_PROGRESS', category: 'SOFTWARE', urgency: 'MEDIUM', issue: 'Excel crash on save', userId: 'u2', userDisplayName: 'User 2', createdAt: '2026-01-02T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z', resolvedAt: null, cancelledAt: null, cancelReason: null, shiftLeftContext: undefined },
    { id: '3', ticketNumber: 'TKT-0003', status: 'RESOLVED', category: 'HARDWARE', urgency: 'LOW', issue: 'Monitor flickering', userId: 'u1', userDisplayName: 'Test User', createdAt: '2026-01-03T00:00:00Z', updatedAt: '2026-01-03T00:00:00Z', resolvedAt: '2026-01-04T00:00:00Z', cancelledAt: null, cancelReason: null, shiftLeftContext: undefined },
  ];

  beforeEach(async () => {
    ticketService = {
      getTickets: vi.fn().mockReturnValue(of(mockTickets)),
    };

    await TestBed.configureTestingModule({
      imports: [TicketListComponent],
      providers: [
        { provide: TicketService, useValue: ticketService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TicketListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load tickets on init', () => {
    expect(ticketService.getTickets).toHaveBeenCalled();
    expect(component.tickets().length).toBe(3);
    expect(component.isLoading()).toBe(false);
  });

  it('should filter tickets by status', () => {
    component.applyFilter('NEW');
    expect(component.filteredTickets().length).toBe(1);
    expect(component.filteredTickets()[0].status).toBe('NEW');
    expect(component.activeFilter()).toBe('NEW');

    component.applyFilter('RESOLVED');
    expect(component.filteredTickets().length).toBe(1);
    expect(component.filteredTickets()[0].status).toBe('RESOLVED');
  });

  it('should show all tickets when filter is ALL', () => {
    component.applyFilter('ALL');
    expect(component.filteredTickets().length).toBe(3);
    expect(component.activeFilter()).toBe('ALL');
  });

  it('should set loading state during ticket load', () => {
    const pendingSubject = new Subject<any>();
    ticketService.getTickets.mockReturnValue(pendingSubject.asObservable());

    component.loadTickets();

    expect(component.isLoading()).toBe(true);
  });

  it('should handle error state gracefully', () => {
    const errorSubject = new Subject<any>();
    ticketService.getTickets.mockReturnValue(errorSubject.asObservable());

    component.loadTickets();
    errorSubject.error(new Error('Failed to load'));

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).not.toBeNull();
    // Tickets list preserved on error (user can retry)
    expect(component.tickets().length).toBe(3);
  });

  it('should show empty list when no tickets returned', () => {
    ticketService.getTickets.mockReturnValue(of([]));

    component.loadTickets();

    expect(component.tickets().length).toBe(0);
    expect(component.filteredTickets().length).toBe(0);
    expect(component.isLoading()).toBe(false);
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TicketService } from './ticket.service';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;

  const mockTicket = {
    id: 'ticket-1',
    ticketNumber: 'TKT-0001',
    status: 'NEW' as const,
    category: 'SOFTWARE' as const,
    urgency: 'HIGH' as const,
    issue: 'My software is broken',
    shiftLeftContext: '{"issue":"software broken","category":"SOFTWARE","urgency":"HIGH"}',
    userId: 'user-1',
    userDisplayName: 'Test User',
    resolvedAt: null,
    cancelledAt: null,
    cancelReason: null,
    createdAt: '2024-06-01T10:00:00Z',
    updatedAt: '2024-06-01T10:00:00Z',
  };

  const mockTickets = [
    mockTicket,
    { ...mockTicket, id: 'ticket-2', ticketNumber: 'TKT-0002', status: 'IN_PROGRESS' as const },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TicketService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('createTicket', () => {
    it('should POST to /api/tickets with correct body and return typed response', () => {
      const request = {
        issue: 'My software is broken',
        category: 'SOFTWARE' as const,
        urgency: 'HIGH' as const,
      };

      service.createTicket(request).subscribe((ticket) => {
        expect(ticket.id).toBe('ticket-1');
        expect(ticket.ticketNumber).toBe('TKT-0001');
        expect(ticket.status).toBe('NEW');
      });

      const req = httpMock.expectOne('/api/tickets');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockTicket);
    });

    it('should include shiftLeftContext when provided', () => {
      const request = {
        issue: 'Software issue',
        category: 'SOFTWARE' as const,
        urgency: 'LOW' as const,
        shiftLeftContext: '{"issue":"test","transcript":[]}',
      };

      service.createTicket(request).subscribe();

      const req = httpMock.expectOne('/api/tickets');
      const body = req.request.body as { shiftLeftContext: string };
      expect(body.shiftLeftContext).toBe('{"issue":"test","transcript":[]}');
      req.flush(mockTicket);
    });
  });

  describe('getTickets', () => {
    it('should GET /api/tickets and return a list', () => {
      service.getTickets().subscribe((tickets) => {
        expect(tickets.length).toBe(2);
        expect(tickets[0].ticketNumber).toBe('TKT-0001');
        expect(tickets[1].ticketNumber).toBe('TKT-0002');
      });

      const req = httpMock.expectOne('/api/tickets');
      expect(req.request.method).toBe('GET');
      req.flush(mockTickets);
    });

    it('should handle empty list', () => {
      service.getTickets().subscribe((tickets) => {
        expect(tickets.length).toBe(0);
      });

      const req = httpMock.expectOne('/api/tickets');
      req.flush([]);
    });
  });

  describe('getTicket', () => {
    it('should GET /api/tickets/:id and return a single ticket', () => {
      service.getTicket('ticket-1').subscribe((ticket) => {
        expect(ticket.id).toBe('ticket-1');
        expect(ticket.issue).toBe('My software is broken');
      });

      const req = httpMock.expectOne('/api/tickets/ticket-1');
      expect(req.request.method).toBe('GET');
      req.flush(mockTicket);
    });
  });

  describe('cancelTicket', () => {
    it('should POST to /api/tickets/:id/cancel with cancelReason', () => {
      service.cancelTicket('ticket-1', 'Resolved on my own').subscribe((ticket) => {
        expect(ticket.status).toBe('CANCELLED');
      });

      const req = httpMock.expectOne('/api/tickets/ticket-1/cancel');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ cancelReason: 'Resolved on my own' });
      req.flush({ ...mockTicket, status: 'CANCELLED', cancelledAt: '2024-06-01T11:00:00Z' });
    });

    it('should POST to /api/tickets/:id/cancel without cancelReason', () => {
      service.cancelTicket('ticket-1').subscribe((ticket) => {
        expect(ticket.status).toBe('CANCELLED');
      });

      const req = httpMock.expectOne('/api/tickets/ticket-1/cancel');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ ...mockTicket, status: 'CANCELLED', cancelledAt: '2024-06-01T11:00:00Z' });
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP error on getTickets', () => {
      let errorResponse: unknown = null;

      service.getTickets().subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/tickets');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      expect((errorResponse as { status: number }).status).toBe(401);
    });

    it('should propagate 404 when ticket not found', () => {
      let errorResponse: unknown = null;

      service.getTicket('nonexistent').subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/tickets/nonexistent');
      req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

      expect((errorResponse as { status: number }).status).toBe(404);
    });
  });
});

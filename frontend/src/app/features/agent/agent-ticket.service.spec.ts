import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { AgentTicketService } from './agent-ticket.service';
import { AgentTicket, WorkNote } from './agent-ticket.model';

describe('AgentTicketService', () => {
  let service: AgentTicketService;
  let httpMock: HttpTestingController;

  const mockTicket: AgentTicket = {
    id: 'tkt-1',
    ticketNumber: 'TKT-0001',
    status: 'NEW',
    category: 'NETWORK',
    urgency: 'HIGH',
    issue: 'Cannot connect',
    userId: 'user-1',
    userDisplayName: 'Alice',
    userEmail: 'alice@example.com',
    assignedToId: null,
    assignedToDisplayName: null,
    resolvedById: null,
    resolvedByDisplayName: null,
    resolutionNotes: null,
    isKnowledgeGap: false,
    resolvedAt: null,
    cancelledAt: null,
    cancelReason: null,
    createdAt: '2024-06-01T10:00:00Z',
    updatedAt: '2024-06-01T10:00:00Z',
  };

  const mockWorkNote: WorkNote = {
    id: 'wn-1',
    authorDisplayName: 'Agent Bob',
    content: 'Investigating',
    createdAt: '2024-06-01T11:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AgentTicketService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AgentTicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getTickets', () => {
    it('should GET /api/agent/tickets with no params when no filters', () => {
      service.getTickets().subscribe((tickets) => {
        expect(tickets.length).toBe(1);
      });
      const req = httpMock.expectOne(
        (r) => r.url === '/api/agent/tickets' && r.params.keys().length === 0);
      req.flush([mockTicket]);
    });

    it('should pass status, category, urgency, and search as query params', () => {
      service.getTickets({
        status: 'NEW',
        category: 'NETWORK',
        urgency: 'HIGH',
        search: 'vpn',
      }).subscribe();
      const req = httpMock.expectOne(
        (r) => r.url === '/api/agent/tickets'
          && r.params.get('status') === 'NEW'
          && r.params.get('category') === 'NETWORK'
          && r.params.get('urgency') === 'HIGH'
          && r.params.get('search') === 'vpn');
      req.flush([mockTicket]);
    });
  });

  it('should GET /api/agent/tickets/:id on getTicket', () => {
    service.getTicket('tkt-1').subscribe((t) => {
      expect(t.id).toBe('tkt-1');
    });
    const req = httpMock.expectOne('/api/agent/tickets/tkt-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockTicket);
  });

  it('should POST on claimTicket', () => {
    service.claimTicket('tkt-1').subscribe((t) => {
      expect(t.status).toBe('IN_PROGRESS');
    });
    const req = httpMock.expectOne('/api/agent/tickets/tkt-1/claim');
    expect(req.request.method).toBe('POST');
    req.flush({ ...mockTicket, status: 'IN_PROGRESS' });
  });

  it('should GET work notes on getWorkNotes', () => {
    service.getWorkNotes('tkt-1').subscribe((notes) => {
      expect(notes.length).toBe(1);
    });
    const req = httpMock.expectOne('/api/agent/tickets/tkt-1/work-notes');
    expect(req.request.method).toBe('GET');
    req.flush([mockWorkNote]);
  });

  it('should POST work note on addWorkNote with content in body', () => {
    service.addWorkNote('tkt-1', 'Following up').subscribe((note) => {
      expect(note.content).toBe('Following up');
    });
    const req = httpMock.expectOne('/api/agent/tickets/tkt-1/work-notes');
    expect(req.request.method).toBe('POST');
    const body = req.request.body as { content?: string };
    expect(body.content).toBe('Following up');
    req.flush({ ...mockWorkNote, content: 'Following up' });
  });

  it('should POST on resolveTicket with resolution notes in body', () => {
    service.resolveTicket('tkt-1', {
      resolutionNotes: 'Fixed by restarting service',
      isKnowledgeGap: false,
    }).subscribe();
    const req = httpMock.expectOne('/api/agent/tickets/tkt-1/resolve');
    expect(req.request.method).toBe('POST');
    const body = req.request.body as { resolutionNotes?: string };
    expect(body.resolutionNotes).toBe('Fixed by restarting service');
    req.flush({ ...mockTicket, status: 'RESOLVED' });
  });
});

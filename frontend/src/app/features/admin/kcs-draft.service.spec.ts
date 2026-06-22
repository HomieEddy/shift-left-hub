import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { KcsDraftService } from './kcs-draft.service';

describe('KcsDraftService', () => {
  let service: KcsDraftService;
  let httpMock: HttpTestingController;

  const mockDraft = {
    id: 'draft-1',
    titleEn: 'How to fix network connection',
    titleFr: null,
    slug: 'how-to-fix-network-connection',
    excerpt: 'A guide to fix network issues',
    status: 'DRAFT' as const,
    sourceTicketId: 'ticket-1',
    sourceTicketNumber: 'TKT-0001',
    similarityWarnings: [],
    tags: [],
    createdAt: '2024-06-01T10:00:00Z',
  };

  const mockPaginatedResponse = {
    content: [mockDraft, { ...mockDraft, id: 'draft-2', titleEn: 'Printer setup guide' }],
    totalPages: 1,
    totalElements: 2,
    number: 0,
    size: 20,
  };

  const mockPendingCount = { pendingCount: 5 };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        KcsDraftService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(KcsDraftService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDrafts', () => {
    it('should GET /api/admin/kcs/drafts with pagination', () => {
      service.getDrafts().subscribe((response) => {
        expect(response.content.length).toBe(2);
        expect(response.totalPages).toBe(1);
      });

      const req = httpMock.expectOne((r) => r.url === '/api/admin/kcs/drafts');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('20');
      req.flush(mockPaginatedResponse);
    });

    it('should GET with custom page and size', () => {
      service.getDrafts(1, 50).subscribe();

      const req = httpMock.expectOne((r) => r.url === '/api/admin/kcs/drafts');
      expect(req.request.params.get('page')).toBe('1');
      expect(req.request.params.get('size')).toBe('50');
      req.flush(mockPaginatedResponse);
    });
  });

  describe('getDraftDetail', () => {
    it('should GET /api/admin/kcs/drafts/:id', () => {
      service.getDraftDetail('draft-1').subscribe((draft) => {
        expect(draft.id).toBe('draft-1');
        expect(draft.titleEn).toBe('How to fix network connection');
      });

      const req = httpMock.expectOne('/api/admin/kcs/drafts/draft-1');
      expect(req.request.method).toBe('GET');
      req.flush(mockDraft);
    });
  });

  describe('approveDraft', () => {
    it('should PUT to /api/admin/kcs/drafts/:id/approve', () => {
      service.approveDraft('draft-1').subscribe((draft) => {
        expect(draft.status).toBe('PUBLISHED');
      });

      const req = httpMock.expectOne('/api/admin/kcs/drafts/draft-1/approve');
      expect(req.request.method).toBe('PUT');
      // Verify empty body
      expect(req.request.body).toEqual({});
      req.flush({ ...mockDraft, status: 'PUBLISHED' });
    });
  });

  describe('rejectDraft', () => {
    it('should PUT to /api/admin/kcs/drafts/:id/reject', () => {
      service.rejectDraft('draft-1').subscribe((draft) => {
        expect(draft.status).toBe('ARCHIVED');
      });

      const req = httpMock.expectOne('/api/admin/kcs/drafts/draft-1/reject');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush({ ...mockDraft, status: 'ARCHIVED' });
    });
  });

  describe('getPendingCount', () => {
    it('should GET /api/admin/kcs/drafts/pending-count', () => {
      service.getPendingCount().subscribe((response) => {
        expect(response.pendingCount).toBe(5);
      });

      const req = httpMock.expectOne('/api/admin/kcs/drafts/pending-count');
      expect(req.request.method).toBe('GET');
      req.flush(mockPendingCount);
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP error on getDrafts', () => {
      let errorResponse: unknown = null;

      service.getDrafts().subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne((r) => r.url === '/api/admin/kcs/drafts');
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

      expect((errorResponse as { status: number }).status).toBe(403);
    });

    it('should propagate HTTP error on approveDraft', () => {
      let errorResponse: unknown = null;

      service.approveDraft('invalid-id').subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/kcs/drafts/invalid-id/approve');
      req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });

      expect((errorResponse as { status: number }).status).toBe(404);
    });
  });
});

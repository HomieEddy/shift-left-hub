import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { DocumentService } from './document.service';

describe('DocumentService', () => {
  let service: DocumentService;
  let httpMock: HttpTestingController;

  const mockDocument = {
    id: 'doc-1',
    filename: 'guide.md',
    mimeType: 'text/markdown',
    status: 'READY' as const,
    errorMessage: null,
    fileSize: 2048,
    chunkCount: 5,
    createdAt: '2024-06-10T10:00:00Z',
    updatedAt: '2024-06-10T10:05:00Z',
  };

  const mockUploadResponse = {
    id: 'doc-2',
    filename: 'new-doc.md',
    mimeType: 'text/markdown',
    status: 'UPLOADED' as const,
    fileSize: 1024,
    createdAt: '2024-06-10T12:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DocumentService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(DocumentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDocuments', () => {
    it('should GET /api/admin/documents', () => {
      service.getDocuments().subscribe((docs) => {
        expect(docs.length).toBe(1);
        expect(docs[0].filename).toBe('guide.md');
        expect(docs[0].status).toBe('READY');
      });

      const req = httpMock.expectOne('/api/admin/documents');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush([mockDocument]);
    });
  });

  describe('getDocument', () => {
    it('should GET /api/admin/documents/:id', () => {
      service.getDocument('doc-1').subscribe((doc) => {
        expect(doc.id).toBe('doc-1');
        expect(doc.filename).toBe('guide.md');
      });

      const req = httpMock.expectOne('/api/admin/documents/doc-1');
      expect(req.request.method).toBe('GET');
      req.flush(mockDocument);
    });
  });

  describe('uploadFile', () => {
    it('should POST multipart form to /api/admin/documents/upload', () => {
      const file = new File(['test content'], 'test.md', { type: 'text/markdown' });

      service.uploadFile(file).subscribe((response) => {
        expect(response.id).toBe('doc-2');
        expect(response.status).toBe('UPLOADED');
      });

      const req = httpMock.expectOne('/api/admin/documents/upload');
      expect(req.request.method).toBe('POST');
      const body = req.request.body as FormData;
      expect(body instanceof FormData).toBe(true);
      expect(body.has('file')).toBe(true);
      expect(req.request.withCredentials).toBe(true);
      req.flush(mockUploadResponse);
    });
  });

  describe('deleteDocument', () => {
    it('should DELETE /api/admin/documents/:id', () => {
      service.deleteDocument('doc-1').subscribe();

      const req = httpMock.expectOne('/api/admin/documents/doc-1');
      expect(req.request.method).toBe('DELETE');
      expect(req.request.withCredentials).toBe(true);
      req.flush(null);
    });
  });

  describe('reprocessDocument', () => {
    it('should POST to /api/admin/documents/:id/reprocess', () => {
      service.reprocessDocument('doc-1').subscribe((response) => {
        expect(response.id).toBe('doc-2');
      });

      const req = httpMock.expectOne('/api/admin/documents/doc-1/reprocess');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      req.flush(mockUploadResponse);
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP error on getDocuments', () => {
      let errorResponse: unknown = null;

      service.getDocuments().subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/documents');
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

      expect((errorResponse as { status: number }).status).toBe(403);
    });

    it('should propagate HTTP error on uploadFile', () => {
      const file = new File(['test'], 'test.md', { type: 'text/markdown' });
      let errorResponse: unknown = null;

      service.uploadFile(file).subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/documents/upload');
      req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

      expect((errorResponse as { status: number }).status).toBe(400);
    });
  });
});

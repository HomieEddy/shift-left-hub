import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { WorkspaceLlmConfigService } from './workspace-llm-config.service';

describe('WorkspaceLlmConfigService', () => {
  let service: WorkspaceLlmConfigService;
  let httpMock: HttpTestingController;

  const mockConfigResponse = {
    id: 'cfg-1',
    workspaceId: 'ws-1',
    llmProvider: 'OLLAMA',
    endpointUrl: 'http://localhost:11434',
    modelName: 'llama3.2',
    embeddingModelName: 'nomic-embed-text',
    similarityThreshold: 0.65,
    embeddingDimension: 768,
    createdAt: '2024-06-10T10:00:00Z',
    updatedAt: '2024-06-10T10:00:00Z',
  };

  const mockTestResult = {
    success: true,
    message: 'Connection successful. Model llama3.2 responded: hello',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WorkspaceLlmConfigService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(WorkspaceLlmConfigService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getConfig', () => {
    it('should GET /api/admin/workspaces/:id/llm-config', () => {
      service.getConfig('ws-1').subscribe((config) => {
        expect(config.workspaceId).toBe('ws-1');
        expect(config.llmProvider).toBe('OLLAMA');
      });

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/llm-config');
      expect(req.request.method).toBe('GET');
      req.flush(mockConfigResponse);
    });
  });

  describe('saveConfig', () => {
    it('should PUT to /api/admin/workspaces/:id/llm-config', () => {
      const request = {
        llmProvider: 'OPENAI_COMPATIBLE',
        endpointUrl: 'https://api.openai.com/v1',
        apiKey: 'sk-test',
        modelName: 'gpt-4',
      };

      service.saveConfig('ws-1', request).subscribe((config) => {
        expect(config.id).toBe('cfg-1');
      });

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/llm-config');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(request);
      req.flush(mockConfigResponse);
    });
  });

  describe('testConnection', () => {
    it('should POST to /api/admin/workspaces/:id/llm-config/test', () => {
      const request = {
        llmProvider: 'OLLAMA',
        endpointUrl: 'http://localhost:11434',
        modelName: 'llama3.2',
      };

      service.testConnection('ws-1', request).subscribe((result) => {
        expect(result.success).toBe(true);
        expect(result.message).toContain('hello');
      });

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/llm-config/test');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockTestResult);
    });
  });

  describe('deleteConfig', () => {
    it('should DELETE /api/admin/workspaces/:id/llm-config', () => {
      service.deleteConfig('ws-1').subscribe();

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/llm-config');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP error on getConfig', () => {
      let errorResponse: unknown = null;

      service.getConfig('ws-1').subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/llm-config');
      req.flush({ message: 'Not Found' }, { status: 404, statusText: 'Not Found' });

      expect((errorResponse as { status: number }).status).toBe(404);
    });
  });
});

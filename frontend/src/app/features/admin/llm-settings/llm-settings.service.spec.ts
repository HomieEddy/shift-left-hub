import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { LlmSettingsService, AiConfigResponse } from './llm-settings.service';

describe('LlmSettingsService', () => {
  let service: LlmSettingsService;
  let httpMock: HttpTestingController;

  const mockConfig: AiConfigResponse = {
    llmProvider: 'OLLAMA',
    ollamaEndpointUrl: 'http://localhost:11434',
    hasOpenaiKey: false,
    chatModelName: 'llama3.2',
    embeddingModelName: 'nomic-embed-text',
    similarityThreshold: 0.65,
    embeddingDimension: 768,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        LlmSettingsService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(LlmSettingsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should GET /api/ai/config', () => {
    service.getConfig().subscribe((response) => {
      expect(response.llmProvider).toBe('OLLAMA');
    });
    const req = httpMock.expectOne('/api/ai/config');
    expect(req.request.method).toBe('GET');
    req.flush(mockConfig);
  });

  it('should PUT /api/ai/config on updateConfig', () => {
    service.updateConfig({ chatModelName: 'mistral' }).subscribe();
    const req = httpMock.expectOne('/api/ai/config');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.chatModelName).toBe('mistral');
    req.flush(mockConfig);
  });

  it('should POST /api/ai/config/test on testConnection', () => {
    service.testConnection({ llmProvider: 'OLLAMA' }).subscribe((result) => {
      expect(result.success).toBe(true);
    });
    const req = httpMock.expectOne('/api/ai/config/test');
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'OK' });
  });

  it('should POST /api/ai/config/embeddings/reindex on reindexEmbeddings', () => {
    service.reindexEmbeddings().subscribe((result) => {
      expect(result.message).toBe('Re-embedding started');
    });
    const req = httpMock.expectOne('/api/ai/config/embeddings/reindex');
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Re-embedding started' });
  });
});

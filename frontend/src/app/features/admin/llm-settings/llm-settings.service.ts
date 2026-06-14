import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AiConfigResponse {
  llmProvider: string;
  ollamaEndpointUrl: string;
  hasOpenaiKey: boolean;
  chatModelName: string;
  embeddingModelName: string;
  similarityThreshold: number;
  embeddingDimension: number;
}

export interface AiConfigRequest {
  llmProvider?: string;
  ollamaEndpointUrl?: string;
  openaiApiKey?: string;
  chatModelName?: string;
  embeddingModelName?: string;
  similarityThreshold?: number;
}

export interface TestConnectionResult {
  success: boolean;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class LlmSettingsService {
  private http = inject(HttpClient);

  getConfig(): Observable<AiConfigResponse> {
    return this.http.get<AiConfigResponse>('/api/ai/config', { withCredentials: true });
  }

  updateConfig(config: AiConfigRequest): Observable<AiConfigResponse> {
    return this.http.put<AiConfigResponse>('/api/ai/config', config, { withCredentials: true });
  }

  testConnection(config: AiConfigRequest): Observable<TestConnectionResult> {
    return this.http.post<TestConnectionResult>('/api/ai/config/test', config, {
      withCredentials: true,
    });
  }

  reindexEmbeddings(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      '/api/ai/config/embeddings/reindex',
      {},
      { withCredentials: true },
    );
  }
}

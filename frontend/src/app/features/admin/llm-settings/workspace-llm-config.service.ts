import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface WorkspaceLlmConfigResponse {
  id: string;
  workspaceId: string;
  llmProvider: string;
  endpointUrl: string;
  modelName: string;
  embeddingModelName: string;
  similarityThreshold: number;
  embeddingDimension: number;
  systemPrompt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceLlmConfigRequest {
  llmProvider?: string;
  endpointUrl?: string;
  apiKey?: string;
  modelName?: string;
  embeddingModelName?: string;
  similarityThreshold?: number;
  systemPrompt?: string | null;
}

export interface TestConnectionResult {
  success: boolean;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class WorkspaceLlmConfigService {
  private http = inject(HttpClient);

  getConfig(workspaceId: string): Observable<WorkspaceLlmConfigResponse> {
    return this.http.get<WorkspaceLlmConfigResponse>(
      `/api/admin/workspaces/${workspaceId}/llm-config`,
      {
        },
    );
  }

  saveConfig(
    workspaceId: string,
    config: WorkspaceLlmConfigRequest,
  ): Observable<WorkspaceLlmConfigResponse> {
    return this.http.put<WorkspaceLlmConfigResponse>(
      `/api/admin/workspaces/${workspaceId}/llm-config`,
      config,
      {
        },
    );
  }

  testConnection(
    workspaceId: string,
    config: WorkspaceLlmConfigRequest,
  ): Observable<TestConnectionResult> {
    return this.http.post<TestConnectionResult>(
      `/api/admin/workspaces/${workspaceId}/llm-config/test`,
      config,
      {
        },
    );
  }

  deleteConfig(workspaceId: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/workspaces/${workspaceId}/llm-config`, {
      });
  }
}

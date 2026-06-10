import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { WorkspaceDto, CreateWorkspaceRequest, WorkspaceMemberDto, AssignUserRequest } from './workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/workspaces';

  getWorkspaces(): Observable<WorkspaceDto[]> {
    return this.http.get<WorkspaceDto[]>(this.apiUrl);
  }

  getWorkspace(id: string): Observable<WorkspaceDto> {
    return this.http.get<WorkspaceDto>(`${this.apiUrl}/${id}`);
  }

  createWorkspace(request: CreateWorkspaceRequest): Observable<WorkspaceDto> {
    return this.http.post<WorkspaceDto>(this.apiUrl, request);
  }

  getMembers(workspaceId: string): Observable<WorkspaceMemberDto[]> {
    return this.http.get<WorkspaceMemberDto[]>(`${this.apiUrl}/${workspaceId}/members`);
  }

  assignUser(workspaceId: string, request: AssignUserRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${workspaceId}/members`, request);
  }

  getAvailableUsers(workspaceId: string): Observable<WorkspaceMemberDto[]> {
    return this.http.get<WorkspaceMemberDto[]>(`${this.apiUrl}/${workspaceId}/available-users`);
  }
}

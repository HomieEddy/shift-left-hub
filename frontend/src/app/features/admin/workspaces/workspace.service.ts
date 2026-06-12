import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  WorkspaceDto, CreateWorkspaceRequest, WorkspaceMemberDto, AssignUserRequest,
  CreateInvitationRequest, InvitationDto, ChangeRoleRequest, UpdateWorkspaceRequest,
  WorkspaceRoleResponse
} from './workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private http = inject(HttpClient);
  private apiUrl = '/api/admin/workspaces';
  private userApiUrl = '/api/workspaces';
  private invitationsApiUrl = '/api/invitations';

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

  updateWorkspace(id: string, request: UpdateWorkspaceRequest): Observable<WorkspaceDto> {
    return this.http.put<WorkspaceDto>(`${this.apiUrl}/${id}`, request, { withCredentials: true });
  }

  deleteWorkspace(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { withCredentials: true });
  }

  getInvitations(workspaceId: string): Observable<InvitationDto[]> {
    return this.http.get<InvitationDto[]>(`${this.apiUrl}/${workspaceId}/invitations`, { withCredentials: true });
  }

  sendInvitation(workspaceId: string, request: CreateInvitationRequest): Observable<InvitationDto> {
    return this.http.post<InvitationDto>(`${this.apiUrl}/${workspaceId}/invitations`, request, { withCredentials: true });
  }

  revokeInvitation(workspaceId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${workspaceId}/invitations/${invitationId}`, { withCredentials: true });
  }

  removeMember(workspaceId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${workspaceId}/members/${userId}`, { withCredentials: true });
  }

  changeMemberRole(workspaceId: string, userId: string, request: ChangeRoleRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${workspaceId}/members/${userId}/role`, request, { withCredentials: true });
  }

  getMyWorkspaces(): Observable<WorkspaceDto[]> {
    return this.http.get<WorkspaceDto[]>(`${this.userApiUrl}/mine`, { withCredentials: true });
  }

  getMyRole(): Observable<WorkspaceRoleResponse> {
    return this.http.get<WorkspaceRoleResponse>(`${this.userApiUrl}/current/role`, { withCredentials: true });
  }

  leaveWorkspace(id: string): Observable<void> {
    return this.http.post<void>(`${this.userApiUrl}/${id}/leave`, {}, { withCredentials: true });
  }

  getMyInvitations(): Observable<InvitationDto[]> {
    return this.http.get<InvitationDto[]>(`${this.invitationsApiUrl}`, { withCredentials: true });
  }

  acceptInvitation(id: string): Observable<void> {
    return this.http.post<void>(`${this.invitationsApiUrl}/${id}/accept`, {}, { withCredentials: true });
  }

  rejectInvitation(id: string): Observable<void> {
    return this.http.post<void>(`${this.invitationsApiUrl}/${id}/reject`, {}, { withCredentials: true });
  }
}

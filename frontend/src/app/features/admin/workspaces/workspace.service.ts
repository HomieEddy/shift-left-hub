import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  WorkspaceDto,
  CreateWorkspaceRequest,
  WorkspaceMemberDto,
  AssignUserRequest,
  CreateInvitationRequest,
  InvitationDto,
  ChangeRoleRequest,
  UpdateWorkspaceRequest,
} from './workspace.model';
import { SUPPRESS_ERROR_TOAST } from '../../../core/http/http-context-tokens';

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
    return this.http.put<WorkspaceDto>(`${this.apiUrl}/${id}`, request, {});
  }

  deleteWorkspace(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {});
  }

  getInvitations(workspaceId: string): Observable<InvitationDto[]> {
    return this.http.get<InvitationDto[]>(`${this.apiUrl}/${workspaceId}/invitations`, {
      });
  }

  sendInvitation(workspaceId: string, request: CreateInvitationRequest): Observable<InvitationDto> {
    return this.http.post<InvitationDto>(`${this.apiUrl}/${workspaceId}/invitations`, request, {
      });
  }

  revokeInvitation(workspaceId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${workspaceId}/invitations/${invitationId}`, {
      });
  }

  removeMember(workspaceId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${workspaceId}/members/${userId}`, {
      });
  }

  changeMemberRole(
    workspaceId: string,
    userId: string,
    request: ChangeRoleRequest,
  ): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${workspaceId}/members/${userId}/role`, request, {
      });
  }

  getMyWorkspaces(): Observable<WorkspaceDto[]> {
    const context = new HttpContext().set(SUPPRESS_ERROR_TOAST, true);
    return this.http.get<WorkspaceDto[]>(`${this.userApiUrl}/mine`, {
      context,
      });
  }

  leaveWorkspace(id: string): Observable<void> {
    return this.http.post<void>(`${this.userApiUrl}/${id}/leave`, {}, {});
  }

  getMyInvitations(): Observable<InvitationDto[]> {
    return this.http.get<InvitationDto[]>(`${this.invitationsApiUrl}`, {});
  }

  acceptInvitation(id: string): Observable<void> {
    return this.http.post<void>(
      `${this.invitationsApiUrl}/${id}/accept`,
      {},
      {},
    );
  }

  rejectInvitation(id: string): Observable<void> {
    return this.http.post<void>(
      `${this.invitationsApiUrl}/${id}/reject`,
      {},
      {},
    );
  }
}

import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { WorkspaceRoleResponse } from '../../features/admin/workspaces/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceRoleService {
  private http = inject(HttpClient);

  private roleSignal = signal<string>('NONE');
  readonly role = this.roleSignal.asReadonly();

  readonly isAdmin = computed(() => this.role() === 'ADMIN');
  readonly isMember = computed(() => this.role() === 'MEMBER' || this.role() === 'ADMIN');
  readonly isReadOnly = computed(() => this.role() === 'READ_ONLY');
  readonly isAuthenticated = computed(() => this.role() !== 'NONE');

  fetchRole(): Observable<WorkspaceRoleResponse> {
    return this.http
      .get<WorkspaceRoleResponse>('/api/workspaces/current/role', { withCredentials: true })
      .pipe(tap((response) => this.roleSignal.set(response.role)));
  }

  refreshRole(): void {
    this.fetchRole().subscribe({
      error: () => {
        console.error('Failed to refresh workspace role');
        this.roleSignal.set('NONE');
      },
    });
  }

  clearRole(): void {
    this.roleSignal.set('NONE');
  }
}

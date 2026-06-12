import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserDto } from './auth.models';

/** Manages authentication state, session tokens, and admin/agent utilities. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<AuthResponse | null>(null);
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = signal(false);
  readonly isAdmin = signal(false);
  readonly isAgent = signal(false);

  private readonly http = inject(HttpClient);

  constructor() {
    this.tryRefreshToken();
  }

  /** Register a new user. Session is set automatically on success. */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', request, { withCredentials: true })
      .pipe(tap(response => this.setSession(response)));
  }

  /** Authenticate with email/password. Session is set on success. */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request, { withCredentials: true })
      .pipe(tap(response => this.setSession(response)));
  }

  /** Refresh the JWT using the HttpOnly cookie. Called on app init. */
  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/refresh', {}, { withCredentials: true })
      .pipe(tap(response => this.setSession(response)));
  }

  /** Switch active workspace by re-issuing JWT with new workspace_id claim. */
  switchWorkspace(workspaceId: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`/api/auth/workspace/${workspaceId}`, {}, { withCredentials: true })
      .pipe(tap(response => this.setSession(response)));
  }

  /** Log out and clear the session on the server. */
  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true })
      .pipe(tap(() => this.clearSession()));
  }

  /** Fetch all users (admin only). */
  getUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>('/api/admin/users', { withCredentials: true });
  }

  /** Get a single user by ID (admin only). */
  getUserById(id: string): Observable<UserDto> {
    return this.http.get<UserDto>(`/api/admin/users/${id}`, { withCredentials: true });
  }

  /** Update a user's role (admin only). */
  updateUserRole(id: string, role: string): Observable<UserDto> {
    return this.http.put<UserDto>(`/api/admin/users/${id}/role`, { role }, { withCredentials: true });
  }

  /** Toggle a user's enabled/disabled status (admin only). */
  toggleUserStatus(id: string): Observable<UserDto> {
    return this.http.put<UserDto>(`/api/admin/users/${id}/status`, {}, { withCredentials: true });
  }

  private setSession(response: AuthResponse): void {
    this.userSignal.set(response);
    this.isAuthenticated.set(true);
    this.isAdmin.set(response.role === 'ROLE_ADMIN');
    this.isAgent.set(response.role === 'ROLE_AGENT' || response.role === 'ROLE_ADMIN');
  }

  private clearSession(): void {
    this.userSignal.set(null);
    this.isAuthenticated.set(false);
    this.isAdmin.set(false);
    this.isAgent.set(false);
  }

  private tryRefreshToken(): void {
    this.refresh().subscribe({
      error: () => this.clearSession(),
    });
  }
}

import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, UserDto } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSignal = signal<AuthResponse | null>(null);
  readonly user = this.userSignal.asReadonly();
  readonly isAuthenticated = signal(false);
  readonly isAdmin = signal(false);
  readonly isAgent = signal(false);

  constructor(private http: HttpClient) {
    this.tryRefreshToken();
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/register', request, { withCredentials: true })
      .pipe(tap(response => this.setSession(response)));
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', request, { withCredentials: true })
      .pipe(tap(response => this.setSession(response)));
  }

  refresh(): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/refresh', {}, { withCredentials: true })
      .pipe(tap(response => this.setSession(response)));
  }

  logout(): Observable<any> {
    return this.http.post('/api/auth/logout', {}, { withCredentials: true })
      .pipe(tap(() => this.clearSession()));
  }

  getUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>('/api/admin/users', { withCredentials: true });
  }

  getUserById(id: string): Observable<UserDto> {
    return this.http.get<UserDto>(`/api/admin/users/${id}`, { withCredentials: true });
  }

  updateUserRole(id: string, role: string): Observable<UserDto> {
    return this.http.put<UserDto>(`/api/admin/users/${id}/role`, { role }, { withCredentials: true });
  }

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

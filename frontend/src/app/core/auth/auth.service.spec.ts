import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const mockAuthResponse = {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    userId: 'user-1',
    email: 'test@example.com',
    role: 'ROLE_USER',
    displayName: 'Test User',
  };

  const mockAdminResponse = {
    accessToken: 'admin-token',
    refreshToken: 'admin-refresh',
    userId: 'admin-1',
    email: 'admin@example.com',
    role: 'ROLE_ADMIN',
    displayName: 'Admin User',
  };

  const mockAgentResponse = {
    accessToken: 'agent-token',
    refreshToken: 'agent-refresh',
    userId: 'agent-1',
    email: 'agent@example.com',
    role: 'ROLE_AGENT',
    displayName: 'Agent User',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    // Flush the constructor's auto-refresh call to clear pending requests.
    // AuthService.tryRefreshToken() runs refresh() in the constructor as a side effect.
    // If auto-refresh is removed from the constructor or made lazy, remove this as well
    // and update the 'signal state after constructor auto-refresh' describe block below.
    httpMock.expectOne('/api/auth/refresh').flush(mockAuthResponse);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('login', () => {
    it('should POST to /api/auth/login and update signals', () => {
      service.login({ email: 'test@example.com', password: 'password' }).subscribe((response) => {
        expect(response.email).toBe('test@example.com');
        expect(service.isAuthenticated()).toBe(true);
        expect(service.user()).toEqual(mockAuthResponse);
      });

      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);

      // The request body should contain email and password
      const body = req.request.body as { email: string; password: string };
      expect(body.email).toBe('test@example.com');
      expect(body.password).toBe('password');

      req.flush(mockAuthResponse);
    });
  });

  describe('register', () => {
    it('should POST to /api/auth/register and update signals', () => {
      service
        .register({
          email: 'new@example.com',
          password: 'Pass123!',
          displayName: 'New User',
        })
        .subscribe((response) => {
          expect(response.email).toBe('test@example.com');
          expect(service.isAuthenticated()).toBe(true);
          expect(service.user()).toEqual(mockAuthResponse);
        });

      const req = httpMock.expectOne('/api/auth/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);

      const body = req.request.body as { email: string; password: string; displayName: string };
      expect(body.email).toBe('new@example.com');
      expect(body.displayName).toBe('New User');

      req.flush(mockAuthResponse);
    });
  });

  describe('refresh', () => {
    it('should POST to /api/auth/refresh and update signals', () => {
      service.refresh().subscribe((response) => {
        expect(response.email).toBe('admin@example.com');
        expect(service.isAuthenticated()).toBe(true);
        expect(service.isAdmin()).toBe(true);
      });

      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      req.flush(mockAdminResponse);
    });
  });

  describe('logout', () => {
    it('should POST to /api/auth/logout and clear all signals', () => {
      // First ensure user is authenticated
      service.logout().subscribe(() => {
        expect(service.isAuthenticated()).toBe(false);
        expect(service.isAdmin()).toBe(false);
        expect(service.isAgent()).toBe(false);
        expect(service.user()).toBeNull();
      });

      const req = httpMock.expectOne('/api/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      req.flush(null);
    });
  });

  describe('role detection', () => {
    it('should set isAdmin to true for ROLE_ADMIN', () => {
      service.login({ email: 'admin@example.com', password: 'admin' }).subscribe(() => {
        expect(service.isAdmin()).toBe(true);
        expect(service.isAgent()).toBe(true);
        expect(service.isAuthenticated()).toBe(true);
      });
      httpMock.expectOne('/api/auth/login').flush(mockAdminResponse);
    });

    it('should set isAgent to true for ROLE_AGENT (but not isAdmin)', () => {
      service.login({ email: 'agent@example.com', password: 'agent' }).subscribe(() => {
        expect(service.isAgent()).toBe(true);
        expect(service.isAdmin()).toBe(false);
        expect(service.isAuthenticated()).toBe(true);
      });
      httpMock.expectOne('/api/auth/login').flush(mockAgentResponse);
    });

    it('should set isAgent to false for ROLE_USER', () => {
      expect(service.isAgent()).toBe(false);
      expect(service.isAdmin()).toBe(false);
    });
  });

  describe('admin user management', () => {
    it('should GET /api/admin/users', () => {
      const mockUsers = [
        {
          id: '1',
          email: 'user1@example.com',
          displayName: 'User 1',
          role: 'ROLE_USER',
          enabled: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        },
        {
          id: '2',
          email: 'user2@example.com',
          displayName: 'User 2',
          role: 'ROLE_ADMIN',
          enabled: true,
          createdAt: '2024-01-01T00:00:00Z',
          updatedAt: '2024-01-01T00:00:00Z',
        },
      ];

      service.getUsers().subscribe((users) => {
        expect(users.length).toBe(2);
        expect(users[0].email).toBe('user1@example.com');
      });

      const req = httpMock.expectOne('/api/admin/users');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(mockUsers);
    });

    it('should GET /api/admin/users/:id', () => {
      const mockUser = {
        id: 'user-1',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'ROLE_USER',
        enabled: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      };

      service.getUserById('user-1').subscribe((user) => {
        expect(user.id).toBe('user-1');
        expect(user.email).toBe('test@example.com');
      });

      const req = httpMock.expectOne('/api/admin/users/user-1');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(mockUser);
    });

    it('should PUT /api/admin/users/:id/role with role body', () => {
      const updatedUser = {
        id: 'user-1',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'ROLE_ADMIN',
        enabled: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      };

      service.updateUserRole('user-1', 'ROLE_ADMIN').subscribe((user) => {
        expect(user.role).toBe('ROLE_ADMIN');
      });

      const req = httpMock.expectOne('/api/admin/users/user-1/role');
      expect(req.request.method).toBe('PUT');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.body).toEqual({ role: 'ROLE_ADMIN' });
      req.flush(updatedUser);
    });

    it('should PUT /api/admin/users/:id/status', () => {
      const updatedUser = {
        id: 'user-1',
        email: 'test@example.com',
        displayName: 'Test User',
        role: 'ROLE_USER',
        enabled: false,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z',
      };

      service.toggleUserStatus('user-1').subscribe((user) => {
        expect(user.enabled).toBe(false);
      });

      const req = httpMock.expectOne('/api/admin/users/user-1/status');
      expect(req.request.method).toBe('PUT');
      expect(req.request.withCredentials).toBe(true);
      req.flush(updatedUser);
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP error on login', () => {
      let errorResponse: unknown = null;

      service.login({ email: 'bad@example.com', password: 'wrong' }).subscribe({
        next: () => {
          throw new Error('should have failed with 401');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/auth/login');
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

      expect((errorResponse as { status: number }).status).toBe(401);
    });

    it('should propagate HTTP error on register', () => {
      let errorResponse: unknown = null;

      service
        .register({ email: 'bad@example.com', password: '123', displayName: 'Bad' })
        .subscribe({
          next: () => {
            throw new Error('should have failed with 400');
          },
          error: (error) => {
            errorResponse = error;
          },
        });

      const req = httpMock.expectOne('/api/auth/register');
      req.flush({ message: 'Validation error' }, { status: 400, statusText: 'Bad Request' });

      expect((errorResponse as { status: number }).status).toBe(400);
    });

    it('should handle logout network error gracefully', () => {
      let errorResponse: unknown = null;

      service.logout().subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/auth/logout');
      req.error(new ProgressEvent('network error'));

      expect((errorResponse as { status: number }).status).toBe(0);
    });
  });

  describe('signal state after constructor auto-refresh', () => {
    it('should be authenticated after successful constructor refresh', () => {
      expect(service.isAuthenticated()).toBe(true);
      expect(service.user()?.email).toBe('test@example.com');
      expect(service.isAdmin()).toBe(false);
    });
  });
});

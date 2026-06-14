import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { WorkspaceService } from './workspace.service';

describe('WorkspaceService', () => {
  let service: WorkspaceService;
  let httpMock: HttpTestingController;

  const mockWorkspace = {
    id: 'ws-1',
    name: 'Default Workspace',
    slug: 'default',
    description: 'Default workspace',
    logoUrl: null,
    memberCount: 3,
    createdBy: 'user-1',
    createdAt: '2024-06-01T10:00:00Z',
    updatedAt: '2024-06-01T10:00:00Z',
  };

  const mockMember = {
    userId: 'user-1',
    email: 'user@example.com',
    displayName: 'User One',
    role: 'ADMIN',
    joinedAt: '2024-06-01T10:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WorkspaceService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(WorkspaceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getWorkspaces', () => {
    it('should GET /api/admin/workspaces', () => {
      service.getWorkspaces().subscribe((workspaces) => {
        expect(workspaces.length).toBe(1);
        expect(workspaces[0].name).toBe('Default Workspace');
      });

      const req = httpMock.expectOne('/api/admin/workspaces');
      expect(req.request.method).toBe('GET');
      req.flush([mockWorkspace]);
    });
  });

  describe('getWorkspace', () => {
    it('should GET /api/admin/workspaces/:id', () => {
      service.getWorkspace('ws-1').subscribe((workspace) => {
        expect(workspace.id).toBe('ws-1');
        expect(workspace.name).toBe('Default Workspace');
      });

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1');
      expect(req.request.method).toBe('GET');
      req.flush(mockWorkspace);
    });
  });

  describe('createWorkspace', () => {
    it('should POST to /api/admin/workspaces', () => {
      const request = { name: 'New WS', description: 'A new workspace' };

      service.createWorkspace(request).subscribe((workspace) => {
        expect(workspace.name).toBe('Default Workspace');
      });

      const req = httpMock.expectOne('/api/admin/workspaces');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockWorkspace);
    });

    it('should create workspace without optional fields', () => {
      const request = { name: 'Minimal WS' };

      service.createWorkspace(request).subscribe((workspace) => {
        expect(workspace.slug).toBe('default');
      });

      const req = httpMock.expectOne('/api/admin/workspaces');
      expect(req.request.body).toEqual(request);
      req.flush(mockWorkspace);
    });
  });

  describe('getMembers', () => {
    it('should GET /api/admin/workspaces/:id/members', () => {
      service.getMembers('ws-1').subscribe((members) => {
        expect(members.length).toBe(1);
        expect(members[0].email).toBe('user@example.com');
      });

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/members');
      expect(req.request.method).toBe('GET');
      req.flush([mockMember]);
    });
  });

  describe('assignUser', () => {
    it('should POST to /api/admin/workspaces/:id/members', () => {
      const request = { userId: 'user-2', role: 'MEMBER' as const };

      service.assignUser('ws-1', request).subscribe();

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/members');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(null);
    });
  });

  describe('getAvailableUsers', () => {
    it('should GET /api/admin/workspaces/:id/available-users', () => {
      service.getAvailableUsers('ws-1').subscribe((users) => {
        expect(users.length).toBe(1);
        expect(users[0].displayName).toBe('User One');
      });

      const req = httpMock.expectOne('/api/admin/workspaces/ws-1/available-users');
      expect(req.request.method).toBe('GET');
      req.flush([mockMember]);
    });
  });

  describe('error handling', () => {
    it('should propagate HTTP error on getWorkspaces', () => {
      let errorResponse: unknown = null;

      service.getWorkspaces().subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/workspaces');
      req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

      expect((errorResponse as { status: number }).status).toBe(403);
    });

    it('should propagate HTTP error on createWorkspace', () => {
      let errorResponse: unknown = null;

      service.createWorkspace({ name: '' }).subscribe({
        next: () => {
          throw new Error('should have failed');
        },
        error: (error) => {
          errorResponse = error;
        },
      });

      const req = httpMock.expectOne('/api/admin/workspaces');
      req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

      expect((errorResponse as { status: number }).status).toBe(400);
    });
  });
});

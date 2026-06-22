import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { WorkspaceRoleService } from './workspace-role.service';

describe('WorkspaceRoleService', () => {
  let service: WorkspaceRoleService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        WorkspaceRoleService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(WorkspaceRoleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should start with role NONE and isAdmin=false', () => {
    expect(service.role()).toBe('NONE');
    expect(service.isAdmin()).toBe(false);
  });

  it('should fetch role from /api/workspaces/current/role and update signal', () => {
    service.fetchRole().subscribe((response) => {
      expect(response.role).toBe('ADMIN');
    });
    const req = httpMock.expectOne('/api/workspaces/current/role');
    expect(req.request.method).toBe('GET');
    req.flush({ role: 'ADMIN' });
    expect(service.role()).toBe('ADMIN');
    expect(service.isAdmin()).toBe(true);
  });

  it('should derive isMember true for both ADMIN and MEMBER', () => {
    service.fetchRole().subscribe();
    httpMock.expectOne('/api/workspaces/current/role').flush({ role: 'MEMBER' });
    expect(service.isMember()).toBe(true);
    expect(service.isAdmin()).toBe(false);
  });

  it('should clear role on refreshRole error', () => {
    service.fetchRole().subscribe();
    httpMock.expectOne('/api/workspaces/current/role').flush('error', {
      status: 500,
      statusText: 'Server Error',
    });
    expect(service.role()).toBe('NONE');
  });

  it('should clear role when clearRole is called', () => {
    service.fetchRole().subscribe();
    httpMock.expectOne('/api/workspaces/current/role').flush({ role: 'ADMIN' });
    service.clearRole();
    expect(service.role()).toBe('NONE');
  });
});

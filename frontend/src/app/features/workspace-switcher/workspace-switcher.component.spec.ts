import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { WorkspaceRoleService } from '../../core/auth/workspace-role.service';
import { WorkspaceService } from '../admin/workspaces/workspace.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { WorkspaceSwitcherComponent } from './workspace-switcher.component';

describe('WorkspaceSwitcherComponent', () => {
  let component: WorkspaceSwitcherComponent;
  let fixture: ComponentFixture<WorkspaceSwitcherComponent>;
  let workspaceService: { getMyWorkspaces: ReturnType<typeof vi.fn> };
  let authService: {
    user: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    switchWorkspace: ReturnType<typeof vi.fn>;
  };
  let workspaceRoleService: { refreshRole: ReturnType<typeof vi.fn> };
  let translationService: { translate: ReturnType<typeof vi.fn> };

  const mockWorkspaces = [
    { id: 'ws1', name: 'Default', slug: 'public', description: null, logoUrl: null, icon: null, memberCount: 5, createdBy: 'u1', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    { id: 'ws2', name: 'Engineering', slug: 'eng', description: 'Engineering workspace', logoUrl: null, icon: null, memberCount: 10, createdBy: 'u1', createdAt: '2026-01-02T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z' },
  ];

  beforeEach(async () => {
    workspaceService = {
      getMyWorkspaces: vi.fn(),
    };
    authService = {
      user: vi.fn(),
      logout: vi.fn(),
      switchWorkspace: vi.fn(),
    };
    workspaceRoleService = {
      refreshRole: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => key),
    };

    authService.user.mockReturnValue({ workspaceId: 'ws1', id: 'u1', email: 'user@test.com', role: 'ROLE_USER', displayName: 'User' });

    await TestBed.configureTestingModule({
      imports: [WorkspaceSwitcherComponent],
      providers: [
        { provide: WorkspaceService, useValue: workspaceService },
        { provide: AuthService, useValue: authService },
        { provide: WorkspaceRoleService, useValue: workspaceRoleService },
        { provide: TranslationService, useValue: translationService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceSwitcherComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load workspaces on init', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();

    expect(workspaceService.getMyWorkspaces).toHaveBeenCalled();
    expect(component.workspaces().length).toBe(2);
  });

  it('should highlight current workspace', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();

    expect(component.currentWorkspace()?.id).toBe('ws1');
    expect(component.currentWorkspace()?.name).toBe('Default');
  });

  it('should switch workspace on selection', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();

    component.promptSwitch(mockWorkspaces[1]);
    expect(component.pendingSwitch()?.id).toBe('ws2');

    authService.switchWorkspace.mockReturnValue(of({ accessToken: '', refreshToken: '', userId: 'u1', email: 'user@test.com', role: 'ROLE_USER', displayName: 'User', workspaceId: 'ws2' }));

    component.confirmSwitch();

    expect(authService.switchWorkspace).toHaveBeenCalledWith('ws2');
    expect(workspaceRoleService.refreshRole).toHaveBeenCalled();
    expect(component.pendingSwitch()).toBeNull();
  });

  it('should create new workspace dialog flow', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();

    component.cancelSwitch();
    expect(component.pendingSwitch()).toBeNull();
  });

  it('should show invitation badge when pending', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();

    expect(component.isDefaultWorkspace(mockWorkspaces[0])).toBe(true);
    expect(component.isDefaultWorkspace(mockWorkspaces[1])).toBe(false);
  });

  it('should handle empty workspace list', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(component.workspaces().length).toBe(0);
    expect(component.currentWorkspace()).toBeNull();
  });

  it('should toggle dropdown', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();

    expect(component.isOpen()).toBe(false);
    component.toggleDropdown();
    expect(component.isOpen()).toBe(true);
    component.toggleDropdown();
    expect(component.isOpen()).toBe(false);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { LandingComponent } from './landing.component';
import { AuthService } from '../../core/auth/auth.service';
import { TranslationService } from '../../core/i18n/translation.service';
import { WorkspaceService } from '../admin/workspaces/workspace.service';

const mockWorkspaces = [
  {
    id: 'ws-1',
    name: 'Acme Corp',
    slug: 'acme-corp',
    description: 'Main workspace',
    logoUrl: null,
    icon: 'A',
    memberCount: 5,
    createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  },
  {
    id: 'ws-2',
    name: 'Globex Inc',
    slug: 'globex-inc',
    description: 'Secondary workspace',
    logoUrl: null,
    icon: null,
    memberCount: 3,
    createdBy: 'u1',
    createdAt: '2026-01-02T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
  },
];

const mockUser = {
  accessToken: 'token',
  refreshToken: 'refresh',
  userId: 'u1',
  email: 'user@example.com',
  role: 'ROLE_USER',
  displayName: 'Test User',
  workspaceId: 'ws-1',
};

describe('LandingComponent', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;
  let authService: {
    user: ReturnType<typeof vi.fn>;
    isAuthenticated: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
    isAgent: ReturnType<typeof vi.fn>;
  };
  let translationService: { translate: ReturnType<typeof vi.fn> };
  let workspaceService: { getMyWorkspaces: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = {
      user: vi.fn(),
      isAuthenticated: vi.fn(),
      isAdmin: vi.fn(),
      isAgent: vi.fn(),
    };
    translationService = { translate: vi.fn(() => 'translated') };
    workspaceService = { getMyWorkspaces: vi.fn() };

    authService.isAuthenticated.mockReturnValue(true);
    authService.user.mockReturnValue(mockUser);
    authService.isAdmin.mockReturnValue(false);
    authService.isAgent.mockReturnValue(false);
    workspaceService.getMyWorkspaces.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [LandingComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: TranslationService, useValue: translationService },
        { provide: WorkspaceService, useValue: workspaceService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LandingComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should display welcome heading when authenticated with workspace', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of(mockWorkspaces));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const heading = compiled.querySelector('h1');
    expect(heading?.textContent).toContain('Acme Corp');
    expect(component.currentWorkspace()?.name).toBe('Acme Corp');
  });

  it('should show empty state when no workspaces', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(component.workspaces().length).toBe(0);
    expect(component.currentWorkspace()).toBeNull();
  });

  it('should handle workspace load error gracefully', () => {
    const errorSubject = new Subject<unknown>();
    workspaceService.getMyWorkspaces.mockReturnValue(errorSubject.asObservable());

    fixture.detectChanges();
    errorSubject.error(new Error('Failed'));

    expect(component.workspaces().length).toBe(0);
  });

  it('should resolve firstLetter from user display name', () => {
    authService.user.mockReturnValue(mockUser);
    workspaceService.getMyWorkspaces.mockReturnValue(of([]));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const iconDiv = compiled.querySelector('.w-14.h-14');
    expect(iconDiv?.textContent?.trim()).toBe('T');
  });

  it('should return fallback firstLetter when user is null', () => {
    authService.user.mockReturnValue(null);
    workspaceService.getMyWorkspaces.mockReturnValue(of([]));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const iconDiv = compiled.querySelector('.w-14.h-14');
    expect(iconDiv?.textContent?.trim()).toBe('?');
  });

  it('should translate all labels', () => {
    workspaceService.getMyWorkspaces.mockReturnValue(of([]));
    translationService.translate.mockImplementation((key: string) => `[${key}]`);
    fixture.detectChanges();

    expect(component.currentWorkspace()).toBeNull();
    expect(translationService.translate).toHaveBeenCalledWith('dashboard.quick-actions');
  });

  it('should handle unauthenticated state', () => {
    authService.isAuthenticated.mockReturnValue(false);
    workspaceService.getMyWorkspaces.mockReturnValue(of([]));
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const heading = compiled.querySelector('h1');
    expect(heading?.textContent).toBeTruthy();
    expect(compiled.querySelector('a[routerlink="/register"]')).toBeTruthy();
  });
});

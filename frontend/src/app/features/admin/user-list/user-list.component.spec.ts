import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { UserDto } from '../../../core/auth/auth.models';
import { TranslationService } from '../../../core/i18n/translation.service';
import { ConfirmationDialogService } from '../../../shared/ui/confirmation-dialog/confirmation-dialog.service';
import { UserListComponent } from './user-list.component';

describe('UserListComponent', () => {
  let component: UserListComponent;
  let fixture: ComponentFixture<UserListComponent>;
  let authService: {
    getUsers: ReturnType<typeof vi.fn>;
    updateUserRole: ReturnType<typeof vi.fn>;
    toggleUserStatus: ReturnType<typeof vi.fn>;
  };
  let translationService: { translate: ReturnType<typeof vi.fn> };
  let confirmationDialog: { confirm: ReturnType<typeof vi.fn> };

  const mockUsers: UserDto[] = [
    { id: '1', email: 'admin@test.com', displayName: 'Admin', role: 'ROLE_ADMIN', enabled: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
    { id: '2', email: 'user@test.com', displayName: 'User', role: 'ROLE_USER', enabled: true, createdAt: '2026-01-02T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z' },
    { id: '3', email: 'disabled@test.com', displayName: 'Disabled', role: 'ROLE_USER', enabled: false, createdAt: '2026-01-03T00:00:00Z', updatedAt: '2026-01-03T00:00:00Z' },
  ];

  beforeEach(async () => {
    authService = {
      getUsers: vi.fn(),
      updateUserRole: vi.fn(),
      toggleUserStatus: vi.fn(),
    };
    translationService = {
      translate: vi.fn((key: string) => key),
    };
    confirmationDialog = {
      confirm: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [UserListComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: TranslationService, useValue: translationService },
        { provide: ConfirmationDialogService, useValue: confirmationDialog },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    authService.getUsers.mockReturnValue(of(mockUsers));
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should load users on init', () => {
    authService.getUsers.mockReturnValue(of(mockUsers));
    fixture.detectChanges();

    expect(authService.getUsers).toHaveBeenCalled();
    expect(component['users']().length).toBe(3);
    expect(component['isLoading']()).toBe(false);
  });

  it('should toggle user enabled state', () => {
    authService.getUsers.mockReturnValue(of(mockUsers));
    confirmationDialog.confirm.mockReturnValue(of(true));
    fixture.detectChanges();

    const updatedUser = { ...mockUsers[1], enabled: false };
    authService.toggleUserStatus.mockReturnValue(of(updatedUser));

    component.toggleStatus(mockUsers[1]);

    expect(authService.toggleUserStatus).toHaveBeenCalledWith('2');
    expect(component['users']().find((u) => u.id === '2')?.enabled).toBe(false);
  });

  it('should update user role', () => {
    authService.getUsers.mockReturnValue(of(mockUsers));
    fixture.detectChanges();

    component.openRoleDialog(mockUsers[1]);
    expect(component['editingUser']()).toEqual(mockUsers[1]);
    expect(component['showRoleDialog']()).toBe(true);

    const updatedUser = { ...mockUsers[1], role: 'ROLE_AGENT' };
    authService.updateUserRole.mockReturnValue(of(updatedUser));

    component.updateRole('ROLE_AGENT');

    expect(authService.updateUserRole).toHaveBeenCalledWith('2', 'ROLE_AGENT');
    expect(component['users']().find((u) => u.id === '2')?.role).toBe('ROLE_AGENT');
    expect(component['showRoleDialog']()).toBe(false);
  });

  it('should handle update error gracefully', () => {
    authService.getUsers.mockReturnValue(of(mockUsers));
    fixture.detectChanges();

    component.openRoleDialog(mockUsers[1]);
    const errorSubject = new Subject<unknown>();
    authService.updateUserRole.mockReturnValue(errorSubject.asObservable());

    component.updateRole('ROLE_AGENT');
    errorSubject.error(new Error('Update failed'));

    expect(component['errorMessage']()).toBe('error.update-role');
    expect(component['showRoleDialog']()).toBe(false);
  });

  it('should show loading and loaded states', () => {
    const pendingSubject = new Subject<unknown>();
    authService.getUsers.mockReturnValue(pendingSubject.asObservable());

    fixture.detectChanges();
    expect(component['isLoading']()).toBe(true);

    pendingSubject.next(mockUsers);
    pendingSubject.complete();

    expect(component['isLoading']()).toBe(false);
    expect(component['users']().length).toBe(3);
  });

  it('should handle empty user list', () => {
    authService.getUsers.mockReturnValue(of([]));
    fixture.detectChanges();

    expect(component['users']().length).toBe(0);
    expect(component['isLoading']()).toBe(false);
    expect(component['errorMessage']()).toBe('');
  });
});

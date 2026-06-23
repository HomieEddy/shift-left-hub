import { TestBed } from '@angular/core/testing';
import { Router, UrlSegment } from '@angular/router';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { adminGuard } from './admin.guard';

describe('adminGuard (canMatch)', () => {
  let authService: { isAdmin: ReturnType<typeof vi.fn>; isAuthenticated: ReturnType<typeof vi.fn>; refresh: ReturnType<typeof vi.fn> };
  let navigateSpy: ReturnType<typeof vi.fn>;

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      adminGuard({}, [] as UrlSegment[]),
    );

  beforeEach(() => {
    authService = {
      isAdmin: vi.fn(),
      isAuthenticated: vi.fn(),
      refresh: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([{ path: 'admin', canMatch: [adminGuard], children: [] }]),
      ],
    });
    navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  it('allows when user is admin', () => {
    authService.isAdmin.mockReturnValue(true);
    expect(runGuard()).toBe(true);
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('redirects to home when authenticated but not admin', () => {
    authService.isAdmin.mockReturnValue(false);
    authService.isAuthenticated.mockReturnValue(true);
    expect(runGuard()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  it('refreshes and allows when refresh returns admin', () => {
    authService.isAdmin.mockReturnValueOnce(false).mockReturnValueOnce(true);
    authService.isAuthenticated.mockReturnValue(false);
    authService.refresh.mockReturnValue(of(undefined));
    const result = runGuard();
    if (typeof result === 'object' && 'subscribe' in result) {
      (result as ReturnType<typeof of>).subscribe();
    }
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('redirects to login when refresh fails', () => {
    authService.isAdmin.mockReturnValue(false);
    authService.isAuthenticated.mockReturnValue(false);
    authService.refresh.mockReturnValue(throwError(() => new Error('401')));
    const result = runGuard();
    if (typeof result === 'object' && 'subscribe' in result) {
      (result as ReturnType<typeof of>).subscribe();
    }
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});

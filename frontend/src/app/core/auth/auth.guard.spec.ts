import { TestBed } from '@angular/core/testing';
import { Router, UrlSegment } from '@angular/router';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { authGuard } from './auth.guard';

describe('authGuard (canMatch)', () => {
  let authService: { isAuthenticated: ReturnType<typeof vi.fn>; refresh: ReturnType<typeof vi.fn> };
  let navigateSpy: ReturnType<typeof vi.fn>;

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({}, [] as UrlSegment[]),
    );

  beforeEach(() => {
    authService = {
      isAuthenticated: vi.fn(),
      refresh: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([{ path: 'private', canMatch: [authGuard], children: [] }]),
      ],
    });
    navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  it('allows when user is authenticated', () => {
    authService.isAuthenticated.mockReturnValue(true);
    expect(runGuard()).toBe(true);
  });

  it('redirects to login when refresh fails', () => {
    authService.isAuthenticated.mockReturnValue(false);
    authService.refresh.mockReturnValue(throwError(() => new Error('401')));
    const result = runGuard();
    if (typeof result === 'object' && 'subscribe' in result) {
      (result as ReturnType<typeof of>).subscribe();
    }
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('allows when refresh resolves', () => {
    authService.isAuthenticated.mockReturnValueOnce(false).mockReturnValueOnce(true);
    authService.refresh.mockReturnValue(of(undefined));
    const result = runGuard();
    if (typeof result === 'object' && 'subscribe' in result) {
      (result as ReturnType<typeof of>).subscribe();
    }
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});

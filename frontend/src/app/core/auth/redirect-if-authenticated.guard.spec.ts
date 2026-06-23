import { TestBed } from '@angular/core/testing';
import { Router, UrlSegment } from '@angular/router';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from './auth.service';
import { redirectIfAuthenticatedGuard } from './redirect-if-authenticated.guard';

describe('redirectIfAuthenticatedGuard (canMatch)', () => {
  let authService: { isAuthenticated: ReturnType<typeof vi.fn> };
  let navigateSpy: ReturnType<typeof vi.fn>;

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      redirectIfAuthenticatedGuard({}, [] as UrlSegment[]),
    );

  beforeEach(() => {
    authService = { isAuthenticated: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([{ path: 'login', canMatch: [redirectIfAuthenticatedGuard], children: [] }]),
      ],
    });
    navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  it('redirects to chat when authenticated', () => {
    authService.isAuthenticated.mockReturnValue(true);
    expect(runGuard()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/chat']);
  });

  it('allows when not authenticated', () => {
    authService.isAuthenticated.mockReturnValue(false);
    expect(runGuard()).toBe(true);
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});

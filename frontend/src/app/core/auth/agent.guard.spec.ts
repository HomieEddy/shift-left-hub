import { TestBed } from '@angular/core/testing';
import { Router, UrlSegment } from '@angular/router';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { agentGuard } from './agent.guard';

describe('agentGuard (canMatch)', () => {
  let authService: { isAgent: ReturnType<typeof vi.fn>; isAuthenticated: ReturnType<typeof vi.fn>; refresh: ReturnType<typeof vi.fn> };
  let navigateSpy: ReturnType<typeof vi.fn>;

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      agentGuard({}, [] as UrlSegment[]),
    );

  beforeEach(() => {
    authService = {
      isAgent: vi.fn(),
      isAuthenticated: vi.fn(),
      refresh: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([{ path: 'agent', canMatch: [agentGuard], children: [] }]),
      ],
    });
    navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  it('allows when user is agent', () => {
    authService.isAgent.mockReturnValue(true);
    expect(runGuard()).toBe(true);
  });

  it('redirects to home when authenticated but not agent', () => {
    authService.isAgent.mockReturnValue(false);
    authService.isAuthenticated.mockReturnValue(true);
    expect(runGuard()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/']);
  });

  it('redirects to login when refresh fails', () => {
    authService.isAgent.mockReturnValue(false);
    authService.isAuthenticated.mockReturnValue(false);
    authService.refresh.mockReturnValue(throwError(() => new Error('401')));
    const result = runGuard();
    if (typeof result === 'object' && 'subscribe' in result) {
      (result as ReturnType<typeof of>).subscribe();
    }
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('refreshes successfully when refresh resolves', () => {
    authService.isAgent.mockReturnValueOnce(false).mockReturnValueOnce(true);
    authService.isAuthenticated.mockReturnValue(false);
    authService.refresh.mockReturnValue(of(undefined));
    const result = runGuard();
    if (typeof result === 'object' && 'subscribe' in result) {
      (result as ReturnType<typeof of>).subscribe();
    }
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});

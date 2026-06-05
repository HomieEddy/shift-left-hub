import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const agentGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAgent()) {
    return true;
  }

  if (authService.isAuthenticated()) {
    router.navigate(['/']);
    return false;
  }

  return authService.refresh().pipe(
    map(() => {
      if (authService.isAgent()) return true;
      router.navigate(['/']);
      return false;
    }),
    catchError(() => {
      router.navigate(['/login']);
      return of(false);
    }),
  );
};

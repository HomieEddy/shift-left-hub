import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const adminGuard: CanMatchFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  if (authService.isAuthenticated()) {
    void router.navigate(['/']);
    return false;
  }

  return authService.refresh().pipe(
    map(() => {
      if (authService.isAdmin()) return true;
      void router.navigate(['/']);
      return false;
    }),
    catchError(() => {
      void router.navigate(['/login']);
      return of(false);
    }),
  );
};

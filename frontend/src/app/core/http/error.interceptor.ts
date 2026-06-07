import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { $localize } from '@angular/localize/init';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = $localize`:@@http.error.unexpected:An unexpected error occurred`;

      if (error.status === 401) {
        message = $localize`:@@http.error.sessionExpired:Your session has expired. Please log in again.`;
        router.navigate(['/login']);
      } else if (error.status === 403) {
        message = $localize`:@@http.error.forbidden:You do not have permission to perform this action.`;
      } else if (error.status >= 400 && error.status < 500) {
        message = error.error?.message || error.error?.error || $localize`:@@http.error.invalidRequest:Invalid request`;
      } else if (error.status >= 500) {
        message = $localize`:@@http.error.serverError:Server error. Please try again later.`;
      }

      // TODO: Replace with shared notification service (toast/snackbar) — planned for post-v1.0
      console.error(`[HTTP Error ${error.status}]:`, message);

      return throwError(() => error);
    })
  );
};

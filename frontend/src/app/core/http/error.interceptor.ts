import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = 'An unexpected error occurred';

      if (error.status === 401) {
        message = 'Your session has expired. Please log in again.';
        router.navigate(['/login']);
      } else if (error.status === 403) {
        message = 'You do not have permission to perform this action.';
      } else if (error.status >= 400 && error.status < 500) {
        message = error.error?.message || error.error?.error || 'Invalid request';
      } else if (error.status >= 500) {
        message = 'Server error. Please try again later.';
      }

      // TODO: Replace with toast/snackbar notification in shared components
      console.error(`[HTTP Error ${error.status}]:`, message);

      return throwError(() => error);
    })
  );
};

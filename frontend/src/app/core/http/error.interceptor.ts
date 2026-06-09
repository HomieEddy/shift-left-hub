import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { $localize } from '@angular/localize/init';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/ui/toast/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message: string | null = null;

      if (error.status === 401) {
        message = $localize`:@@http.error.sessionExpired:Your session has expired. Please log in again.`;
        void router.navigate(['/login']);
      } else if (error.status === 403) {
        message = $localize`:@@http.error.forbidden:You do not have permission to perform this action.`;
      } else if (error.status >= 400 && error.status < 500) {
        if (req.url.includes('/auth/refresh') || (error.url?.includes('/auth/refresh') ?? false)) {
          return throwError(() => error);
        }
        const body = error.error as { message?: string; error?: string } | null;
        message = body?.message ?? body?.error ?? $localize`:@@http.error.invalidRequest:Invalid request`;
      } else if (error.status >= 500) {
        message = $localize`:@@http.error.serverError:Server error. Please try again later.`;
      }

      if (message != null) {
        toastService.error(message);
        console.error(`[HTTP Error ${error.status}]:`, message);
      }

      return throwError(() => error);
    })
  );
};

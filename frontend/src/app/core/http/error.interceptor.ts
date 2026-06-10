import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { TranslationService } from '../i18n/translation.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toastService = inject(ToastService);
  const translationService = inject(TranslationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message: string | null = null;

      if (error.status === 401) {
        message = translationService.translate('http.error.sessionExpired');
        void router.navigate(['/login']);
      } else if (error.status === 403) {
        message = translationService.translate('http.error.forbidden');
      } else if (error.status >= 400 && error.status < 500) {
        if (req.url.includes('/auth/refresh') || (error.url?.includes('/auth/refresh') ?? false)) {
          return throwError(() => error);
        }
        const body = error.error as { message?: string; error?: string } | null;
        message = body?.message ?? body?.error ?? translationService.translate('http.error.invalidRequest');
      } else if (error.status >= 500) {
        message = translationService.translate('http.error.serverError');
      }

      if (message != null) {
        toastService.error(message);
        console.error(`[HTTP Error ${error.status}]:`, message);
      }

      return throwError(() => error);
    })
  );
};

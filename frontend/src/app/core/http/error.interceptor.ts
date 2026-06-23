import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, isDevMode } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { LoggerService } from '../logging/logger.service';
import { TranslationService } from '../i18n/translation.service';
import { SUPPRESS_ERROR_TOAST } from './http-context-tokens';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const logger = inject(LoggerService);
  const toastService = inject(ToastService);
  const translationService = inject(TranslationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (req.context.get(SUPPRESS_ERROR_TOAST)) {
        return throwError(() => error);
      }

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
        const body = error.error as
          | { detail?: string; message?: string; error?: string }
          | null;
        // Prefer `detail` (RFC 7807 ProblemDetail emitted by the backend
        // GlobalExceptionHandler) over the legacy `message`/`error` fields.
        message =
          body?.detail ??
          body?.message ??
          body?.error ??
          translationService.translate('http.error.invalidRequest');
      } else if (error.status >= 500) {
        message = translationService.translate('http.error.serverError');
      }

      if (message != null) {
        toastService.error(message);
        if (isDevMode()) {
          logger.error(`[HTTP Error ${error.status}]:`, message);
        }
      }

      return throwError(() => error);
    }),
  );
};

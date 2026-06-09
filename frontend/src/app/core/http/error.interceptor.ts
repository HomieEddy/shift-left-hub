import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslationService } from '../i18n/translation.service';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/ui/toast/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let message = translationService.translate('http.error.unexpected');

      if (error.status === 401) {
        message = translationService.translate('http.error.sessionExpired');
        void router.navigate(['/login']);
      } else if (error.status === 403) {
        message = translationService.translate('http.error.forbidden');
      } else if (error.status >= 400 && error.status < 500) {
        const body = error.error as { message?: string; error?: string } | null;
        message = body?.message ?? body?.error ?? translationService.translate('http.error.invalidRequest');
      } else if (error.status >= 500) {
        message = translationService.translate('http.error.serverError');
      }

      toastService.error(message);
      console.error(`[HTTP Error ${error.status}]:`, message);

      return throwError(() => error);
    })
  );
};

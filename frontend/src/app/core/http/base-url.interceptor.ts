import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthTokenService } from '../auth/auth-token.service';

export const baseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  const authTokenService = inject(AuthTokenService);
  const env = (window as unknown as { __env?: { apiBaseUrl?: string } }).__env;
  const baseUrl = env?.apiBaseUrl ?? '';
  const token = authTokenService.accessToken();
  const headers = token !== null ? req.headers.set('Authorization', `Bearer ${token}`) : req.headers;

  if (baseUrl && req.url.startsWith('/api/')) {
    const apiReq = req.clone({
      url: `${baseUrl.replace(/\/+$/, '')}${req.url}`,
      withCredentials: true,
      headers,
    });
    return next(apiReq);
  }

  if (req.url.startsWith('/api/')) {
    return next(req.clone({ withCredentials: true, headers }));
  }

  return next(req);
};

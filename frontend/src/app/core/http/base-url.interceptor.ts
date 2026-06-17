import { HttpInterceptorFn } from '@angular/common/http';

export const baseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  const env = (window as unknown as { __env?: { apiBaseUrl?: string } }).__env;
  const baseUrl = env?.apiBaseUrl ?? '';

  if (baseUrl && req.url.startsWith('/api/')) {
    const apiReq = req.clone({
      url: `${baseUrl.replace(/\/+$/, '')}${req.url}`,
      withCredentials: true,
    });
    return next(apiReq);
  }

  if (req.url.startsWith('/api/')) {
    return next(req.clone({ withCredentials: true }));
  }

  return next(req);
};

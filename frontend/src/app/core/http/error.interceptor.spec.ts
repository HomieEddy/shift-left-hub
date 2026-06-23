import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { errorInterceptor } from './error.interceptor';
import { provideRouter } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { TranslationService } from '../i18n/translation.service';
import { vi } from 'vitest';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let toast: { error: ReturnType<typeof vi.fn> };
  let translation: { translate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    toast = { error: vi.fn() };
    translation = {
      translate: vi.fn((k: string) => `translated:${k}`),
    };
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ToastService, useValue: toast },
        { provide: TranslationService, useValue: translation },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('prefers ProblemDetail.detail over message and error fields', () => {
    http.get('/api/test').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/test');
    req.flush(
      { type: 'about:blank', title: 'Not Found', detail: 'Article 42 not found', status: 404 },
      { status: 404, statusText: 'Not Found' },
    );

    expect(toast.error).toHaveBeenCalledWith('Article 42 not found');
  });

  it('falls back to message field when detail is absent (legacy Spring responses)', () => {
    http.get('/api/legacy').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/legacy');
    req.flush(
      { message: 'Legacy error message', error: 'Bad Request' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(toast.error).toHaveBeenCalledWith('Legacy error message');
  });

  it('falls back to error field when detail and message are absent', () => {
    http.get('/api/very-legacy').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/very-legacy');
    req.flush(
      { error: 'Very old error string' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(toast.error).toHaveBeenCalledWith('Very old error string');
  });

  it('falls back to translation key when no recognized field is present', () => {
    http.get('/api/empty').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/empty');
    req.flush({}, { status: 400, statusText: 'Bad Request' });

    expect(toast.error).toHaveBeenCalledWith('translated:http.error.invalidRequest');
  });

  it('shows a session-expired toast and navigates to /login for 401', () => {
    http.get('/api/secure').subscribe({ error: () => undefined });

    const req = httpMock.expectOne('/api/secure');
    req.flush(
      { detail: 'Unauthorized' },
      { status: 401, statusText: 'Unauthorized' },
    );

    // The interceptor toasts a session-expired message AND navigates to
    // /login. The toast is intentional (it explains the redirect to the
    // user). The test verifies the toast is called with the session-expired
    // translation key, not the ProblemDetail detail.
    expect(toast.error).toHaveBeenCalledWith('translated:http.error.sessionExpired');
  });
});

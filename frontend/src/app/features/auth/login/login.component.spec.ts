import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: { login: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(async () => {
    authService = { login: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call authService.login on submit and navigate on success', () => {
    authService.login.mockReturnValue(of({ accessToken: 'token', refreshToken: 'refresh', userId: '1', email: 'test@example.com', role: 'ROLE_USER', displayName: 'Test' }));
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.email = 'test@example.com';
    component.password = 'password';
    component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'password',
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/articles']);
  });

  it('should set isLoading to true during submit', () => {
    authService.login.mockReturnValue(new Subject<any>().asObservable());

    component.email = 'test@example.com';
    component.password = 'password';
    component.onSubmit();

    expect(component.isLoading).toBe(true);
  });

  it('should set errorMessage on 401 response', () => {
    authService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));

    component.email = 'wrong@example.com';
    component.password = 'wrong';
    component.onSubmit();

    expect(component.errorMessage).toBe('Invalid email or password.');
    expect(component.isLoading).toBe(false);
  });

  it('should set generic error on non-401 response', () => {
    authService.login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.email = 'test@example.com';
    component.password = 'password';
    component.onSubmit();

    expect(component.errorMessage).toBe('Login failed. Please try again.');
    expect(component.isLoading).toBe(false);
  });
});

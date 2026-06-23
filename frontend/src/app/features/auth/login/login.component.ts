import { Component, DestroyRef, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../../core/auth/auth.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { HttpErrorResponse } from '@angular/common/http';

/** Login form with email/password authentication. */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  protected translationService = inject(TranslationService);

  email = '';
  password = '';
  showPassword = false;
  errorMessage = '';
  isLoading = false;

  onSubmit(): void {
    this.errorMessage = '';
    this.isLoading = true;

    this.authService
      .login({ email: this.email, password: this.password })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/articles']);
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading = false;
          if (err.status === 401) {
            this.errorMessage = this.translationService.translate('error.invalid-credentials');
          } else {
            this.errorMessage = this.translationService.translate('error.login-failed');
          }
        },
        complete: () => {
          this.isLoading = false;
        },
      });
  }
}

import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { TranslationService } from '../../../core/i18n/translation.service';
import { HttpErrorResponse } from '@angular/common/http';

/** Registration form with client-side password validation. */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  protected translationService = inject(TranslationService);

  email = '';
  password = '';
  showPassword = false;
  displayName = '';
  errorMessage = '';
  isLoading = false;
  showPasswordRules = false;

  get passwordStrength(): number {
    let score = 0;
    if (this.password.length >= 8) score++;
    if (/[A-Z]/.test(this.password)) score++;
    if (/[0-9]/.test(this.password)) score++;
    return score;
  }

  get passwordValid(): boolean {
    return this.passwordStrength === 3;
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (!this.passwordValid) {
      this.errorMessage = this.translationService.translate('error.password-rules');
      return;
    }

    this.isLoading = true;

    this.authService.register({
      email: this.email,
      password: this.password,
      displayName: this.displayName,
    }).subscribe({
      next: () => {
        // Auto-login per D-18 — AuthService already set session via tap()
        void this.router.navigate(['/articles']);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        if (err.status === 409) {
          this.errorMessage = this.translationService.translate('error.email-exists');
        } else {
          this.errorMessage = this.translationService.translate('error.registration-failed');
        }
      },
      complete: () => {
        this.isLoading = false;
      },
    });
  }
}

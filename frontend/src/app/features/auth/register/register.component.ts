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
  displayName = '';
  errorMessage = '';
  isLoading = false;
  showPasswordRules = false;

  get passwordValid(): boolean {
    return (
      this.password.length >= 8 &&
      /[A-Z]/.test(this.password) &&
      /[0-9]/.test(this.password)
    );
  }

  onSubmit(): void {
    this.errorMessage = '';

    if (!this.passwordValid) {
      this.errorMessage = 'Password must be at least 8 characters with 1 uppercase letter and 1 number.';
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
          this.errorMessage = 'An account with this email already exists.';
        } else {
          this.errorMessage = 'Registration failed. Please try again.';
        }
      },
      complete: () => {
        this.isLoading = false;
      },
    });
  }
}

import { Page } from '@playwright/test';

/**
 * Page object for the login page (/login).
 *
 * All element selectors use data-testid attributes for
 * stability across markup changes.
 */
export class LoginPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the login page. */
  async goto(): Promise<void> {
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Fill in credentials and submit the login form.
   * Waits for navigation to complete after submission.
   */
  async login(email: string, password: string): Promise<void> {
    await this.page.getByTestId('login-email').fill(email);
    await this.page.getByTestId('login-password').fill(password);
    await this.page.getByTestId('login-submit').click();
    // Successful login redirects to /articles
    await this.page.waitForURL('**/articles');
    await this.page.waitForLoadState('networkidle');
  }
}

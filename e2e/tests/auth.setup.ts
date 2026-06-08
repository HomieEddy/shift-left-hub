import { test as setup, expect } from '@playwright/test';

/** Path relative to project root where authenticated state is stored. */
const AUTH_FILE = '.auth/user.json';

/**
 * Authentication setup for E2E tests.
 *
 * Logs in with test user credentials and saves the storage state
 * (HttpOnly JWT cookies) to .auth/user.json so that subsequent
 * test runs skip the login form.
 *
 * The test credentials are supplied via environment variables:
 *   E2E_USER_EMAIL    (default: user@shiftleft.com)
 *   E2E_USER_PASSWORD (default: ShiftLeft!2026)
 *
 * If the environment is CI, tests that depend on the auth
 * setup will use the pre-seeded storage state from the CI
 * pipeline's test fixture.
 */
setup('authenticate as test user', async ({ page }) => {
  const email = process.env.E2E_USER_EMAIL ?? 'user@shiftleft.com';
  const password = process.env.E2E_USER_PASSWORD ?? 'ShiftLeft!2026';

  // Navigate to the login page
  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  // Fill in credentials using data-testid selectors
  await page.getByTestId('login-email').fill(email);
  await page.getByTestId('login-password').fill(password);

  // Submit the login form
  await page.getByTestId('login-submit').click();

  // Wait for navigation away from login page (successful auth redirects to /articles)
  await page.waitForURL('**/articles');
  await page.waitForLoadState('networkidle');

  // Verify the user is logged in by checking for the logout button
  await expect(page.getByTestId('nav-logout')).toBeVisible();

  // Save the authenticated state to the shared storage file
  await page.context().storageState({ path: AUTH_FILE });
});

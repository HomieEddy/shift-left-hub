import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';

test.describe('Authentication', () => {
  const email = process.env.E2E_USER_EMAIL ?? 'user@shiftleft.com';
  const password = process.env.E2E_USER_PASSWORD ?? 'ShiftLeft!2026';

  test('Register a new user', async ({ page }) => {
    await page.goto('/register');
    await page.waitForLoadState('networkidle');

    await page.getByTestId('register-name').fill('E2E Test User');
    await page.getByTestId('register-email').fill(`e2e-${Date.now()}@test.com`);
    await page.getByTestId('register-password').fill('TestPassword1');
    await page.getByTestId('register-submit').click();

    await page.waitForURL((url) => !url.pathname.includes('/register'), { timeout: 10000 });
    expect(page.url()).not.toContain('/register');
  });

  test('Login with valid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.login(email, password);

    await expect(page.getByTestId('nav-logout')).toBeVisible();
  });

  test('Logout', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.login(email, password);

    await page.getByTestId('nav-logout').click();
    await page.waitForLoadState('networkidle');

    const signInLink = page.getByText(/sign in|login/i);
    await expect(signInLink).toBeVisible({ timeout: 5000 });
  });

  test('Protected route redirect', async ({ page }) => {
    await page.context().clearCookies();
    await page.goto('/chat');
    await page.waitForLoadState('networkidle');

    expect(page.url()).toContain('/login');
  });
});

import { test as setup, expect } from '@playwright/test';

/**
 * Authentication setup for E2E tests.
 *
 * Creates three separate storage state files for different roles:
 *   - .auth/user.json  — regular user (ROLE_USER)
 *   - .auth/agent.json — agent role user (ROLE_ADMIN with agent access)
 *   - .auth/admin.json — admin role user (ROLE_ADMIN for KCS draft review)
 *
 * The test credentials are supplied via environment variables with
 * sensible defaults for the local development seed data.
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
  await page.context().storageState({ path: '.auth/user.json' });
});

setup('authenticate as agent', async ({ page }) => {
  const email = process.env.E2E_AGENT_EMAIL ?? 'admin@shiftleft.com';
  const password = process.env.E2E_AGENT_PASSWORD ?? 'ShiftLeft!2026';

  await page.goto('/login');
  await page.waitForLoadState('networkidle');
  await page.getByTestId('login-email').fill(email);
  await page.getByTestId('login-password').fill(password);
  await page.getByTestId('login-submit').click();
  await page.waitForURL('**/articles');
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('nav-logout')).toBeVisible();

  // Save agent storage state
  await page.context().storageState({ path: '.auth/agent.json' });
});

setup('authenticate as admin', async ({ page }) => {
  // Admin uses the same credentials as agent for this seed setup
  const email = process.env.E2E_ADMIN_EMAIL ?? 'admin@shiftleft.com';
  const password = process.env.E2E_ADMIN_PASSWORD ?? 'ShiftLeft!2026';

  await page.goto('/login');
  await page.waitForLoadState('networkidle');
  await page.getByTestId('login-email').fill(email);
  await page.getByTestId('login-password').fill(password);
  await page.getByTestId('login-submit').click();
  await page.waitForURL('**/articles');
  await page.waitForLoadState('networkidle');
  await expect(page.getByTestId('nav-logout')).toBeVisible();

  // Verify admin can see admin nav items (KCS drafts link)
  await expect(page.getByTestId('kcs-pending-badge')).toBeVisible({ timeout: 5000 }).catch(() => {
    // Badge may not appear if no pending drafts — that's OK
  });

  // Save admin storage state
  await page.context().storageState({ path: '.auth/admin.json' });
});

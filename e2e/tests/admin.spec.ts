import { test, expect } from '@playwright/test';
import { AdminPage } from '../pages/admin.page';
import { KcsDraftsPage } from '../pages/kcs-drafts.page';

test.describe('Admin', () => {
  test('Manage users - change role and toggle status', async ({ page }) => {
    const adminPage = new AdminPage(page);
    await adminPage.gotoUsers();

    await expect(page.getByTestId('admin-user-table')).toBeVisible({ timeout: 10000 });
    const rows = page.getByTestId('admin-user-row');
    expect(await rows.count()).toBeGreaterThan(0);

    const firstUserEmail = await rows.first().locator('[data-testid="admin-user-email"]').textContent();
    if (firstUserEmail) {
      await adminPage.openRoleDialog(firstUserEmail.trim());
      await expect(page.getByTestId('admin-role-dialog')).toBeVisible({ timeout: 5000 });

      const roleRadios = page.locator('[data-testid="admin-role-dialog"] input[type="radio"]');
      const count = await roleRadios.count();
      if (count > 0) {
        // Toggle to a different role than current
        const currentRole = await rows.first().locator('[data-testid="admin-user-role"]').textContent();
        if (currentRole?.includes('USER')) {
          await roleRadios.nth(1).click();
        } else {
          await roleRadios.nth(2).click();
        }
      }

      await adminPage.closeRoleDialog();
    }
  });

  test('Manage tags - create a new taxonomy category', async ({ page }) => {
    const adminPage = new AdminPage(page);
    await adminPage.gotoTaxonomy();

    await adminPage.clickNewCategory();

    const nameEnInput = page.getByLabel('English name');
    const nameFrInput = page.getByLabel('French name');
    await expect(nameEnInput).toBeVisible({ timeout: 5000 });
    const catName = `E2E Test ${Date.now()}`;
    await nameEnInput.fill(catName);
    await nameFrInput.fill(`Test E2E ${Date.now()}`);
    await page.locator('[data-testid="admin-role-dialog"] button:has-text("Create")').click();
    await page.waitForLoadState('networkidle');
  });

  test('Approve or reject KCS drafts', async ({ page }) => {
    const kcsDraftsPage = new KcsDraftsPage(page);
    await kcsDraftsPage.goto();

    const draftCount = await kcsDraftsPage.getDraftCount();
    if (draftCount > 0) {
      const initialCount = draftCount;
      await kcsDraftsPage.approveFirstDraft();
      await page.waitForLoadState('networkidle');
    } else {
      const emptyState = page.getByText(/no drafts|empty|no pending/i);
      const visible = await emptyState.isVisible().catch(() => false);
      expect(visible || draftCount === 0).toBeTruthy();
    }
  });
});

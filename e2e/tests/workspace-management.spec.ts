import { test, expect } from '@playwright/test';
import { WorkspaceManagementPage } from '../pages/workspace-management.page';

test.describe('Workspace Management', () => {
  test('Switch workspace', async ({ page }) => {
    const wsPage = new WorkspaceManagementPage(page);
    await wsPage.goto();

    await wsPage.openWorkspaceSwitcher();
    await wsPage.selectWorkspace('Public');
    await wsPage.confirmWorkspaceSwitch();

    const currentName = await wsPage.getCurrentWorkspaceName();
    expect(currentName).toContain('Public');
  });

  test('View workspace members as admin', async ({ page, context }) => {
    const browserInstance = context.browser();
    if (!browserInstance) {
      throw new Error('Browser instance not available for admin context');
    }
    const adminContext = await browserInstance.newContext({ storageState: '.auth/admin.json' });
    const adminPage = await adminContext.newPage();

    await adminPage.goto('/admin/users');
    await adminPage.waitForLoadState('networkidle');

    await expect(adminPage.getByTestId('admin-user-table')).toBeVisible({ timeout: 10000 });
    const rows = adminPage.getByTestId('admin-user-row');
    expect(await rows.count()).toBeGreaterThan(0);

    await adminContext.close();
  });
});

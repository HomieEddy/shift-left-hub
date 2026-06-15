import { Page, Locator, expect } from '@playwright/test';

/**
 * Page object for the Admin panel covering user management, taxonomy,
 * and KCS draft navigation.
 *
 * Provides methods to navigate admin sections and interact with
 * user rows, taxonomy tree nodes, and document management.
 */
export class AdminPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the admin users page. */
  async gotoUsers(): Promise<void> {
    await this.page.goto('/admin/users');
    await this.page.waitForLoadState('networkidle');
  }

  /** Find a user row by email text content within the table. */
  getUserRow(email: string): Locator {
    return this.page.locator(`[data-testid="admin-user-row"]:has-text("${email}")`);
  }

  /** Open the edit role dialog for a user identified by email. */
  async openRoleDialog(email: string): Promise<void> {
    await this.getUserRow(email).locator('[data-testid="admin-edit-role-btn"]').click();
    await this.page.waitForSelector('[data-testid="admin-role-dialog"]', { timeout: 5000 });
  }

  /** Select a role in the role dialog by radio input value. */
  async selectRole(role: string): Promise<void> {
    await this.page.locator(`[data-testid="admin-role-dialog"] input[value="${role}"]`).click();
  }

  /** Close the role dialog. */
  async closeRoleDialog(): Promise<void> {
    await this.page.locator('[data-testid="admin-role-dialog"] button:has-text("Cancel")').click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Toggle a user's enabled/disabled status. */
  async toggleUserStatus(email: string): Promise<void> {
    await this.getUserRow(email).locator('[data-testid="admin-toggle-status-btn"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Navigate to the admin taxonomy page. */
  async gotoTaxonomy(): Promise<void> {
    await this.page.goto('/admin/taxonomy');
    await this.page.waitForLoadState('networkidle');
  }

  /** Click the "New Category" button. */
  async clickNewCategory(): Promise<void> {
    await this.page.getByTestId('taxonomy-new-btn').click();
  }

  /** Fill the create category form with English and French names. */
  async fillCategoryForm(nameEn: string, nameFr: string): Promise<void> {
    await this.page.getByLabel('English name').fill(nameEn);
    await this.page.getByLabel('French name').fill(nameFr);
  }

  /** Submit the category creation form. */
  async submitCategory(): Promise<void> {
    await this.page.locator('[data-testid="admin-role-dialog"] button:has-text("Create")').click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Find a taxonomy tree node by name text. */
  getCategoryNode(name: string): Locator {
    return this.page.locator(`[data-testid^="taxonomy-node-"]:has-text("${name}")`);
  }

  /** Navigate to the KCS drafts admin page. */
  async gotoKcsDrafts(): Promise<void> {
    await this.page.goto('/admin/kcs-drafts');
    await this.page.waitForLoadState('networkidle');
  }
}

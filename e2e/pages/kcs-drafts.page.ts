import { Page, expect } from '@playwright/test';

/**
 * Page object for the Admin KCS Drafts queue (/admin/kcs-drafts).
 *
 * Provides methods to view, approve, and reject AI-drafted knowledge base
 * articles. Used by the golden path to verify KCS draft creation after
 * an agent flags a ticket as a knowledge gap.
 */
export class KcsDraftsPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the KCS drafts admin page. */
  async goto(): Promise<void> {
    await this.page.goto('/admin/kcs-drafts');
    await this.page.waitForLoadState('networkidle');
  }

  /** Get the number of draft rows visible in the table. */
  async getDraftCount(): Promise<number> {
    return this.page.locator('[data-testid="draft-row"]').count();
  }

  /** Get the title text of the first draft in the queue. */
  async getFirstDraftTitle(): Promise<string | null> {
    const titleEl = this.page.locator('[data-testid="draft-title"]').first();
    return (await titleEl.isVisible()) ? titleEl.textContent() : null;
  }

  /** Approve the first draft in the queue. */
  async approveFirstDraft(): Promise<void> {
    await this.page.locator('[data-testid="approve-draft-btn"]').first().click();
    // Wait for the confirmation modal and confirm
    await this.page.waitForTimeout(500);
    await this.page.locator('[data-testid="modal-confirm-btn"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Reject the first draft in the queue. */
  async rejectFirstDraft(): Promise<void> {
    await this.page.locator('[data-testid="reject-draft-btn"]').first().click();
    // Wait for the confirmation modal and confirm
    await this.page.waitForTimeout(500);
    await this.page.locator('[data-testid="modal-confirm-btn"]').click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Get the pending KCS draft count from the nav badge. */
  async getPendingBadgeCount(): Promise<number> {
    const badge = this.page.locator('[data-testid="kcs-pending-badge"]');
    if (!(await badge.isVisible().catch(() => false))) return 0;
    const text = await badge.textContent();
    return text ? parseInt(text.trim(), 10) : 0;
  }

  /** Assert that at least one draft row exists. */
  async expectDraftExists(): Promise<void> {
    await expect(
      this.page.locator('[data-testid="draft-row"]').first(),
    ).toBeVisible({ timeout: 10000 });
  }
}

import { Page, Locator, expect } from '@playwright/test';

/**
 * Page object for the Agent Dashboard (/agent/tickets and /agent/tickets/:id).
 *
 * Covers claiming a ticket from the queue and resolving it with
 * resolution notes and the knowledge gap flag.
 */
export class AgentDashboardPage {
  /* ---- Ticket Queue (list page) ---- */
  readonly ticketQueue: Locator;
  readonly claimButton: Locator;

  /* ---- Ticket Detail (resolve page) ---- */
  readonly resolveForm: Locator;
  readonly resolutionNotes: Locator;
  readonly knowledgeGapCheckbox: Locator;
  readonly resolveButton: Locator;
  readonly confirmResolveButton: Locator;

  constructor(private readonly page: Page) {
    /* List page */
    this.ticketQueue = this.page.locator('table tbody tr');
    this.claimButton = this.page.getByTestId('agent-claim');

    /* Detail page */
    this.resolveForm = this.page.locator('app-agent-ticket-detail');
    this.resolutionNotes = this.page.getByTestId('agent-resolution-notes');
    this.knowledgeGapCheckbox = this.page.getByTestId('agent-knowledge-gap');
    this.resolveButton = this.page.getByTestId('agent-resolve');
    this.confirmResolveButton = this.page.getByTestId('agent-confirm-resolve');
  }

  /** Navigate to the agent ticket queue. */
  async gotoQueue(): Promise<void> {
    await this.page.goto('/agent/tickets');
    await this.page.waitForLoadState('networkidle');
  }

  /** Alias: navigate to the agent dashboard (ticket queue). */
  async goto(): Promise<void> {
    return this.gotoQueue();
  }

  /** Wait for the ticket queue to have at least one row. */
  async waitForTickets(timeout = 15000): Promise<void> {
    await expect(this.ticketQueue.first()).toBeVisible({ timeout });
  }

  /** Get the number of tickets in the queue. */
  async getTicketCount(): Promise<number> {
    return this.ticketQueue.count();
  }

  /**
   * Claim the first NEW ticket in the queue.
   * 1. Clicks the "Claim" button on the first NEW row
   * 2. Confirms in the claim modal.
   * After claiming, the page should redirect to the ticket detail.
   */
  async claimTicket(): Promise<void> {
    // Click the first visible Claim button to open the confirmation modal
    await this.page.locator('table tbody tr').locator('button').first().click();
    // Wait for the confirmation modal
    await this.page.waitForSelector('.fixed.inset-0', { timeout: 5000 });
    // Click the "Confirm" button in the modal (second button in the modal)
    const confirmModal = this.page.locator('.fixed.inset-0 .bg-white');
    await confirmModal.locator('button').last().click();
    // Wait for navigation to ticket detail page (/agent/tickets/:id)
    await this.page.waitForURL(/\/agent\/tickets\/[0-9a-f-]+/);
    await this.page.waitForLoadState('networkidle');
  }

  /** Claim the first ticket in the queue. Alias for claimTicket(). */
  async claimFirstTicket(): Promise<void> {
    return this.claimTicket();
  }

  /** Fill resolution notes textarea. */
  async fillResolutionNotes(text: string): Promise<void> {
    await this.resolutionNotes.fill(text);
  }

  /** Check (or uncheck) the knowledge gap checkbox. */
  async clickKnowledgeGapCheckbox(): Promise<void> {
    await this.knowledgeGapCheckbox.check();
  }

  /** Click the resolve button and confirm in the modal. */
  async clickResolve(): Promise<void> {
    await this.resolveButton.click();
    // Wait for confirmation modal
    await this.page.waitForSelector('.fixed.inset-0', { timeout: 5000 });
    // Click confirm
    await this.confirmResolveButton.click();
    // Wait for the page to reflect the RESOLVED state
    await this.page.waitForFunction(
      () => document.querySelector('.bg-green-50') !== null,
      { timeout: 10000 },
    );
  }

  /** Get the current ticket status text (e.g., "Resolved"). */
  async getTicketStatus(): Promise<string> {
    const statusEl = this.page.getByTestId('ticket-resolved');
    await expect(statusEl).toBeVisible({ timeout: 10000 });
    return statusEl.textContent() ?? '';
  }

  /**
   * Resolve the ticket with the given notes.
   *
   * @param notes         Resolution notes text
   * @param isKnowledgeGap Whether to flag as a knowledge gap for KCS
   */
  async resolveTicket(notes: string, isKnowledgeGap = false): Promise<void> {
    await this.fillResolutionNotes(notes);
    if (isKnowledgeGap) {
      await this.clickKnowledgeGapCheckbox();
    }
    await this.clickResolve();
  }
}

import { Page, Locator } from '@playwright/test';

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

  /**
   * Claim the first NEW ticket in the queue.
   * 1. Clicks the "Claim" button on the first NEW row
   * 2. Confirms in the claim modal (the button text is "Claim")
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
    await this.page.waitForURL(/\/agent\/tickets\/\d+/);
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Resolve the ticket with the given notes.
   *
   * @param notes         Resolution notes text
   * @param isKnowledgeGap Whether to flag as a knowledge gap for KCS
   */
  async resolveTicket(notes: string, isKnowledgeGap = false): Promise<void> {
    // Fill resolution notes
    await this.resolutionNotes.fill(notes);

    // Check knowledge gap if needed
    if (isKnowledgeGap) {
      await this.knowledgeGapCheckbox.check();
    }

    // Click resolve button to open confirmation modal
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
}

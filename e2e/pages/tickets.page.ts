import { Page, Locator } from '@playwright/test';

/**
 * Page object for the escalation form and Ticket list.
 *
 * The escalation form is rendered inline in the chat page as
 * <app-escalation-form>. The ticket list is at /tickets.
 */
export class TicketsPage {
  /* ---- Escalation form selectors (inline in chat) ---- */
  readonly escalationForm: Locator;
  readonly issueTextarea: Locator;
  readonly categorySelect: Locator;
  readonly urgencySelect: Locator;
  readonly submitButton: Locator;

  /* ---- My Tickets page ---- */
  readonly ticketTable: Locator;
  readonly ticketList: Locator;

  constructor(private readonly page: Page) {
    this.escalationForm = this.page.locator('app-escalation-form');
    this.issueTextarea = this.page.getByTestId('escalation-issue');
    this.categorySelect = this.page.getByTestId('escalation-category');
    this.urgencySelect = this.page.getByTestId('escalation-urgency');
    this.submitButton = this.page.getByTestId('escalation-submit');
    this.ticketTable = this.page.locator('table tbody');
    this.ticketList = this.page.locator('table tbody tr');
  }

  /**
   * Fill in and submit the escalation form.
   *
   * @param issue - Description of the issue
   * @param category - Category value (e.g. 'SOFTWARE', 'HARDWARE', 'NETWORK', 'ACCESS')
   * @param urgency  - Urgency value (e.g. 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
   */
  async createEscalation(issue: string, category: string, urgency: string): Promise<void> {
    await this.issueTextarea.fill(issue);
    await this.categorySelect.selectOption(category);
    await this.urgencySelect.selectOption(urgency);
    await this.submitButton.click();
    // Wait for success state (ticket number shown)
    await this.page.waitForFunction(
      () => document.querySelector('app-escalation-form')?.textContent?.includes('Ticket created'),
      { timeout: 10000 },
    );
  }

  /** Navigate to the My Tickets page. */
  async gotoMyTickets(): Promise<void> {
    await this.page.goto('/tickets');
    await this.page.waitForLoadState('networkidle');
  }

  /** Get the number of ticket rows in the table. */
  async ticketCount(): Promise<number> {
    return this.ticketList.count();
  }

  /** Get the first ticket number from the tickets table. */
  async getFirstTicketNumber(): Promise<string | null> {
    const firstRow = this.ticketList.first();
    if (!(await firstRow.isVisible().catch(() => false))) return null;
    const text = await firstRow.textContent();
    const match = text?.match(/TKT-\d{4}/);
    return match ? match[0] : null;
  }
}

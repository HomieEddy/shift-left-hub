import { Page, Locator, expect } from '@playwright/test';

/**
 * Page object for the AI Chat interface (/chat).
 *
 * Provides high-level methods to interact with the chat:
 * send a message, wait for the AI response, and escalate
 * to a human agent.
 */
export class ChatPage {
  readonly messageInput: Locator;
  readonly sendButton: Locator;
  readonly chatMessages: Locator;
  readonly escalateButton: Locator;
  readonly feedbackYesButton: Locator;
  readonly feedbackNoButton: Locator;

  constructor(private readonly page: Page) {
    this.messageInput = this.page.getByTestId('chat-input');
    this.sendButton = this.page.getByTestId('chat-send');
    this.chatMessages = this.page.getByTestId('chat-messages');
    this.escalateButton = this.page.getByTestId('chat-escalate');
    this.feedbackYesButton = this.page.getByTestId('chat-feedback-yes');
    this.feedbackNoButton = this.page.getByTestId('chat-feedback-no');
  }

  /** Navigate to the chat page. */
  async goto(): Promise<void> {
    await this.page.goto('/chat');
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Type a message and click send.
   * Does NOT wait for the response — use waitForResponse() for that.
   */
  async sendMessage(text: string): Promise<void> {
    await this.messageInput.fill(text);
    await this.sendButton.click();
  }

  /**
   * Wait for an AI response to appear in the chat.
   * Returns true if a response was received within timeout, false if fallback appeared instead.
   */
  async waitForResponse(timeout = 30000): Promise<boolean> {
    try {
      await this.page.waitForFunction(
        () => {
          const container = document.querySelector('[data-testid="chat-messages"]');
          if (!container) return false;
          const assistantBubbles = container.querySelectorAll('[data-testid="assistant-message"]');
          return assistantBubbles.length > 0;
        },
        { timeout },
      );
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Check if the fallback/escalate section is visible.
   */
  async isFallbackVisible(): Promise<boolean> {
    return this.escalateButton.isVisible().catch(() => false);
  }

  /**
   * Click the "Escalate to Human Agent" button and wait for escalation form.
   */
  async clickEscalate(): Promise<void> {
    await this.escalateButton.click();
    await this.page.waitForSelector('app-escalation-form', { timeout: 10000 });
  }

  /** Alias for clickEscalate() — kept for backward compatibility. */
  async escalate(): Promise<void> {
    return this.clickEscalate();
  }

  /**
   * Click "Did this solve your problem?" → No to trigger escalate option.
   */
  async clickDidNotSolve(): Promise<void> {
    await this.feedbackNoButton.click();
    // Wait a moment for the escalate option to appear
    await this.page.waitForTimeout(500);
  }

  /**
   * Fill and submit the escalation form with category and urgency.
   * The issue description is pre-filled from the AI chat context.
   */
  async submitEscalationForm(category: string, urgency: string): Promise<void> {
    await this.page.getByTestId('escalation-category').selectOption(category);
    await this.page.getByTestId('escalation-urgency').selectOption(urgency);
    await this.page.getByTestId('escalation-submit').click();
    // Wait for the success confirmation
    await this.page.waitForFunction(
      () => document.querySelector('app-escalation-form')?.textContent?.includes('Ticket created'),
      { timeout: 10000 },
    );
  }

  /**
   * Get the ticket number from the escalation confirmation.
   */
  async getConfirmationTicketNumber(): Promise<string> {
    const confirmation = this.page.locator('app-escalation-form');
    const text = await confirmation.textContent();
    const match = text?.match(/TKT-\d{4}/);
    return match ? match[0] : '';
  }
}

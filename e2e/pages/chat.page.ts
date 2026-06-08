import { Page, Locator } from '@playwright/test';

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

  constructor(private readonly page: Page) {
    this.messageInput = this.page.getByTestId('chat-input');
    this.sendButton = this.page.getByTestId('chat-send');
    this.chatMessages = this.page.getByTestId('chat-messages');
    this.escalateButton = this.page.getByTestId('chat-escalate');
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
   * Polls for an assistant-style message bubble to appear inside the chat container.
   */
  async waitForResponse(timeout = 30000): Promise<void> {
    await this.page.waitForFunction(
      () => {
        const container = document.querySelector('[data-testid="chat-messages"]');
        if (!container) return false;
        const assistantBubbles = container.querySelectorAll('.bg-slate-100');
        return assistantBubbles.length > 0;
      },
      { timeout },
    );
  }

  /**
   * Click the "Escalate to Human Agent" button.
   * This should trigger the escalation form to appear.
   */
  async escalate(): Promise<void> {
    await this.escalateButton.click();
    // Wait for the escalation form to be visible (app-escalation-form)
    await this.page.waitForSelector('app-escalation-form', { timeout: 10000 });
  }
}

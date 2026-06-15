import { test, expect } from '@playwright/test';
import { ChatPage } from '../pages/chat.page';

test.describe('Escalation', () => {
  test('User requests human agent and ticket is created', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();

    await chatPage.sendMessage('How do I configure my VPN?');
    const responded = await chatPage.waitForResponse(30000);

    if (responded) {
      const feedbackVisible = await page.getByTestId('chat-feedback-prompt').isVisible({ timeout: 10000 }).catch(() => false);
      if (feedbackVisible) {
        await chatPage.clickDidNotSolve();
      }
      await chatPage.clickEscalate();
    } else {
      await chatPage.clickEscalate();
    }

    await chatPage.submitEscalationForm('NETWORK', 'HIGH');

    const ticketNumber = await chatPage.getConfirmationTicketNumber();
    expect(ticketNumber).toMatch(/TKT-\d{4}/);

    await page.goto('/tickets');
    await page.waitForLoadState('networkidle');
  });

  test('Agent views and resolves the ticket', async ({ page, context }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();

    await chatPage.sendMessage('How do I reset my password?');
    await chatPage.waitForResponse(30000);
    const feedbackVisible = await page.getByTestId('chat-feedback-prompt').isVisible({ timeout: 5000 }).catch(() => false);
    if (feedbackVisible) {
      await chatPage.clickDidNotSolve();
    }
    await chatPage.clickEscalate();
    await chatPage.submitEscalationForm('ACCESS', 'MEDIUM');

    const browserInstance = context.browser();
    if (!browserInstance) {
      throw new Error('Browser instance not available for agent context');
    }
    const agentContext = await browserInstance.newContext({ storageState: '.auth/agent.json' });
    const agentPage = await agentContext.newPage();
    await agentPage.goto('/agent/tickets');
    await agentPage.waitForLoadState('networkidle');

    await agentPage.waitForSelector('[data-testid="agent-claim"]', { timeout: 10000 });
    await agentPage.locator('[data-testid="agent-claim"]').first().click();

    try {
      await agentPage.waitForSelector('[data-testid="claim-modal"]', { timeout: 5000 });
      await agentPage.locator('[data-testid="claim-modal"] button').last().click();
      await agentPage.waitForURL(/\/agent\/tickets\//);
      await agentPage.waitForLoadState('networkidle');
    } catch {
      // May have auto-navigated
    }

    await agentPage.getByTestId('agent-resolution-notes').fill('Resolved via E2E test');
    await agentPage.getByTestId('agent-knowledge-gap').check();
    await agentPage.getByTestId('agent-resolve').click();
    await agentPage.waitForSelector('[data-testid="resolve-confirm-modal"]', { timeout: 5000 });
    await agentPage.getByTestId('agent-confirm-resolve').click();

    await expect(agentPage.getByTestId('ticket-resolved')).toBeVisible({ timeout: 10000 });
    await agentContext.close();
  });
});

import { test, expect } from '@playwright/test';
import { ChatPage } from '../pages/chat.page';

test.describe('AI Self-Service', () => {
  test('Ask question and receive streamed response', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();

    await chatPage.sendMessage('How do I reset my network password?');
    const responded = await chatPage.waitForResponse(30000);

    if (responded) {
      await expect(page.getByTestId('assistant-message').first()).toBeVisible({ timeout: 5000 });
    } else {
      await expect(page.getByTestId('chat-escalate')).toBeVisible({ timeout: 5000 });
    }
  });

  test('Rate the answer', async ({ page }) => {
    const chatPage = new ChatPage(page);
    await chatPage.goto();

    await chatPage.sendMessage('How do I configure my VPN?');
    const responded = await chatPage.waitForResponse(30000);

    if (responded) {
      const feedbackVisible = await page.getByTestId('chat-feedback-prompt').isVisible({ timeout: 10000 }).catch(() => false);
      if (feedbackVisible) {
        await page.getByTestId('chat-feedback-yes').click();
        await expect(page.getByTestId('chat-feedback-prompt')).not.toBeVisible({ timeout: 5000 });
      }
    } else {
      await expect(page.getByTestId('chat-escalate')).toBeVisible({ timeout: 5000 });
    }
  });
});

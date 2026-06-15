import { test, expect } from '@playwright/test';
import { KnowledgeBasePage } from '../pages/knowledge-base.page';

test.describe('Knowledge Base', () => {
  test('Browse articles list', async ({ page }) => {
    await page.goto('/articles');
    await page.waitForLoadState('networkidle');

    await expect(page.getByTestId('article-list')).toBeVisible({ timeout: 10000 });
    const cards = page.getByTestId('article-card');
    const count = await cards.count();
    expect(count).toBeGreaterThan(0);
  });

  test('Search articles', async ({ page }) => {
    const kbPage = new KnowledgeBasePage(page);
    await kbPage.goto();
    await kbPage.search('password');

    await expect(kbPage.searchResults.first()).toBeVisible({ timeout: 10000 });
    const titles = await kbPage.getResultTitles();
    expect(titles.length).toBeGreaterThan(0);
  });

  test('View article content', async ({ page }) => {
    await page.goto('/articles');
    await page.waitForLoadState('networkidle');

    const firstCard = page.getByTestId('article-card').first();
    await firstCard.locator('a').click();
    await page.waitForLoadState('networkidle');

    await expect(page.getByTestId('article-viewer')).toBeVisible({ timeout: 10000 });
  });

  test('Bilingual content switching', async ({ page }) => {
    await page.goto('/articles');
    await page.waitForLoadState('networkidle');

    const firstCard = page.getByTestId('article-card').first();
    await firstCard.locator('a').click();
    await page.waitForLoadState('networkidle');

    await expect(page.getByTestId('article-viewer')).toBeVisible({ timeout: 10000 });

    const frButton = page.getByText('FR');
    if (await frButton.isVisible()) {
      await frButton.click();
      await page.waitForLoadState('networkidle');
      await expect(page.getByTestId('article-viewer')).toBeVisible();
    }

    const enButton = page.getByText('EN');
    if (await enButton.isVisible()) {
      await enButton.click();
      await page.waitForLoadState('networkidle');
      await expect(page.getByTestId('article-viewer')).toBeVisible();
    }
  });
});

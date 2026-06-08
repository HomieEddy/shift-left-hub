import { Page, Locator } from '@playwright/test';
import { expect } from '@playwright/test';

/**
 * Page object for the Knowledge Base article search page (/articles/search).
 *
 * Covers searching articles, viewing results, and opening an article
 * in the article viewer.
 */
export class KnowledgeBasePage {
  readonly searchInput: Locator;
  readonly searchResults: Locator;
  readonly articleViewer: Locator;

  constructor(private readonly page: Page) {
    this.searchInput = this.page.getByTestId('kb-search-input');
    this.searchResults = this.page.getByTestId('kb-search-result');
    this.articleViewer = this.page.locator('article');
  }

  /** Navigate to the KB search page. */
  async goto(): Promise<void> {
    await this.page.goto('/articles');
    await this.page.waitForLoadState('networkidle');
    // Click the "Search Articles" link to reach the search page
    await this.page.getByText('Search Articles').click();
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Type a query into the search input.
   * The search fires on input change (debounced), so we wait for
   * results to appear after typing.
   */
  async search(query: string): Promise<void> {
    await this.searchInput.fill(query);
    // Wait for the search results to load (debounced API call)
    await this.page.waitForResponse(
      (resp) =>
        resp.url().includes('/api/articles/search') && resp.status() === 200,
    );
  }

  /**
   * Click on a search result at the given index (0-based) to open
   * the article viewer page.
   */
  async openArticle(index: number): Promise<void> {
    const results = this.searchResults;
    await results.nth(index).locator('a').click();
    // Wait for the article viewer to load (the <article> element)
    await expect(this.articleViewer).toBeVisible({ timeout: 10000 });
  }
}

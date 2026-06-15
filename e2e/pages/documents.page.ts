import { Page, Locator } from '@playwright/test';

/**
 * Page object for the document management page (/admin/documents).
 *
 * Provides methods to upload files, verify indexing status,
 * and manage document lifecycle (create article, delete).
 */
export class DocumentsPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the admin documents page. */
  async goto(): Promise<void> {
    await this.page.goto('/admin/documents');
    await this.page.waitForLoadState('networkidle');
  }

  /**
   * Upload a file via the hidden file input.
   * The testid `doc-file-input` will be added to the hidden
   * `<input #fileInput>` in the spec plan.
   *
   * @param filePath Absolute path to the file to upload
   */
  async uploadFile(filePath: string): Promise<void> {
    await this.page.getByTestId('doc-file-input').setInputFiles(filePath);
    await this.page.waitForLoadState('networkidle');
  }

  /** Select a category from the dropdown for upload. */
  async selectCategory(categoryId: string): Promise<void> {
    await this.page.getByTestId('doc-category-select').selectOption(categoryId);
  }

  /** Find a document row by filename in the document table. */
  getDocumentRow(filename: string): Locator {
    return this.page.locator(`[data-testid="doc-row"]:has-text("${filename}")`);
  }

  /** Read the status badge text for a document. */
  async getDocumentStatus(filename: string): Promise<string> {
    return (await this.getDocumentRow(filename).locator('[data-testid="doc-status"]').textContent()) ?? '';
  }

  /** Poll until document status is READY or INDEXED (max 60s). */
  async waitForIndexed(filename: string, timeout = 60000): Promise<void> {
    const start = Date.now();
    const row = this.getDocumentRow(filename);
    while (Date.now() - start < timeout) {
      const status = await row.locator('[data-testid="doc-status"]').textContent().catch(() => '');
      if (status && (status.includes('READY') || status.includes('INDEXED'))) return;
      await this.page.waitForTimeout(1000);
    }
    throw new Error(`Timed out waiting for ${filename} to be indexed (${timeout}ms)`);
  }

  /** Click the delete button for a document by filename. */
  async clickDelete(filename: string): Promise<void> {
    await this.getDocumentRow(filename).locator('[data-testid="doc-delete-btn"]').click();
  }

  /** Confirm the document deletion modal. */
  async confirmDelete(): Promise<void> {
    await this.page.locator('[data-testid="doc-delete-modal"] button:has-text("Confirm")').click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Click the "Create Article" button for a processed document. */
  async clickCreateArticle(filename: string): Promise<void> {
    await this.getDocumentRow(filename).locator('[data-testid="doc-create-article-btn"]').click();
    await this.page.waitForLoadState('networkidle');
  }
}

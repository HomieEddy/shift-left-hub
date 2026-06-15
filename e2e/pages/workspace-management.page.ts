import { Page, Locator } from '@playwright/test';

/**
 * Page object for workspace switching and member management.
 *
 * The workspace switcher is available in the top navigation from
 * any authenticated page. Provides methods to switch workspaces
 * and access workspace member management.
 */
export class WorkspaceManagementPage {
  constructor(private readonly page: Page) {}

  /** Navigate to the default landing page where workspace switcher is visible. */
  async goto(): Promise<void> {
    await this.page.goto('/articles');
    await this.page.waitForLoadState('networkidle');
  }

  /** Open the workspace switcher dropdown. */
  async openWorkspaceSwitcher(): Promise<void> {
    await this.page.getByTestId('workspace-toggle').click();
    await this.page.waitForSelector('[data-testid="workspace-dropdown"]', { timeout: 5000 });
  }

  /** Select a workspace from the dropdown by its display name. */
  async selectWorkspace(name: string): Promise<void> {
    await this.page.getByRole('button', { name }).click();
  }

  /** Confirm the workspace switch in the confirmation modal. */
  async confirmWorkspaceSwitch(): Promise<void> {
    await this.page.getByTestId('workspace-switch-confirm').click();
    await this.page.waitForLoadState('networkidle');
  }

  /** Get the name of the currently active workspace. */
  async getCurrentWorkspaceName(): Promise<string> {
    return (await this.page.getByTestId('workspace-current-name').textContent()) ?? '';
  }
}

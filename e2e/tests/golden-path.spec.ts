import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { ChatPage } from '../pages/chat.page';
import { TicketsPage } from '../pages/tickets.page';
import { AgentDashboardPage } from '../pages/agent-dashboard.page';
import { KcsDraftsPage } from '../pages/kcs-drafts.page';

/**
 * Golden Path — the critical happy path through the entire Shift-Left
 * Knowledge Hub application.
 *
 * Flow across three browser contexts:
 *   1. User logs in → sends AI query → escalates to agent
 *   2. Agent views tickets → resolves → flags KCS gap
 *   3. Admin verifies KCS draft was created
 *
 * Three separate browser contexts simulate distinct user roles without
 * sharing authenticated state. The setup project creates storage state
 * files (.auth/user.json, .auth/agent.json, .auth/admin.json) that are
 * loaded automatically per project configuration.
 */
test.describe('Golden Path', () => {
  let loginPage: LoginPage;
  let chatPage: ChatPage;
  let ticketsPage: TicketsPage;
  let agentDashboardPage: AgentDashboardPage;
  let kcsDraftsPage: KcsDraftsPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    chatPage = new ChatPage(page);
    ticketsPage = new TicketsPage(page);
    agentDashboardPage = new AgentDashboardPage(page);
    kcsDraftsPage = new KcsDraftsPage(page);
  });

  test('User: login → AI query → escalate to agent', async ({ page }) => {
    // 1. Login as user (pre-authenticated via storageState from setup project)
    await test.step('Navigate to chat as authenticated user', async () => {
      await page.goto('/chat');
      await page.waitForLoadState('networkidle');
      // Verify authenticated by checking for the logout button
      await expect(page.getByTestId('nav-logout')).toBeVisible();
    });

    // 2. Send an AI query
    await test.step('Send AI query', async () => {
      await chatPage.sendMessage('How do I reset my network password?');
    });

    // 3. Wait for AI response
    await test.step('Wait for AI response', async () => {
      const hasResponse = await chatPage.waitForResponse(30000);
      expect(hasResponse).toBe(true);
    });

    // 4. Escalate to agent (if response didn't solve it)
    await test.step('Escalate to human agent', async () => {
      if (await chatPage.isFallbackVisible()) {
        await chatPage.clickEscalate();
      } else {
        // Click "Did this solve?" → No → triggers escalate option
        await chatPage.clickDidNotSolve();
        // Wait a moment for the escalation flow to appear
        await page.waitForTimeout(1000);
        if (await chatPage.isFallbackVisible()) {
          await chatPage.clickEscalate();
        }
      }

      // Fill escalation form
      await chatPage.submitEscalationForm('NETWORK', 'HIGH');
    });

    // 5. Verify ticket confirmation
    await test.step('Verify ticket creation', async () => {
      const ticketNumber = await chatPage.getConfirmationTicketNumber();
      expect(ticketNumber).toMatch(/TKT-\d{4}/);
    });
  });

  test('Agent: view tickets → resolve → flag KCS gap', async ({ browser }) => {
    // Use a second browser context for the agent (loaded from .auth/agent.json)
    const agentContext = await browser.newContext({
      storageState: '.auth/agent.json',
    });
    const agentPage = await agentContext.newPage();
    agentDashboardPage = new AgentDashboardPage(agentPage);

    await test.step('Navigate to agent dashboard', async () => {
      await agentDashboardPage.goto();
    });

    await test.step('Claim first ticket', async () => {
      await agentDashboardPage.waitForTickets();
      const ticketCount = await agentDashboardPage.getTicketCount();
      expect(ticketCount).toBeGreaterThan(0);
      await agentDashboardPage.claimFirstTicket();
    });

    await test.step('Resolve with KCS gap flag', async () => {
      await agentDashboardPage.fillResolutionNotes(
        'User was able to reset password via self-service portal. Updated documentation.',
      );
      await agentDashboardPage.clickKnowledgeGapCheckbox();
      await agentDashboardPage.clickResolve();
    });

    await test.step('Verify resolved status', async () => {
      const status = await agentDashboardPage.getTicketStatus();
      expect(status).toContain('Resolved');
    });

    await agentContext.close();
  });

  test('Admin: verify KCS draft created', async ({ browser }) => {
    // Create a third browser context for the admin (loaded from .auth/admin.json)
    const adminContext = await browser.newContext({
      storageState: '.auth/admin.json',
    });
    const adminPage = await adminContext.newPage();
    kcsDraftsPage = new KcsDraftsPage(adminPage);

    await test.step('Navigate to KCS drafts', async () => {
      await kcsDraftsPage.goto();
    });

    await test.step('Verify at least one draft exists', async () => {
      await kcsDraftsPage.expectDraftExists();
      const draftTitle = await kcsDraftsPage.getFirstDraftTitle();
      expect(draftTitle).toBeTruthy();
    });

    await test.step('Verify pending badge shows count', async () => {
      const pendingCount = await kcsDraftsPage.getPendingBadgeCount();
      expect(pendingCount).toBeGreaterThan(0);
    });

    await adminContext.close();
  });
});

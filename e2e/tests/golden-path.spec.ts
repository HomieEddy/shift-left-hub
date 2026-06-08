import { test, expect, Page } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { KnowledgeBasePage } from '../pages/knowledge-base.page';
import { ChatPage } from '../pages/chat.page';
import { TicketsPage } from '../pages/tickets.page';
import { AgentDashboardPage } from '../pages/agent-dashboard.page';

/**
 * Golden Path — the critical happy path through the entire Shift-Left
 * Knowledge Hub application.
 *
 * Flow:
 *   1. User logs in
 *   2. User searches the Knowledge Base
 *   3. User opens an article
 *   4. User navigates to Chat and sends a query
 *   5. User escalates the issue to a human agent (triggering fallback)
 *   6. Agent (admin role) logs in via a second browser context
 *   7. Agent claims and resolves the ticket
 *
 * This test uses two separate browser contexts to simulate both
 * the User and Agent roles without sharing authenticated state.
 */

test.describe('Golden Path', () => {
  let userPage: Page;
  let agentPage: Page;

  test.beforeAll(async ({ browser }) => {
    // Create two isolated browser contexts
    // User context — authenticated as a regular user
    const userContext = await browser.newContext({
      storageState: '.auth/user.json',
    });
    userPage = await userContext.newPage();

    // Agent context — authenticated as an agent/admin
    // We re-login here because the storageState from setup is for the user role.
    // The agent login happens inline in the test flow.
    const agentContext = await browser.newContext();
    agentPage = await agentContext.newPage();
  });

  test('login → search article → chat → escalate → agent resolves', async () => {
    // ─────────────────────────────────────────────────
    // 1. User logs in (pre-authenticated via storageState)
    // ─────────────────────────────────────────────────
    const loginPage = new LoginPage(userPage);
    await test.step('User logs in', async () => {
      // The user is already authenticated from the setup project's storageState.
      // Verify by navigating to /articles and checking for the logout button.
      await userPage.goto('/articles');
      await userPage.waitForLoadState('networkidle');
      await expect(userPage.getByTestId('nav-logout')).toBeVisible();
    });

    // ─────────────────────────────────────────────────
    // 2. User searches the Knowledge Base
    // ─────────────────────────────────────────────────
    const kbPage = new KnowledgeBasePage(userPage);
    let hasResults = false;

    await test.step('User searches Knowledge Base', async () => {
      await kbPage.search('login');
      const resultCount = await kbPage.searchResults.count();
      hasResults = resultCount > 0;
      // If results exist, the search is functional
      if (hasResults) {
        console.log(`Found ${resultCount} search results for "login"`);
      }
    });

    // ─────────────────────────────────────────────────
    // 3. User opens an article (if results exist)
    // ─────────────────────────────────────────────────
    await test.step('User opens an article', async () => {
      if (hasResults) {
        await kbPage.openArticle(0);
        // Verify we're on an article page
        await expect(kbPage.articleViewer).toBeVisible({ timeout: 10000 });
        // Navigate back to chat for the next flow step
        await userPage.goto('/chat');
        await userPage.waitForLoadState('networkidle');
      } else {
        // No results — navigate directly to chat
        await userPage.goto('/chat');
        await userPage.waitForLoadState('networkidle');
      }
    });

    // ─────────────────────────────────────────────────
    // 4. User sends a chat message
    // ─────────────────────────────────────────────────
    const chatPage = new ChatPage(userPage);

    await test.step('User sends chat query', async () => {
      await chatPage.sendMessage('I cannot log in to the VPN');
      // Wait for the AI to respond
      await chatPage.waitForResponse(45000);
      // Verify the chat contains the user's message
      await expect(userPage.getByText('I cannot log in to the VPN')).toBeVisible();
    });

    // ─────────────────────────────────────────────────
    // 5. User escalates to a human agent
    // ─────────────────────────────────────────────────
    const ticketsPage = new TicketsPage(userPage);

    await test.step('User escalates to human agent', async () => {
      // Check if the fallback/escalate button appeared
      const escalateVisible = await chatPage.escalateButton.isVisible().catch(() => false);

      if (escalateVisible) {
        // Click escalate — the fallback section appeared
        await chatPage.escalate();
      } else {
        // If no fallback appeared, we may need to trigger it.
        // Sometimes AI answers successfully — force escalate by refreshing
        // or navigating. The app shows an escalate option when the AI can't
        // resolve the issue. We navigate directly to the escalate context.
        await userPage.goto('/chat');
        await userPage.waitForLoadState('networkidle');
        // Try sending a message that should trigger fallback
        await chatPage.sendMessage('This did not help, I need a human agent');
        await chatPage.waitForResponse(45000);
        // Now try escalate again
        if (await chatPage.escalateButton.isVisible().catch(() => false)) {
          await chatPage.escalate();
        }
      }

      // Fill in the escalation form
      const escalationFormVisible = await ticketsPage.escalationForm.isVisible().catch(() => false);
      expect(escalationFormVisible).toBeTruthy();
      await ticketsPage.createEscalation(
        'VPN login not working after upgrade',
        'NETWORK',
        'HIGH',
      );
      await expect(userPage.getByText(/Ticket created|Ticket Submitted/)).toBeVisible();
    });

    // ─────────────────────────────────────────────────
    // 6. Agent logs in and claims the ticket
    // ─────────────────────────────────────────────────
    const agentLogin = new LoginPage(agentPage);
    const agentDashboard = new AgentDashboardPage(agentPage);

    await test.step('Agent logs in', async () => {
      const agentEmail = process.env.E2E_AGENT_EMAIL ?? 'admin@shiftleft.com';
      const agentPassword = process.env.E2E_AGENT_PASSWORD ?? 'ShiftLeft!2026';
      await agentLogin.goto();
      await agentLogin.login(agentEmail, agentPassword);
      // Verify agent is on a role-restricted page
      await expect(agentPage.getByText('Logout')).toBeVisible();
    });

    await test.step('Agent claims the ticket', async () => {
      await agentDashboard.gotoQueue();
      // Wait for the ticket queue to load
      await agentPage.waitForLoadState('networkidle');

      const queueCount = await agentDashboard.ticketQueue.count();
      expect(queueCount).toBeGreaterThan(0);

      // Claim the first ticket
      await agentDashboard.claimTicket();
      // Verify we're on a ticket detail page
      await expect(agentPage.getByText(/Ticket #|Back to Queue/)).toBeVisible();
    });

    // ─────────────────────────────────────────────────
    // 7. Agent resolves the ticket
    // ─────────────────────────────────────────────────
    await test.step('Agent resolves the ticket', async () => {
      await agentDashboard.resolveTicket(
        'VPN credentials were reset. User can now log in successfully.',
        true, // Flag as knowledge gap for KCS
      );
      // Verify the resolved state is visible
      await expect(agentPage.getByTestId('ticket-resolved')).toBeVisible({ timeout: 10000 });
      // Verify the knowledge gap flag is shown
      await expect(agentPage.getByText(/Knowledge Gap|flagged/)).toBeVisible();
    });
  });
});

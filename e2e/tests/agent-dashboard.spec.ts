import { test, expect } from '@playwright/test';
import { AgentDashboardPage } from '../pages/agent-dashboard.page';

test.describe('Agent Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.request.post('/api/tickets', {
      data: { issue: 'E2E test ticket - agent dashboard', category: 'SOFTWARE', urgency: 'MEDIUM' },
    });
  });

  test('View agent queue and claim a ticket', async ({ page }) => {
    const agentDashboard = new AgentDashboardPage(page);
    await agentDashboard.gotoQueue();
    await agentDashboard.waitForTickets();

    const count = await agentDashboard.getTicketCount();
    expect(count).toBeGreaterThan(0);

    await agentDashboard.claimFirstTicket();
  });

  test('Resolve a ticket and flag for KCS', async ({ page }) => {
    const agentDashboard = new AgentDashboardPage(page);
    await agentDashboard.gotoQueue();
    await agentDashboard.waitForTickets();
    expect(await agentDashboard.getTicketCount()).toBeGreaterThan(0);

    await agentDashboard.claimFirstTicket();

    await agentDashboard.resolveTicket('Resolved via E2E testing - KCS gap flagged', true);

    await expect(page.getByTestId('ticket-resolved')).toBeVisible({ timeout: 10000 });
    const status = await agentDashboard.getTicketStatus();
    expect(status).toContain('Resolved');
  });
});

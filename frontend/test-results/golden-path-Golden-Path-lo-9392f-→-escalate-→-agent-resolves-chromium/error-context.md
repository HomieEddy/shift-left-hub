# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: golden-path.spec.ts >> Golden Path >> login → search article → chat → escalate → agent resolves
- Location: ..\e2e\tests\golden-path.spec.ts:44:7

# Error details

```
Error: expect(received).toBeTruthy()

Received: false
```

# Page snapshot

```yaml
- generic [ref=e2]:
  - banner [ref=e3]:
    - generic [ref=e5]:
      - generic [ref=e6]:
        - link "SL Knowledge Hub" [ref=e7] [cursor=pointer]:
          - /url: /
        - navigation [ref=e8]:
          - link "AI Assistant" [ref=e9] [cursor=pointer]:
            - /url: /chat
          - link "My Tickets" [ref=e10] [cursor=pointer]:
            - /url: /tickets
          - link "Knowledge Base" [ref=e11] [cursor=pointer]:
            - /url: /articles
      - generic [ref=e12]:
        - generic [ref=e13]:
          - button "EN" [ref=e14]
          - button "FR" [ref=e15]
        - generic [ref=e16]: Regular User
        - button "Logout" [ref=e17]
  - main [ref=e18]:
    - generic [ref=e20]:
      - generic [ref=e21]:
        - heading "AI Assistant" [level=1] [ref=e22]
        - paragraph [ref=e23]: Ask me anything about IT issues
      - generic [ref=e26]: This did not help, I need a human agent
      - generic [ref=e34]:
        - textbox "Describe your IT issue..." [disabled] [ref=e35]
        - button "Send" [disabled] [ref=e36]
```

# Test source

```ts
  35  |     userPage = await userContext.newPage();
  36  | 
  37  |     // Agent context — authenticated as an agent/admin
  38  |     // We re-login here because the storageState from setup is for the user role.
  39  |     // The agent login happens inline in the test flow.
  40  |     const agentContext = await browser.newContext();
  41  |     agentPage = await agentContext.newPage();
  42  |   });
  43  | 
  44  |   test('login → search article → chat → escalate → agent resolves', async () => {
  45  |     // ─────────────────────────────────────────────────
  46  |     // 1. User logs in (pre-authenticated via storageState)
  47  |     // ─────────────────────────────────────────────────
  48  |     const loginPage = new LoginPage(userPage);
  49  |     await test.step('User logs in', async () => {
  50  |       // The user is already authenticated from the setup project's storageState.
  51  |       // Verify by navigating to /articles and checking for the logout button.
  52  |       await userPage.goto('/articles');
  53  |       await userPage.waitForLoadState('networkidle');
  54  |       await expect(userPage.getByTestId('nav-logout')).toBeVisible();
  55  |     });
  56  | 
  57  |     // ─────────────────────────────────────────────────
  58  |     // 2. User searches the Knowledge Base
  59  |     // ─────────────────────────────────────────────────
  60  |     const kbPage = new KnowledgeBasePage(userPage);
  61  |     let hasResults = false;
  62  | 
  63  |     await test.step('User searches Knowledge Base', async () => {
  64  |       await kbPage.goto();
  65  |       await kbPage.search('login');
  66  |       const resultCount = await kbPage.searchResults.count();
  67  |       hasResults = resultCount > 0;
  68  |       // If results exist, the search is functional
  69  |       if (hasResults) {
  70  |         console.log(`Found ${resultCount} search results for "login"`);
  71  |       }
  72  |     });
  73  | 
  74  |     // ─────────────────────────────────────────────────
  75  |     // 3. User opens an article (if results exist)
  76  |     // ─────────────────────────────────────────────────
  77  |     await test.step('User opens an article', async () => {
  78  |       if (hasResults) {
  79  |         await kbPage.openArticle(0);
  80  |         // Verify we're on an article page
  81  |         await expect(kbPage.articleViewer).toBeVisible({ timeout: 10000 });
  82  |         // Navigate back to chat for the next flow step
  83  |         await userPage.goto('/chat');
  84  |         await userPage.waitForLoadState('networkidle');
  85  |       } else {
  86  |         // No results — navigate directly to chat
  87  |         await userPage.goto('/chat');
  88  |         await userPage.waitForLoadState('networkidle');
  89  |       }
  90  |     });
  91  | 
  92  |     // ─────────────────────────────────────────────────
  93  |     // 4. User sends a chat message
  94  |     // ─────────────────────────────────────────────────
  95  |     const chatPage = new ChatPage(userPage);
  96  | 
  97  |     await test.step('User sends chat query', async () => {
  98  |       await chatPage.sendMessage('I cannot log in to the VPN');
  99  |       // Wait for the AI to respond
  100 |       await chatPage.waitForResponse(45000);
  101 |       // Verify the chat contains the user's message
  102 |       await expect(userPage.getByText('I cannot log in to the VPN')).toBeVisible();
  103 |     });
  104 | 
  105 |     // ─────────────────────────────────────────────────
  106 |     // 5. User escalates to a human agent
  107 |     // ─────────────────────────────────────────────────
  108 |     const ticketsPage = new TicketsPage(userPage);
  109 | 
  110 |     await test.step('User escalates to human agent', async () => {
  111 |       // Check if the fallback/escalate button appeared
  112 |       const escalateVisible = await chatPage.escalateButton.isVisible().catch(() => false);
  113 | 
  114 |       if (escalateVisible) {
  115 |         // Click escalate — the fallback section appeared
  116 |         await chatPage.escalate();
  117 |       } else {
  118 |         // If no fallback appeared, we may need to trigger it.
  119 |         // Sometimes AI answers successfully — force escalate by refreshing
  120 |         // or navigating. The app shows an escalate option when the AI can't
  121 |         // resolve the issue. We navigate directly to the escalate context.
  122 |         await userPage.goto('/chat');
  123 |         await userPage.waitForLoadState('networkidle');
  124 |         // Try sending a message that should trigger fallback
  125 |         await chatPage.sendMessage('This did not help, I need a human agent');
  126 |         await chatPage.waitForResponse(45000);
  127 |         // Now try escalate again
  128 |         if (await chatPage.escalateButton.isVisible().catch(() => false)) {
  129 |           await chatPage.escalate();
  130 |         }
  131 |       }
  132 | 
  133 |       // Fill in the escalation form
  134 |       const escalationFormVisible = await ticketsPage.escalationForm.isVisible().catch(() => false);
> 135 |       expect(escalationFormVisible).toBeTruthy();
      |                                     ^ Error: expect(received).toBeTruthy()
  136 |       await ticketsPage.createEscalation(
  137 |         'VPN login not working after upgrade',
  138 |         'NETWORK',
  139 |         'HIGH',
  140 |       );
  141 |       await expect(userPage.getByText(/Ticket created|Ticket Submitted/)).toBeVisible();
  142 |     });
  143 | 
  144 |     // ─────────────────────────────────────────────────
  145 |     // 6. Agent logs in and claims the ticket
  146 |     // ─────────────────────────────────────────────────
  147 |     const agentLogin = new LoginPage(agentPage);
  148 |     const agentDashboard = new AgentDashboardPage(agentPage);
  149 | 
  150 |     await test.step('Agent logs in', async () => {
  151 |       const agentEmail = process.env.E2E_AGENT_EMAIL ?? 'admin@shiftleft.com';
  152 |       const agentPassword = process.env.E2E_AGENT_PASSWORD ?? 'ShiftLeft!2026';
  153 |       await agentLogin.goto();
  154 |       await agentLogin.login(agentEmail, agentPassword);
  155 |       // Verify agent is on a role-restricted page
  156 |       await expect(agentPage.getByText('Logout')).toBeVisible();
  157 |     });
  158 | 
  159 |     await test.step('Agent claims the ticket', async () => {
  160 |       await agentDashboard.gotoQueue();
  161 |       // Wait for the ticket queue to load
  162 |       await agentPage.waitForLoadState('networkidle');
  163 | 
  164 |       const queueCount = await agentDashboard.ticketQueue.count();
  165 |       expect(queueCount).toBeGreaterThan(0);
  166 | 
  167 |       // Claim the first ticket
  168 |       await agentDashboard.claimTicket();
  169 |       // Verify we're on a ticket detail page
  170 |       await expect(agentPage.getByText(/Ticket #|Back to Queue/)).toBeVisible();
  171 |     });
  172 | 
  173 |     // ─────────────────────────────────────────────────
  174 |     // 7. Agent resolves the ticket
  175 |     // ─────────────────────────────────────────────────
  176 |     await test.step('Agent resolves the ticket', async () => {
  177 |       await agentDashboard.resolveTicket(
  178 |         'VPN credentials were reset. User can now log in successfully.',
  179 |         true, // Flag as knowledge gap for KCS
  180 |       );
  181 |       // Verify the resolved state is visible
  182 |       await expect(agentPage.getByTestId('ticket-resolved')).toBeVisible({ timeout: 10000 });
  183 |       // Verify the knowledge gap flag is shown
  184 |       await expect(agentPage.getByText(/Knowledge Gap|flagged/)).toBeVisible();
  185 |     });
  186 |   });
  187 | });
  188 | 
```
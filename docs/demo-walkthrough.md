# Shift-Left Knowledge Hub — Demo Walkthrough

## Prerequisites

- Docker Compose running: `docker compose up`
- Demo data seeded (admin user + demo articles via DataSeeder/KbSeeder)
- Browser: Chrome (for best Playwright compatibility)

## 1. Login as User

1. Navigate to `http://localhost:4200`
2. Click **Login** in the nav bar
3. Enter email: `user@shiftleft.com`
4. Enter password: `ShiftLeft!2026`
5. Click **Sign In**
6. **Verify:** Landing page shows in English with welcome message

## 2. Browse Knowledge Base

1. Click **Knowledge Base** in the nav bar
2. Type a search query (e.g., "password", "VPN", "email")
3. **Verify:** Search results appear with highlighted snippets
4. Click on an article result
5. **Verify:** Full article content renders with markdown formatting
6. Switch language to **Français** using the language switcher
7. **Verify:** Article title and content display in French

## 3. AI Chat

1. Click **AI Assistant** in the nav bar
2. Type: "How do I reset my password?"
3. Press Enter or click Send
4. **Verify:** AI response streams in real-time with typing indicator
5. After response completes, click **No** on "Did this solve your problem?"

## 4. Escalate to Agent

1. Click **Escalate to Agent**
2. Category: Select `SOFTWARE`
3. Urgency: Select `HIGH`
4. Click **Submit**
5. **Verify:** Confirmation message shows with ticket number (e.g., TKT-0001)
6. Note the ticket number for Step 5

## 5. Agent Dashboard (Login as Admin)

1. Log out (click profile → Logout)
2. Log in with admin credentials:
   - Email: `admin@shiftleft.com`
   - Password: `ShiftLeft!2026`
3. Click **Ticket Queue** in the nav bar
4. **Verify:** Ticket list shows the ticket from Step 4
5. Click **Claim** on the ticket
6. Open the ticket detail

## 6. Resolve Ticket

1. In ticket detail, view the **Shift-Left Context** section
2. **Verify:** Full AI chat transcript is visible
3. Add a work note (e.g., "Reset password guide provided via email")
4. Fill resolution notes: "Advised user on password reset procedure"
5. Check **Flag as Knowledge Gap**
6. Click **Resolve**
7. **Verify:** Ticket status changes to RESOLVED

## 7. KCS Draft Review (Admin)

1. Navigate to **Admin** → **KCS Drafts**
2. **Verify:** Auto-drafted article appears from Step 6's Knowledge Gap
3. Review the AI-generated content (title, overview, steps)
4. Click **Approve** to publish the article
5. Switch to **Knowledge Base**
6. **Verify:** New article appears in the published listing

## 8. Language Switch Verification

1. Switch application to French using the language switcher
2. Navigate through all pages:
   - Landing page
   - Login page
   - Knowledge Base
   - AI Assistant
   - Tickets
   - Agent Dashboard
   - Admin pages
3. **Verify:** No layout overflow or text truncation in any view
4. **Verify:** All navigation labels, buttons, and form labels are in French
5. Switch back to English
6. **Verify:** All labels return to English

## Summary

| Step | Feature | Verification Point |
|------|---------|-------------------|
| 1 | Login | User authenticates successfully |
| 2 | Knowledge Base | FTS search returns results, bilingual content renders |
| 3 | AI Chat | SSE streaming works, feedback mechanism functions |
| 4 | Escalation | Ticket created with full context |
| 5 | Agent Dashboard | Agent can view and claim tickets |
| 6 | Resolution | Agent resolves ticket with notes + KCS flag |
| 7 | KCS Draft | AI auto-drafts article, admin approves |
| 8 | i18n | Bilingual UI renders without overflow |

## Troubleshooting

- **Docker not starting:** Ensure Docker Desktop is running and ports 5433/8082/4200 are free
- **AI responses failing:** Verify Ollama is running (`ollama serve`) and the model is pulled
- **No seeded data:** Check logs for DataSeeder/KbSeeder execution. Set `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD`
- **Playwright tests fail:** Run `npx playwright install` to ensure browsers are installed

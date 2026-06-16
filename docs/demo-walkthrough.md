# Shift-Left Knowledge Hub — Demo Walkthrough

## Production Verification

After deploying to production, use these URLs instead of localhost:

- **Frontend:** `https://shift-left-hub.vercel.app`
- **Backend Health:** `https://shift-left-hub-backend-production.up.railway.app/actuator/health`

All steps below use `https://shift-left-hub.vercel.app` as the base URL instead of `http://localhost:4200`.

## Prerequisites

### Local Development
- Docker Compose running: `docker compose up`
- Java 21 + Maven for backend: `cd backend && ./mvnw spring-boot:run`
- Node.js 22+ for frontend: `cd frontend && pnpm install && pnpm start`
- Browser: Chrome (for best Playwright compatibility)

### Production Verification
- Frontend deployed on Vercel: `https://shift-left-hub.vercel.app`
- Backend deployed on Railway: `https://shift-left-hub-backend-production.up.railway.app`
- Both services have built and deployed from the latest `master` commit

## 1. Login as User

1. Navigate to `http://localhost:4200`
2. Click **Login** in the nav bar
3. Enter email: `user@shiftleft.com`
4. Enter password: `ShiftLeft!2026`
5. Click **Sign In**
6. **Verify:** Landing page shows with neutral "Knowledge Hub" branding

## 2. Browse Knowledge Base

1. Click **Knowledge Base** in the nav bar
2. Type a search query (e.g., "password", "vacation", "policy")
3. **Verify:** Search results appear with highlighted snippets and category badges
4. Click on an article result
5. **Verify:** Full article content renders with markdown formatting
6. Switch language to **Français** using the language switcher
7. **Verify:** Article title and content display in French

## 3. AI Chat

1. Click **Assistant** in the nav bar
2. Type: "How do I reset my password?"
3. Press Enter or click Send
4. **Verify:** AI response streams in real-time with typing indicator
5. **Verify:** Sources are cited in the response (article/document badges with scores)
6. After response completes, click **No** on "Did this solve your problem?"

## 4. Escalate to Agent

1. Click **Escalate to Agent**
2. Category: Select `SOFTWARE`
3. Urgency: Select `HIGH`
4. Click **Submit**
5. **Verify:** Confirmation message shows with ticket number (e.g., TKT-0001)
6. Note the ticket number for Step 6

## 5. Switch Workspace (v2.0 Feature)

1. Click the workspace switcher in the nav bar (shows current workspace name)
2. Select a different workspace from the dropdown
3. **Verify:** UI reloads for the selected workspace context
4. **Verify:** Knowledge Base now shows articles for the selected workspace
5. Switch back to the original workspace

## 6. Agent Dashboard (Login as Admin)

1. Log out (click profile → Logout)
2. Log in with admin credentials:
   - Email: `admin@shiftleft.com`
   - Password: `ShiftLeft!2026`
3. Click **Ticket Queue** in the nav bar
4. **Verify:** Ticket list shows the ticket from Step 4
5. Click **Claim** on the ticket
6. Open the ticket detail

## 7. Resolve Ticket

1. In ticket detail, view the **Shift-Left Context** section
2. **Verify:** Full AI chat transcript is visible
3. Add a work note (e.g., "Reset password guide provided via email")
4. Fill resolution notes: "Advised user on password reset procedure"
5. Check **Flag as Knowledge Gap**
6. Click **Resolve**
7. **Verify:** Ticket status changes to RESOLVED

## 8. KCS Draft Review (Admin)

1. Navigate to **Admin** → **KCS Drafts**
2. **Verify:** Auto-drafted article appears from Step 7's Knowledge Gap
3. Review the AI-generated content (title, overview, steps)
4. Click **Approve** to publish the article
5. Switch to **Knowledge Base**
6. **Verify:** New article appears in the published listing

## 9. Language Switch Verification

1. Switch application to French using the language switcher
2. Navigate through all pages:
   - Landing page
   - Login page
   - Knowledge Base
   - Assistant
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
| 2 | Knowledge Base | FTS + category filter search, bilingual content |
| 3 | AI Chat | SSE streaming, source citations, feedback mechanism |
| 4 | Escalation | Ticket created with full context |
| 5 | Workspace Switcher | Context switching between workspaces |
| 6 | Agent Dashboard | Agent can view and claim tickets |
| 7 | Resolution | Agent resolves with notes + KCS flag |
| 8 | KCS Draft | AI auto-drafts article, admin approves |
| 9 | i18n | Bilingual UI renders without overflow |

## Troubleshooting

- **Docker not starting:** Ensure Docker Desktop is running and ports 5432/8080/4200 are free
- **AI responses failing:** Verify OpenAI API key is configured or Ollama is running (`ollama serve`)
- **No seeded data:** Check backend logs for seeder execution
- **Workspace switcher empty:** Ensure seed data was created (4 workspaces: HR, Legal, IT, Public)
- **Playwright tests fail:** Run `npx playwright install` to ensure browsers are installed

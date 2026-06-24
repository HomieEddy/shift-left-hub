# Pre-Deploy Checklist — Phase 21 (v2.1)

> **Updated:** 2026-06-23 — v2.2.
> Backend Spring Boot 4.0.6 (not 3.5). Actuator exposes `health`, `info`,
> `prometheus`, `metrics` (v2.2 tier 17). Frontend build output is
> `dist/shiftleft-hub/browser` (Angular 17+ application builder).

## Prerequisites

- [ ] You have a GitHub account with push access to this repo
- [ ] Branch protection on `master` requires CI to pass before merge
- [ ] All CI pipeline jobs pass on your latest commit (run `git push` and check GitHub Actions)

## 1. Create Railway Project (Backend + Database)

1. Go to https://railway.app and sign in (or create account)
2. Click **New Project** → **Deploy from GitHub repo**
3. Select this repository
4. Configure the service:
   - **Root Directory:** `backend`
   - **Build Command:** (leave default — Railway auto-detects Dockerfile)
   - **Start Command:** (leave empty — Railway uses CMD from Dockerfile)
5. Railway detects `backend/Dockerfile` and builds automatically
6. Verify first build succeeds (watch Railway dashboard)

### 1b. Add PostgreSQL Database

1. In the Railway project dashboard, click **New** → **Database** → **Add PostgreSQL**
2. Railway provisions a PostgreSQL 16 instance
3. Copy the `DATABASE_URL` connection string from the PostgreSQL service's **Connect** tab
4. Format it as a JDBC URL: `jdbc:postgresql://[host]:[port]/[dbname]?sslmode=require`
   - Extract host, port, dbname, user, password from the `DATABASE_URL`
   - The standard Railway DATABASE_URL format is: `postgresql://[user]:[password]@[host]:[port]/[dbname]`

## 2. Configure Railway Environment Variables (Secrets)

In the Railway backend service dashboard, add these environment variables:

| Variable | Value | Notes |
|----------|-------|-------|
| `PORT` | `8080` | Matches Dockerfile EXPOSE |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...` | JDBC URL from step 1b |
| `SPRING_DATASOURCE_USERNAME` | (from DATABASE_URL) | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | (from DATABASE_URL) | PostgreSQL password |
| `APP_JWT_SECRET` | Generate a 256-bit random secret | `openssl rand -base64 32` |
| `APP_ADMIN_EMAIL` | `admin@shiftleft.com` | Admin seed login |
| `APP_ADMIN_PASSWORD` | Choose a strong password | Admin seed password |
| `APP_AI_ENCRYPTION_KEY` | Generate a random key | `openssl rand -base64 32` |
| `APP_AI_ENCRYPTION_SALT` | Generate a random salt | `openssl rand -base64 16` |
| `SPRING_AI_OPENAI_API_KEY` | Your OpenAI API key | Or Ollama-compatible endpoint |
| `SPRING_PROFILES_ACTIVE` | `docker` | Enables Docker profile config |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `none` | Flyway manages schema (use `validate` after first successful deploy) |
| `SPRING_FLYWAY_ENABLED` | `true` | Enables migrations |
| `APP_AUTH_COOKIE_SECURE` | `true` | Required for HTTPS production cookies |
| `APP_AUTH_COOKIE_SAME_SITE` | `Lax` | API traffic is same-origin through Vercel rewrites |
| `APP_CORS_ALLOWED_ORIGINS` | `https://[your-vercel-app].vercel.app` | Explicit trusted frontend origins only; use comma-separated values for multiple origins, no wildcards |
| `APP_EMBEDDING_PROVIDER` | `OPENAI_COMPATIBLE` | Must match embedding API format |
| `APP_EMBEDDING_ENDPOINT_URL` | `https://api.voyageai.com/v1/` | Your embedding provider base URL |
| `APP_EMBEDDING_API_KEY` | Your Voyage AI key | Stored in env var, never in DB |
| `APP_EMBEDDING_MODEL` | `voyage-4-lite` | 1024-dimensional embeddings |
| `SPRING_AI_VECTORSTORE_PGVECTOR_DIMENSIONS` | `1024` | Must match embedding model dimension |

Note the Railway app URL: `https://[your-railway-app].up.railway.app`
You'll need this for `frontend/vercel.json` and CORS config.

## 3. Create Vercel Project (Frontend)

1. Go to https://vercel.com and sign in (or create account)
2. Click **Add New** → **Project**
3. Import your GitHub repository
4. Configure:
   - **Root Directory:** `frontend`
   - **Framework Preset:** Angular (Vercel auto-detects)
   - **Build Command:** `pnpm build`
   - **Output Directory:** `dist/shiftleft-hub/browser`
5. Note the Vercel app URL: `https://[your-vercel-app].vercel.app`

## 4. Set Vercel Environment Variables

In the Vercel project dashboard → **Settings** → **Environment Variables**:

| Variable | Value | Environment |
|----------|-------|-------------|
| (No backend-related env vars needed) | | |

Vercel builds the Angular SPA as static files. Runtime API calls use same-origin `/api` URLs and `frontend/vercel.json` rewrites them to Railway.

## 5. Verify Vercel API Rewrite (One-Time Initial Setup)

Perform this step **once** before the first Vercel deploy. Vercel does not reload `vercel.json` on subsequent deploys — the config is static after the initial build.

1. Edit `frontend/vercel.json` and set the `/api/:path*` rewrite destination to `https://[your-railway-app].up.railway.app/api/:path*`
2. Keep `frontend/public/env.js` set to `apiBaseUrl = ''`
3. Commit and push to `master`

If the Railway app URL ever changes (e.g., app is recreated), you must repeat this step to update the hardcoded destination in `vercel.json`.

## 6. Set CORS Origin (if Railway deployed before Vercel)

If Railway deployed before Vercel, update the Railway env var:
- `APP_CORS_ALLOWED_ORIGINS` → set to `https://[your-vercel-app].vercel.app`
Railway will auto-redeploy when env vars change.

## 7. Final Checks

- [ ] Backend builds and deploys successfully on Railway (check Railway dashboard logs)
- [ ] Database migration runs (look for Flyway migration logs in Railway output)
- [ ] Seeders execute (look for "Seeding..." log messages in Railway output)
- [ ] Frontend builds and deploys successfully on Vercel (check Vercel dashboard)
- [ ] Backend health endpoint responds: `https://[railway-app].up.railway.app/actuator/health`
- [ ] Frontend loads in browser: `https://[vercel-app].vercel.app`
- [ ] CORS is working (frontend can call backend — open browser dev console, no CORS errors)

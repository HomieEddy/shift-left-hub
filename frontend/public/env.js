// Runtime environment configuration for Shift-Left Knowledge Hub.
// Keep empty so the browser calls same-origin /api routes.
// Local development uses the Angular dev proxy; Vercel rewrites /api to Railway.
(window.__env || (window.__env = {})).apiBaseUrl = '';

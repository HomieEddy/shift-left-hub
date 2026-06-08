import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for Shift-Left Knowledge Hub E2E tests.
 * Timeouts adjusted for Windows compatibility (45000ms base).
 * Uses data-testid as the primary selector strategy.
 */
export default defineConfig({
  testDir: './e2e/tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,

  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['list'],
  ],

  timeout: 45000,
  expect: {
    timeout: 10000,
  },

  use: {
    baseURL: 'http://localhost:4200',
    testIdAttribute: 'data-testid',
    screenshot: 'only-on-failure',
    trace: 'on-first-retry',
    actionTimeout: 10000,
    navigationTimeout: 30000,
  },

  projects: [
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
    },
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/user.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'agent-chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/agent.json',
      },
      dependencies: ['setup'],
    },
    {
      name: 'admin-chromium',
      use: {
        ...devices['Desktop Chrome'],
        storageState: '.auth/admin.json',
      },
      dependencies: ['setup'],
    },
  ],
});

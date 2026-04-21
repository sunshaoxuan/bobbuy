import { defineConfig } from '@playwright/test';

const isCI = !!process.env.CI;

export default defineConfig({
  testDir: './e2e',
  timeout: isCI ? 90_000 : 60_000,
  expect: {
    timeout: isCI ? 15_000 : 10_000
  },
  retries: isCI ? 2 : 0,
  workers: isCI ? 1 : 2,
  fullyParallel: false,
  use: {
    baseURL: 'http://127.0.0.1:5173',
    actionTimeout: isCI ? 20_000 : 10_000,
    navigationTimeout: isCI ? 45_000 : 30_000,
    trace: 'retain-on-failure'
  },
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5173',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: !isCI,
    timeout: isCI ? 180_000 : 120_000
  }
});

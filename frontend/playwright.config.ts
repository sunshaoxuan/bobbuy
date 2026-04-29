import { defineConfig } from '@playwright/test';

const isCI = !!process.env.CI;
const CI_TEST_TIMEOUT = 90_000;
const LOCAL_TEST_TIMEOUT = 60_000;
const CI_EXPECT_TIMEOUT = 15_000;
const LOCAL_EXPECT_TIMEOUT = 10_000;
const CI_ACTION_TIMEOUT = 20_000;
const LOCAL_ACTION_TIMEOUT = 10_000;
const CI_NAVIGATION_TIMEOUT = 45_000;
const LOCAL_NAVIGATION_TIMEOUT = 30_000;
const CI_WEB_SERVER_TIMEOUT = 180_000;
const LOCAL_WEB_SERVER_TIMEOUT = 120_000;

export default defineConfig({
  testDir: './e2e',
  reporter: [['list'], ['html', { open: 'never' }]],
  timeout: isCI ? CI_TEST_TIMEOUT : LOCAL_TEST_TIMEOUT,
  expect: {
    timeout: isCI ? CI_EXPECT_TIMEOUT : LOCAL_EXPECT_TIMEOUT
  },
  retries: isCI ? 2 : 0,
  workers: isCI ? 1 : 2,
  fullyParallel: false,
  use: {
    baseURL: 'http://127.0.0.1:5173',
    actionTimeout: isCI ? CI_ACTION_TIMEOUT : LOCAL_ACTION_TIMEOUT,
    navigationTimeout: isCI ? CI_NAVIGATION_TIMEOUT : LOCAL_NAVIGATION_TIMEOUT,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1 --port 5173',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: !isCI,
    timeout: isCI ? CI_WEB_SERVER_TIMEOUT : LOCAL_WEB_SERVER_TIMEOUT
  }
});

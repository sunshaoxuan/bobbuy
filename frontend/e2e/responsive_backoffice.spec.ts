import { expect, test } from '@playwright/test';
import { assertNoHorizontalOverflow, setAgentContext, setupCommonMocks, VIEWPORTS } from './responsive_helpers';

type BackofficeCase = {
  name: string;
  path: string;
  assertPageReady: (page: import('@playwright/test').Page) => Promise<void>;
  assertActionReachable: (page: import('@playwright/test').Page) => Promise<void>;
};

const BACKOFFICE_CASES: BackofficeCase[] = [
  {
    name: 'Dashboard',
    path: '/dashboard',
    assertPageReady: async (page) => expect(page.locator('[data-testid="dashboard-title"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.getByText('Latest Trips')).toBeVisible()
  },
  {
    name: 'Trips',
    path: '/trips',
    assertPageReady: async (page) => expect(page.locator('[data-testid="trips-title"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.locator('[data-testid="trips-submit"]')).toBeVisible()
  },
  {
    name: 'Users',
    path: '/users',
    assertPageReady: async (page) => expect(page.locator('[data-testid="users-title"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.locator('[data-testid="users-submit"]')).toBeVisible()
  },
  {
    name: 'Orders(back-office)',
    path: '/orders',
    assertPageReady: async (page) => expect(page.locator('[data-testid="orders-title"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.locator('[data-testid="orders-submit"]')).toBeVisible()
  },
  {
    name: 'OrderDesk',
    path: '/order-desk',
    assertPageReady: async (page) => expect(page.locator('[data-testid="orderdesk-root"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.locator('[data-testid="orderdesk-input"]')).toBeVisible()
  }
];

test.describe('Responsive: Back-office matrix pages (core)', () => {
  for (const pageCase of BACKOFFICE_CASES) {
    for (const vp of VIEWPORTS) {
      test(`${pageCase.name} @ ${vp.label}`, async ({ page }) => {
        await page.setViewportSize({ width: vp.width, height: vp.height });
        await setupCommonMocks(page);
        await setAgentContext(page);
        await page.goto(pageCase.path);
        await pageCase.assertPageReady(page);
        await pageCase.assertActionReachable(page);
        await assertNoHorizontalOverflow(page);
      });
    }
  }
});

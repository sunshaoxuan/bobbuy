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
    name: 'ProcurementDashboard',
    path: '/procurement',
    assertPageReady: async (page) => expect(page.locator('[data-testid="procurement-trip-select"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible()
  },
  {
    name: 'StockMaster',
    path: '/stock-master',
    assertPageReady: async (page) => expect(page.locator('[data-testid="stock-master-title"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.locator('[data-testid="stock-master-add-row"]').first()).toBeVisible()
  },
  {
    name: 'PickingMaster',
    path: '/picking',
    assertPageReady: async (page) => expect(page.getByRole('heading', { name: 'Picking Master' })).toBeVisible(),
    assertActionReachable: async (page) => expect(page.locator('[data-testid="picking-trip-select"]')).toBeVisible()
  },
  {
    name: 'ZenAuditView',
    path: '/audit/2000',
    assertPageReady: async (page) => expect(page.locator('[data-testid="zen-audit-title"]')).toBeVisible(),
    assertActionReachable: async (page) => expect(page.locator('[data-testid="zen-audit-operator-filter"]')).toBeVisible()
  }
];

test.describe('Responsive: Back-office matrix pages (ops)', () => {
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

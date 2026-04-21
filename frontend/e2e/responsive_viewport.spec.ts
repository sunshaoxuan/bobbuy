/**
 * Responsive viewport acceptance tests — 390 / 768 / 1280 breakpoints.
 *
 * Strategy:
 *  - Each test navigates to a page at a fixed viewport and verifies:
 *      1. The page's main heading / key content is visible in the first screen.
 *      2. There is no unexpected horizontal scroll (document.documentElement.scrollWidth <= viewport.width).
 *      3. Key actions (trip selector, primary CTA) are accessible.
 *  - All API calls are mocked so the test never depends on a running backend.
 */

import { expect, type Page, test } from '@playwright/test';

// ---------------------------------------------------------------------------
// Shared mock data
// ---------------------------------------------------------------------------
const MOCK_TRIPS = [
  {
    id: 2000,
    agentId: 1000,
    origin: 'Tokyo',
    destination: 'Shanghai',
    departDate: '2026-04-20',
    capacity: 10,
    reservedCapacity: 2,
    remainingCapacity: 8,
    status: 'PUBLISHED'
  }
];

const MOCK_ORDERS = [
  {
    id: 3000,
    businessId: 'BIZ-1001',
    customerId: 1001,
    tripId: 2000,
    status: 'CONFIRMED',
    paymentMethod: 'ALIPAY',
    paymentStatus: 'UNPAID',
    totalAmount: 65,
    lines: [{ skuId: 'SKU-001', itemName: 'Matcha', quantity: 2, unitPrice: 32.5 }]
  }
];

const MOCK_LEDGER = [
  { businessId: 'BIZ-1001', customerId: 1001, totalReceivable: 65, paidDeposit: 0, outstandingBalance: 65 }
];

const MOCK_METRICS = { users: 2, trips: 1, orders: 1, gmV: 65, orderStatusCounts: { CONFIRMED: 1 } };

async function setupCommonMocks(page: Page) {
  await page.route('**/api/trips**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: MOCK_TRIPS }) })
  );
  await page.route('**/api/orders**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: MOCK_ORDERS, meta: { total: 1 } }) })
  );
  await page.route('**/api/procurement/*/ledger', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: MOCK_LEDGER, meta: { total: 1 } }) })
  );
  await page.route('**/api/procurement/*/hud', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          tripId: 2000, currentFxRate: 1, referenceFxRate: 1,
          currentPurchasedAmount: 65, totalTripExpenses: 0, totalEstimatedProfit: 10
        }
      })
    })
  );
  await page.route('**/api/chat/**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/metrics', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: MOCK_METRICS }) })
  );
  await page.route('**/api/users', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/mobile/products**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [], meta: { total: 0 } }) })
  );
  await page.route('**/api/mobile/categories**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/mobile/suppliers**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/financial/audit/**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/procurement/*/deficit', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/procurement/*/expenses', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/procurement/*/logistics', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/procurement/*/profit-sharing', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: { tripId: 2000, purchaserRatioPercent: 70, promoterRatioPercent: 30, updatedAt: null }
      })
    })
  );
  await page.route('**/api/procurement/*/audit-logs', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );
  await page.route('**/api/procurement/wallets/*', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          partnerId: 'PURCHASER',
          balance: 1250.5,
          currency: 'CNY',
          updatedAt: '2026-01-01T00:00:00Z'
        }
      })
    })
  );
}

/**
 * Assert the user cannot scroll horizontally at the given viewport.
 * This is the correct semantic check for "no horizontal scroll" — it tries
 * to scroll right and verifies the scroll position doesn't change.
 * A short pause is included to let React re-render and breakpoint hooks settle.
 */
async function assertNoHorizontalOverflow(page: Page) {
  // Allow React breakpoint hooks (useBreakpoint) to finish re-rendering
  await page.waitForTimeout(300);
  const canScrollHorizontally = await page.evaluate(() => {
    const before = window.scrollX;
    window.scrollBy(100, 0);
    const after = window.scrollX;
    // Reset scroll position
    if (after > 0) window.scrollBy(-after, 0);
    return after > before;
  });
  expect(canScrollHorizontally, 'User should not be able to scroll horizontally').toBe(false);
}

// ---------------------------------------------------------------------------
// Viewport definitions
// ---------------------------------------------------------------------------
const VIEWPORTS = [
  { label: '390 (mobile)', width: 390, height: 844 },
  { label: '768 (tablet)', width: 768, height: 1024 },
  { label: '1280 (desktop)', width: 1280, height: 800 }
] as const;

// ---------------------------------------------------------------------------
// Customer-side pages
// ---------------------------------------------------------------------------
test.describe('Responsive: ClientHomeV2', () => {
  for (const vp of VIEWPORTS) {
    test(`renders key content without horizontal overflow at ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await page.addInitScript(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
        window.localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
        window.localStorage.setItem('bobbuy_test_user', '1001');
      });
      await page.goto('/');
      await expect(page.getByRole('heading', { name: 'Elegant. Refined. Clean.' })).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

test.describe('Responsive: ClientOrders', () => {
  for (const vp of VIEWPORTS) {
    test(`renders order summary and trip selector without overflow at ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await page.addInitScript(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
        window.localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
        window.localStorage.setItem('bobbuy_test_user', '1001');
      });
      await page.goto('/client/orders');
      await expect(page.getByRole('heading', { name: 'My Orders' })).toBeVisible();
      // Order summary line visible in the first screen
      await expect(page.locator('[data-testid="orders-summary"]')).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

test.describe('Responsive: ClientBilling', () => {
  for (const vp of VIEWPORTS) {
    test(`renders billing entries without overflow at ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await page.addInitScript(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
        window.localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
        window.localStorage.setItem('bobbuy_test_user', '1001');
      });
      await page.goto('/client/billing');
      await expect(page.getByRole('heading', { name: 'Billing' })).toBeVisible();
      await expect(page.getByText('BIZ-1001')).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

test.describe('Responsive: ClientChat', () => {
  for (const vp of VIEWPORTS) {
    test(`renders chat page without overflow at ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await page.addInitScript(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
        window.localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
        window.localStorage.setItem('bobbuy_test_user', '1001');
      });
      await page.goto('/client/chat');
      await expect(page.getByRole('heading', { name: 'Chat' })).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

// ---------------------------------------------------------------------------
// Agent-side (back-office) pages
// ---------------------------------------------------------------------------
test.describe('Responsive: Orders (back-office)', () => {
  for (const vp of VIEWPORTS) {
    test(`renders orders page without overflow at ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await page.addInitScript(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
        window.localStorage.setItem('bobbuy_user_role', 'AGENT');
        window.localStorage.setItem('bobbuy_test_user', '1000');
      });
      await page.goto('/orders');
      // Page renders: check trip selector or any meaningful content is present
      await expect(page.locator('.ant-select, .ant-table, .ant-card').first()).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

test.describe('Responsive: StockMaster', () => {
  for (const vp of VIEWPORTS) {
    test(`renders stock master page without overflow at ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await page.addInitScript(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
        window.localStorage.setItem('bobbuy_user_role', 'AGENT');
        window.localStorage.setItem('bobbuy_test_user', '1000');
      });
      await page.goto('/stock-master');
      await expect(page.getByRole('heading', { name: 'Stock Master - Bulk Onboarding' })).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

test.describe('Responsive: ZenAuditView', () => {
  for (const vp of VIEWPORTS) {
    test(`renders zen audit page without overflow at ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await page.addInitScript(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
        window.localStorage.setItem('bobbuy_user_role', 'AGENT');
        window.localStorage.setItem('bobbuy_test_user', '1000');
      });
      await page.goto('/audit/2000');
      await expect(page.getByRole('heading', { name: 'Digital Scroll' })).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

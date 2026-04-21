import { expect, test } from '@playwright/test';

test.describe('client route gates', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/trips', async (route) => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({
          status: 'success',
          data: [
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
          ]
        })
      });
    });

    await page.route('**/api/orders?tripId=2000', async (route) => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({
          status: 'success',
          data: [
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
          ]
        })
      });
    });

    await page.route('**/api/procurement/2000/ledger', async (route) => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({
          status: 'success',
          data: [{ businessId: 'BIZ-1001', customerId: 1001, totalReceivable: 65, paidDeposit: 0, outstandingBalance: 65 }]
        })
      });
    });

    await page.route('**/api/chat/trips/2000', async (route) => {
      await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) });
    });

    await page.route('**/api/chat/send', async (route) => {
      await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: {} }) });
    });

    await page.route('**/api/metrics', async (route) => {
      await route.fulfill({
        status: 200,
        body: JSON.stringify({ status: 'success', data: { users: 0, trips: 0, orders: 0, gmV: 0 } })
      });
    });
  });

  test('customer can access orders, billing and chat with key content loaded', async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('bobbuy_locale', 'en-US');
      window.localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
      window.localStorage.setItem('bobbuy_test_user', '1001');
    });

    await page.goto('/client/orders');
    await expect(page.getByText('My Orders')).toBeVisible();
    await expect(page.getByText(/Orders:\s*1\s*\|\s*Total:/)).toBeVisible();

    await page.goto('/client/billing');
    await expect(page.getByText('Billing')).toBeVisible();
    await expect(page.getByText('BIZ-1001')).toBeVisible();

    await page.goto('/client/chat');
    await expect(page.getByText('Chat')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Open chat' })).toBeVisible();
  });

  test('agent is redirected from customer routes to dashboard', async ({ page }) => {
    await page.addInitScript(() => {
      window.localStorage.setItem('bobbuy_locale', 'en-US');
      window.localStorage.setItem('bobbuy_user_role', 'AGENT');
      window.localStorage.setItem('bobbuy_test_user', '1000');
    });

    await page.goto('/client/orders');
    await expect(page).toHaveURL(/\/dashboard$/);

    await page.goto('/client/billing');
    await expect(page).toHaveURL(/\/dashboard$/);

    await page.goto('/client/chat');
    await expect(page).toHaveURL(/\/dashboard$/);
  });
});

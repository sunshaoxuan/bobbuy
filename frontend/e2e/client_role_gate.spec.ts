import { expect, test } from '@playwright/test';
import { setAgentContext, setCustomerContext, setupCommonMocks } from './responsive_helpers';

test.describe('client route gates', () => {
  test.beforeEach(async ({ page }) => {
    await setupCommonMocks(page);
  });

  test('customer can access orders, billing and chat with key content loaded', async ({ page }) => {
    await setCustomerContext(page);

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
    await setAgentContext(page);

    await page.goto('/client/orders');
    await expect(page).toHaveURL(/\/dashboard$/);

    await page.goto('/client/billing');
    await expect(page).toHaveURL(/\/dashboard$/);

    await page.goto('/client/chat');
    await expect(page).toHaveURL(/\/dashboard$/);
  });
});

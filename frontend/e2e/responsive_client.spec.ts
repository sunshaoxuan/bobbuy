import { expect, test } from '@playwright/test';
import { assertNoHorizontalOverflow, setCustomerContext, setupCommonMocks, VIEWPORTS } from './responsive_helpers';

test.describe('Responsive: Client pages', () => {
  for (const vp of VIEWPORTS) {
    test(`ClientHomeV2 @ ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await setCustomerContext(page);
      await page.goto('/');
      await expect(page.getByRole('heading', { name: 'Elegant. Refined. Clean.' })).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });

    test(`ClientOrders @ ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await setCustomerContext(page);
      await page.goto('/client/orders');
      await expect(page.getByTestId('client-orders-title')).toBeVisible();
      await expect(page.locator('[data-testid="orders-summary"]')).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });

    test(`ClientBilling @ ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await setCustomerContext(page);
      await page.goto('/client/billing');
      await expect(page.getByTestId('client-billing-title')).toBeVisible();
      await expect(page.getByText('BIZ-1001')).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });

    test(`ClientChat @ ${vp.label}`, async ({ page }) => {
      await page.setViewportSize({ width: vp.width, height: vp.height });
      await setupCommonMocks(page);
      await setCustomerContext(page);
      await page.goto('/client/chat');
      await expect(page.getByTestId('client-chat-title')).toBeVisible();
      await assertNoHorizontalOverflow(page);
    });
  }
});

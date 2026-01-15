import { expect, test } from '@playwright/test';

test('orders page shows list header', async ({ page }) => {
  await page.addInitScript(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
  });

  await page.goto('/');
  await page.getByRole('link', { name: 'Orders' }).click();
  await expect(page.getByText('Order List')).toBeVisible();
});

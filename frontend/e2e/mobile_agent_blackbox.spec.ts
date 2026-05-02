import { expect, test } from '@playwright/test';
import {
  MOBILE_BLACKBOX_VIEWPORTS,
  loginAsAgent,
  setupCommonMocks
} from './responsive_helpers';
import {
  assertMobileUsableCheckpoint,
  expectElementInViewport,
  navigateFromMobileNav,
  RUN_REAL_MOBILE_BLACKBOX
} from './mobile_blackbox_helpers';

test.describe('Mobile blackbox: purchasing operator journey', () => {
  for (const viewport of MOBILE_BLACKBOX_VIEWPORTS) {
    test(`agent can complete core mobile tasks @ ${viewport.label}`, async ({ page }) => {
      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      if (!RUN_REAL_MOBILE_BLACKBOX) {
        await setupCommonMocks(page);
      }

      await loginAsAgent(page);
      await expect(page.locator('[data-testid="dashboard-title"]')).toBeVisible();
      await expect(page.getByText('Latest Trips')).toBeVisible();
      await assertMobileUsableCheckpoint(page, 'agent dashboard');

      await navigateFromMobileNav(page, 'Trips');
      await expect(page.locator('[data-testid="trips-title"]')).toBeVisible();
      await expectElementInViewport(page, '[data-testid="trips-submit"]', 'trip save CTA');
      await page.getByLabel('Origin').fill('Tokyo');
      await page.getByLabel('Destination').fill('Shanghai');
      await assertMobileUsableCheckpoint(page, 'agent trips');

      await navigateFromMobileNav(page, 'Orders');
      await expect(page.locator('[data-testid="orders-title"]')).toBeVisible();
      await expectElementInViewport(page, '[data-testid="orders-submit"]', 'order submit CTA');
      await page.getByLabel('Item Name').fill('Matcha Kit');
      await assertMobileUsableCheckpoint(page, 'agent orders');

      await page.getByRole('link', { name: /Scan/ }).click();
      await expect(page.locator('[data-testid="procurement-trip-select"]')).toBeVisible();
      await expect(page.getByText('Procurement Receipt Workbench')).toBeVisible();
      await expect(page.getByRole('button', { name: /CSV/ })).toBeVisible();
      await assertMobileUsableCheckpoint(page, 'agent procurement');

      await page.getByRole('link', { name: /Picking Master/ }).click();
      await expect(page.locator('[data-testid="picking-trip-select"]')).toBeVisible();
      const firstPickingCheckbox = page.getByRole('checkbox').first();
      if (await firstPickingCheckbox.isEnabled()) {
        await firstPickingCheckbox.click();
      }
      await expect(page.getByText(/Ready for Delivery|Pending Delivery/i)).toBeVisible();
      await assertMobileUsableCheckpoint(page, 'agent picking');

      await navigateFromMobileNav(page, 'Stock Master');
      await expect(page.locator('[data-testid="stock-master-title"]')).toBeVisible();
      await expectElementInViewport(page, '[data-testid="stock-master-add-row"]', 'stock add row CTA');
      await page.locator('[data-testid="stock-master-add-row"]').first().click();
      await expect(page.locator('.stock-drawer .ant-drawer-content')).toBeVisible();
      await page.keyboard.press('Escape');
      await expect(page.locator('.stock-drawer .ant-drawer-content')).toBeHidden();
      await assertMobileUsableCheckpoint(page, 'agent stock master');

      await navigateFromMobileNav(page, 'Supplier Directory');
      await expect(page.locator('[data-testid="suppliers-title"]')).toBeVisible();
      await expectElementInViewport(page, '[data-testid="suppliers-submit"]', 'supplier submit CTA');
      await assertMobileUsableCheckpoint(page, 'agent suppliers');

      await navigateFromMobileNav(page, 'Participants');
      await expect(page.locator('[data-testid="users-title"]')).toBeVisible();
      await expectElementInViewport(page, '[data-testid="users-submit"]', 'user submit CTA');
      await assertMobileUsableCheckpoint(page, 'agent users');
    });
  }
});

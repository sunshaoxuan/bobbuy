import { expect, type Page } from '@playwright/test';
import { assertNoHorizontalOverflow } from './responsive_helpers';

export const RUN_REAL_MOBILE_BLACKBOX = process.env.RUN_REAL_MOBILE_BLACKBOX === '1';

export async function assertMobileUsableCheckpoint(page: Page, label: string) {
  await assertNoHorizontalOverflow(page);
  const blockedByOpenDrawer = await page.locator('.ant-drawer-content-wrapper:visible').count();
  expect(blockedByOpenDrawer, `${label}: no drawer should remain open and cover the task surface`).toBe(0);
}

export async function openMobileNav(page: Page) {
  await page.getByRole('button', { name: /Open navigation menu|Open menu/i }).click();
  await expect(page.locator('.ant-drawer-content')).toBeVisible();
}

export async function navigateFromMobileNav(page: Page, name: string | RegExp) {
  await openMobileNav(page);
  await page.locator('.app-mobile-drawer').getByRole('link', { name }).click();
  await expect(page.locator('.ant-drawer-content')).toBeHidden();
}

export async function expectElementInViewport(page: Page, selector: string, label: string) {
  const box = await page.locator(selector).first().boundingBox();
  expect(box, `${label}: element should exist`).not.toBeNull();
  const viewport = page.viewportSize();
  expect(viewport, `${label}: viewport should be known`).not.toBeNull();
  expect(box!.x, `${label}: should not be clipped on the left`).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width, `${label}: should not be clipped on the right`).toBeLessThanOrEqual(viewport!.width + 1);
}

import { expect, test } from '@playwright/test';
import {
  MOBILE_BLACKBOX_VIEWPORTS,
  loginAsCustomer,
  setupCommonMocks
} from './responsive_helpers';
import {
  assertMobileUsableCheckpoint,
  expectElementInViewport,
  navigateFromMobileNav,
  openMobileNav,
  RUN_REAL_MOBILE_BLACKBOX
} from './mobile_blackbox_helpers';

test.describe('Mobile blackbox: customer operator journey', () => {
  for (const viewport of MOBILE_BLACKBOX_VIEWPORTS) {
    test(`customer can complete core mobile tasks @ ${viewport.label}`, async ({ page }) => {
      const sentMessages: Array<Record<string, unknown>> = [];

      await page.setViewportSize({ width: viewport.width, height: viewport.height });
      if (!RUN_REAL_MOBILE_BLACKBOX) {
        await setupCommonMocks(page);
        await page.route('**/api/chat/**/cursor**', (route) =>
          route.fulfill({
            status: 200,
            body: JSON.stringify({ status: 'success', data: { messages: sentMessages, nextCursor: null, hasMore: false } })
          })
        );
        await page.route('**/api/chat/send', async (route) => {
          const payload = (await route.request().postDataJSON()) as Record<string, unknown>;
          sentMessages.push({
            ...payload,
            id: sentMessages.length + 1,
            createdAt: '2026-05-02T00:00:00Z'
          });
          await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: payload }) });
        });
      }

      await loginAsCustomer(page);
      await expect(page.getByRole('heading', { name: 'Elegant. Refined. Clean.' })).toBeVisible();
      await expect(page.locator('.zen-product-card').first()).toBeVisible();
      await expectElementInViewport(page, '.zen-quick-buy', 'quick buy CTA');
      await page.locator('.zen-quick-buy').first().click();
      await expect(page.getByText(/success|failed|失敗|成功|失败/i)).toBeVisible();
      await assertMobileUsableCheckpoint(page, 'customer discover');

      await openMobileNav(page);
      await expect(page.locator('.ant-drawer-content').getByRole('link', { name: 'My Orders' })).toBeVisible();
      await page.keyboard.press('Escape');
      await expect(page.locator('.ant-drawer-content')).toBeHidden();

      await navigateFromMobileNav(page, 'Orders');
      await expect(page.getByTestId('client-orders-title')).toBeVisible();
      await expect(page.getByText(/Orders:\s*[1-9]\d*\s*\|\s*Total:/)).toBeVisible();
      await expect(page.locator('.client-list-card').first()).toBeVisible();
      await assertMobileUsableCheckpoint(page, 'customer orders');

      await navigateFromMobileNav(page, 'Billing');
      await expect(page.getByTestId('client-billing-title')).toBeVisible();
      await expect(page.locator('.client-list-card').first()).toBeVisible();
      const confirmReceiptButton = page.getByRole('button', { name: 'Confirm receipt' }).first();
      if (await confirmReceiptButton.isEnabled()) {
        await confirmReceiptButton.click();
        await expect(page.getByText('This confirmation is irreversible and will be written to the audit trail.')).toBeVisible();
        await page.getByRole('button', { name: 'Confirm', exact: true }).click();
        await expect(page.getByText('Billing confirmation saved')).toBeVisible();
      } else {
        await expect(confirmReceiptButton).toBeVisible();
      }
      await assertMobileUsableCheckpoint(page, 'customer billing');

      await navigateFromMobileNav(page, 'Chat');
      await expect(page.getByTestId('client-chat-title')).toBeVisible();
      await page.getByRole('button', { name: 'Open chat' }).click();
      await expect(page.getByText('Business Chat')).toBeVisible();
      await expectElementInViewport(page, 'button[aria-label="Upload image"]', 'chat image upload button');
      await page.getByPlaceholder('Type a message...').fill('Please confirm this order.');
      await page.getByRole('button', { name: 'Send message' }).click();
      await expect(page.getByText('Please confirm this order.').last()).toBeVisible();
      if (!RUN_REAL_MOBILE_BLACKBOX) {
        await expect.poll(() => sentMessages.length).toBeGreaterThan(0);
      }
      await assertMobileUsableCheckpoint(page, 'customer chat');
    });
  }
});

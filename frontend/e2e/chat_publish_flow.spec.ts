import { expect, test } from '@playwright/test';

test('chat image confirmation can create a draft item and publish it to mall', async ({ page }) => {
  const messages: Array<Record<string, unknown>> = [];

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

  await page.route('**/api/mobile/products', async (route) => {
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) });
  });

  await page.route('**/api/procurement/wallets/PURCHASER', async (route) => {
    await route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: { partnerId: 'PURCHASER', balance: 100, currency: 'CNY', updatedAt: '2026-04-20T00:00:00Z' }
      })
    });
  });

  await page.route('**/api/orders?tripId=2000', async (route) => {
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) });
  });

  await page.route('**/api/procurement/2000/ledger', async (route) => {
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) });
  });

  await page.route('**/api/chat/trips/2000', async (route) => {
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: messages }) });
  });

  await page.route('**/api/ai/onboard/scan', async (route) => {
    await route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          name: '抹茶礼盒',
          brand: 'BOBBuy',
          description: '',
          price: 88,
          categoryId: 'tea',
          itemNumber: 'SKU-998',
          storageCondition: 'AMBIENT',
          orderMethod: 'DIRECT_BUY',
          mediaGallery: [],
          attributes: {},
          existingProductFound: false,
          existingProductId: null,
          similarProductCandidates: [
            {
              productId: 'prd-temp',
              displayName: '抹茶礼盒大包装',
              itemNumber: 'SKU-998',
              matchReason: 'BRAND_AND_ITEM_NUMBER_FRAGMENT',
              matchSignals: ['BRAND_EXACT', 'ITEM_NUMBER_FRAGMENT'],
              score: 8.4,
              brand: 'BOBBuy',
              categoryId: 'tea',
              matchedFragments: ['bobbuy', '抹茶'],
              aliasSources: ['matcha']
            }
          ],
          visibilityStatus: 'DRAFTER_ONLY',
          detectedPriceTiers: [],
          originalPhotoBase64: 'data:image/png;base64,abc'
        }
      })
    });
  });

  await page.route('**/api/ai/onboard/confirm', async (route) => {
    await route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          product: {
            id: 'prd-temp',
            itemNumber: 'SKU-998',
            isTemporary: true,
            visibilityStatus: 'DRAFTER_ONLY',
            mediaGallery: [{ url: 'https://img.example/matcha.png', type: 'IMAGE' }]
          },
          displayName: '抹茶礼盒大包装',
          displayDescription: ''
        }
      })
    });
  });

  await page.route('**/api/chat/send', async (route) => {
    const payload = (await route.request().postDataJSON()) as Record<string, unknown>;
    messages.push({
      ...payload,
      id: messages.length + 1,
      createdAt: '2026-04-20T22:00:00'
    });
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: payload }) });
  });

  await page.route('**/api/mobile/products/prd-temp', async (route) => {
    await route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          product: {
            id: 'prd-temp',
            visibilityStatus: 'PUBLIC'
          }
        }
      })
    });
  });

  await page.addInitScript(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
    window.localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
  });

  await page.goto('/');

  await page.getByRole('button', { name: 'Open chat' }).click();
  await page.locator('input[type="file"]').setInputFiles({
    name: 'matcha.png',
    mimeType: 'image/png',
    buffer: Buffer.from('89504e470d0a1a0a', 'hex')
  });

  await expect(page.getByText('Confirm image context')).toBeVisible();
  await expect(page.getByText('Pending confirmation')).toBeVisible();
  await expect(page.getByText('Brand: BOBBuy · Category: tea')).toBeVisible();
  await page.getByLabel('抹茶礼盒大包装 · SKU-998 · BRAND_EXACT / ITEM_NUMBER_FRAGMENT').check();
  await page.getByRole('button', { name: 'OK' }).click();

  await expect(page.getByText('Candidate selected')).toBeVisible();
  await expect(page.getByText('BRAND_EXACT · ITEM_NUMBER_FRAGMENT')).toBeVisible();
  await page.getByRole('button', { name: 'Publish to Mall' }).click();

  await expect(page.locator('.ant-tag').filter({ hasText: 'Published to mall' }).first()).toBeVisible();
});

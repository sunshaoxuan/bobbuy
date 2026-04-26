import { expect, type Page } from '@playwright/test';

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
    status: 'PUBLISHED',
    settlementFrozen: false,
    settlementFreezeStage: 'ACTIVE',
    settlementFreezeReason: ''
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

export const VIEWPORTS = [
  { label: '390 (mobile)', width: 390, height: 844 },
  { label: '768 (tablet)', width: 768, height: 1024 },
  { label: '1280 (desktop)', width: 1280, height: 800 }
] as const;

export async function setCustomerContext(page: Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
    window.localStorage.setItem('bobbuy_user_role', 'CUSTOMER');
    window.localStorage.setItem('bobbuy_test_role', 'CUSTOMER');
    window.localStorage.setItem('bobbuy_test_user', '1001');
    window.localStorage.setItem('bobbuy_disable_chat_websocket', 'true');
  });
}

export async function setAgentContext(page: Page) {
  await page.addInitScript(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
    window.localStorage.setItem('bobbuy_user_role', 'AGENT');
    window.localStorage.setItem('bobbuy_test_role', 'AGENT');
    window.localStorage.setItem('bobbuy_test_user', '1000');
    window.localStorage.setItem('bobbuy_disable_chat_websocket', 'true');
  });
}

export async function assertNoHorizontalOverflow(page: Page) {
  await page.waitForTimeout(300);
  const result = await page.evaluate(() => {
    const before = window.scrollX;
    window.scrollBy(120, 0);
    const after = window.scrollX;
    if (after > 0) window.scrollTo(0, window.scrollY);
    const widthOverflow = document.documentElement.scrollWidth - document.documentElement.clientWidth;
    return {
      canScrollHorizontally: after > before,
      widthOverflow
    };
  });
  expect(result.canScrollHorizontally, 'User should not be able to scroll horizontally').toBe(false);
  expect(result.widthOverflow, 'Page should not exceed viewport width').toBeLessThanOrEqual(1);
}

export async function setupCommonMocks(page: Page) {
  // [Fallback] Must stay first: this absorbs non-essential `/api/**` calls and prevents
  // Vite proxy ECONNREFUSED noise in regular local E2E runs.
  // Any spec-level overrides should be registered after setupCommonMocks(page).
  await page.route('**/api/**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: {} }) })
  );

  await page.route('**/api/trips/*/procurement-list', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: [
          {
            skuId: 'SKU-001',
            itemName: 'Matcha',
            totalQuantity: 5,
            purchasedQuantity: 1,
            unitPrice: 32.5,
            businessIds: ['BIZ-1001']
          }
        ]
      })
    })
  );

  await page.route('**/api/trips/*/status', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: MOCK_TRIPS[0] }) })
  );

  await page.route('**/api/trips**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: MOCK_TRIPS }) })
  );

  await page.route('**/api/orders**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: MOCK_ORDERS, meta: { total: 1 } }) })
  );

  await page.route('**/api/users**', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: [{ id: 1001, name: 'Alice Wang', role: 'CUSTOMER', rating: 4.7 }]
      })
    })
  );

  await page.route('**/api/metrics', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({ status: 'success', data: { users: 2, trips: 1, orders: 1, gmV: 65, orderStatusCounts: { CONFIRMED: 1 } } })
    })
  );

  await page.route('**/api/ai/parse', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          items: [{ id: 'ai-1', originalName: 'Matcha', matchedName: 'Matcha', quantity: 1, note: '', price: 32.5, confidence: 0.95 }]
        }
      })
    })
  );

  await page.route('**/api/ai/experience/confirm', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: {} }) })
  );

  await page.route('**/api/chat/**', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );

  await page.route('**/api/procurement/*/hud', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          tripId: 2000,
          currentFxRate: 1,
          referenceFxRate: 1,
          currentPurchasedAmount: 65,
          totalTripExpenses: 0,
          totalEstimatedProfit: 10,
          currentWeight: 1,
          currentVolume: 1,
          categoryCompletionPercent: {},
          partnerShares: []
        }
      })
    })
  );

  await page.route('**/api/procurement/*/ledger', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: [{
          tripId: 2000,
          businessId: 'BIZ-1001',
          customerId: 1001,
          totalReceivable: 65,
          paidDeposit: 0,
          outstandingBalance: 65,
          settlementStatus: 'PENDING_CONFIRMATION',
          settlementFrozen: false,
          settlementFreezeStage: 'ACTIVE',
          settlementFreezeReason: '',
          orderLines: [{ skuId: 'SKU-001', itemName: 'Matcha', orderedQuantity: 2, purchasedQuantity: 1, unitPrice: 32.5, differenceNote: 'Short shipped 1' }]
        }]
      })
    })
  );

  await page.route('**/api/procurement/*/picking', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: [{
          businessId: 'BIZ-1001',
          customerId: 1001,
          customerName: 'Alice Wang',
          deliveryStatus: 'PENDING_DELIVERY',
          addressSummary: 'Shanghai Pudong Century Ave 88',
          readyForDelivery: false,
          items: [{
            skuId: 'SKU-001',
            itemName: 'Matcha',
            orderedQuantity: 2,
            pickedQuantity: 1,
            checked: false,
            labels: ['SHORT_SHIPPED']
          }]
        }]
      })
    })
  );

  await page.route('**/api/procurement/*/picking/*', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          businessId: 'BIZ-1001',
          customerId: 1001,
          customerName: 'Alice Wang',
          deliveryStatus: 'READY_FOR_DELIVERY',
          addressSummary: 'Shanghai Pudong Century Ave 88',
          readyForDelivery: true,
          items: [{
            skuId: 'SKU-001',
            itemName: 'Matcha',
            orderedQuantity: 2,
            pickedQuantity: 1,
            checked: true,
            labels: ['SHORT_SHIPPED']
          }]
        }
      })
    })
  );

  await page.route('**/api/procurement/*/receipts', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: [{
          id: 11,
          tripId: 2000,
          fileName: 'receipt-1.jpg',
          originalImageUrl: 'https://example.com/receipt.jpg',
          thumbnailUrl: 'https://example.com/receipt.jpg',
          processingStatus: 'READY_FOR_REVIEW',
          uploadedAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
          reconciliationResult: {
            receiptItems: [{ name: 'Matcha', quantity: 1, unitPrice: 32.5 }],
            matchedOrderLines: [],
            unmatchedReceiptItems: [{ name: 'Store sample item', quantity: 1, disposition: 'UNREVIEWED' }],
            missingOrderedItems: [],
            selfUseItems: []
          }
        }]
      })
    })
  );

  await page.route('**/api/procurement/*/profit-sharing', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: { tripId: 2000, purchaserRatioPercent: 70, promoterRatioPercent: 30, shares: [] }
      })
    })
  );

  await page.route('**/api/procurement/*/expenses', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );

  await page.route('**/api/procurement/*/logistics', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );

  await page.route('**/api/procurement/*/deficit', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );

  await page.route('**/api/procurement/*/audit-logs', (route) =>
    route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: [] }) })
  );

  await page.route('**/api/procurement/wallets/*/transactions', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: [{ id: 9000, partnerId: 'PURCHASER', amount: 45.2, type: 'TRIP_PAYOUT', tripId: 2000, createdAt: '2026-01-01T00:00:00Z' }]
      })
    })
  );

  await page.route('**/api/procurement/wallets/*', (route) =>
    route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: { partnerId: 'PURCHASER', balance: 1250.5, currency: 'CNY', updatedAt: '2026-01-01T00:00:00Z' }
      })
    })
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
}

import { expect, test } from '@playwright/test';
import { setAgentContext, setCustomerContext, setupCommonMocks } from './responsive_helpers';

test('client billing confirmation posts irreversible action', async ({ page }) => {
  let confirmedAction = '';
  await setupCommonMocks(page);
  await page.route('**/api/procurement/2000/ledger/BIZ-1001/confirm', async (route) => {
    const payload = await route.request().postDataJSON();
    confirmedAction = payload.action;
    await route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: {
          tripId: 2000,
          businessId: 'BIZ-1001',
          customerId: 1001,
          totalReceivable: 65,
          paidDeposit: 0,
          outstandingBalance: 65,
          settlementStatus: 'RECEIPT_CONFIRMED',
          settlementFrozen: false,
          settlementFreezeStage: 'ACTIVE',
          settlementFreezeReason: '',
          receiptConfirmedAt: '2026-01-01T00:00:00Z',
          receiptConfirmedBy: '1001',
          orderLines: []
        }
      })
    });
  });
  await setCustomerContext(page);
  await page.goto('/client/billing');
  await page.getByRole('button', { name: 'Confirm receipt' }).click();
  await page.getByRole('dialog').getByRole('button', { name: 'Confirm', exact: true }).click();
  await expect.poll(() => confirmedAction).toBe('RECEIPT');
});

test('settlement frozen trip disables order mutations and shows receipt workbench', async ({ page }) => {
  await setupCommonMocks(page);
  await page.route('**/api/trips**', async (route) => {
    await route.fulfill({
      status: 200,
      body: JSON.stringify({
        status: 'success',
        data: [{
          id: 2000,
          agentId: 1000,
          origin: 'Tokyo',
          destination: 'Shanghai',
          departDate: '2026-04-20',
          capacity: 10,
          reservedCapacity: 2,
          remainingCapacity: 8,
          status: 'COMPLETED',
          settlementFrozen: true,
          settlementFreezeStage: 'PROCUREMENT_PENDING_SETTLEMENT',
          settlementFreezeReason: 'Trip completed and frozen for settlement'
        }]
      })
    });
  });
  await setAgentContext(page);
  await page.goto('/orders');
  await expect(page.getByText('PROCUREMENT_PENDING_SETTLEMENT')).toBeVisible();
  await expect(page.locator('[data-testid="orders-submit"]')).toBeDisabled();

  await page.goto('/procurement');
  await expect(page.getByText('Procurement Receipt Workbench')).toBeVisible();
  await expect(page.getByText(/Settlement freeze:/)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save reconciliation' })).toBeVisible();
});

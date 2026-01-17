import { expect, test } from '@playwright/test';

type Trip = {
  id: number;
  agentId: number;
  origin: string;
  destination: string;
  departDate: string;
  capacity: number;
  reservedCapacity: number;
  remainingCapacity: number;
  status: string;
};

type Order = {
  id: number;
  customerId: number;
  tripId: number;
  itemName: string;
  quantity: number;
  unitPrice: number;
  serviceFee: number;
  estimatedTax: number;
  currency: string;
  status: string;
};

type AuditLog = {
  id: number;
  entityType: string;
  entityId: number;
  action: string;
  beforeValue: string;
  afterValue: string;
  userId: number;
  createdAt: string;
};

test('shopping flow patches status and writes audit logs', async ({ page }) => {
  const trips: Trip[] = [];
  const orders: Order[] = [];
  const auditLogs: AuditLog[] = [];
  let auditId = 1;

  const addAuditLog = (entityType: string, entityId: number, beforeValue: string, afterValue: string) => {
    auditLogs.push({
      id: auditId++,
      entityType,
      entityId,
      action: 'STATUS_CHANGE',
      beforeValue,
      afterValue,
      userId: 0,
      createdAt: new Date().toISOString()
    });
  };

  await page.route('**/api/trips', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: trips }) });
      return;
    }
    if (route.request().method() === 'POST') {
      const payload = (await route.request().postDataJSON()) as Omit<Trip, 'id' | 'remainingCapacity'>;
      const id = 2000 + trips.length;
      const remainingCapacity = payload.capacity - payload.reservedCapacity;
      const trip: Trip = { id, remainingCapacity, ...payload };
      trips.push(trip);
      await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: trip }) });
      return;
    }
    await route.fallback();
  });

  await page.route('**/api/orders', async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: orders }) });
      return;
    }
    if (route.request().method() === 'POST') {
      const payload = (await route.request().postDataJSON()) as Omit<Order, 'id'>;
      const id = 3000 + orders.length;
      const order: Order = { id, ...payload };
      orders.push(order);
      await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: order }) });
      return;
    }
    await route.fallback();
  });

  await page.route('**/api/trips/*/status', async (route) => {
    const tripId = Number(route.request().url().split('/').slice(-2)[0]);
    const payload = (await route.request().postDataJSON()) as { status: string };
    const trip = trips.find((item) => item.id === tripId);
    if (!trip) {
      await route.fulfill({ status: 404 });
      return;
    }
    addAuditLog('TRIP', tripId, trip.status, payload.status);
    trip.status = payload.status;
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: trip }) });
  });

  await page.route('**/api/orders/*/status', async (route) => {
    const orderId = Number(route.request().url().split('/').slice(-2)[0]);
    const payload = (await route.request().postDataJSON()) as { status: string };
    const order = orders.find((item) => item.id === orderId);
    if (!order) {
      await route.fulfill({ status: 404 });
      return;
    }
    addAuditLog('ORDER', orderId, order.status, payload.status);
    order.status = payload.status;
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: order }) });
  });

  await page.route('**/api/audit-logs', async (route) => {
    await route.fulfill({ status: 200, body: JSON.stringify({ status: 'success', data: auditLogs }) });
  });

  await page.addInitScript(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
  });

  await page.goto('/');
  await page.getByRole('link', { name: 'Trips' }).click();

  await page.getByLabel('Agent ID').fill('1000');
  await page.getByLabel('Origin').fill('Tokyo');
  await page.getByLabel('Destination').fill('Seoul');
  await page.getByLabel('Departure Date').fill('2026-02-01');
  await page.getByLabel('Capacity').fill('3');
  await page.getByRole('button', { name: 'Save Trip' }).click();

  const tripRow = page.getByRole('row', { name: /Tokyo/ });
  await tripRow.getByRole('combobox').click();
  await page.getByText('PUBLISHED', { exact: true }).click();

  const tripAudit = await page.request.get('/api/audit-logs');
  const tripLogs = (await tripAudit.json()) as { data: AuditLog[] };
  expect(tripLogs.data.some((log) => log.entityType === 'TRIP' && log.afterValue === 'PUBLISHED')).toBe(true);

  await page.getByRole('link', { name: 'Orders' }).click();
  await page.getByLabel('Customer ID').fill('1001');
  await page.getByLabel('Trip ID').fill('2000');
  await page.getByLabel('Item Name').fill('Camera');
  await page.getByLabel('Quantity').fill('1');
  await page.getByLabel('Unit Price').fill('200');
  await page.getByLabel('Service Fee').fill('10');
  await page.getByLabel('Estimated Tax').fill('5');
  await page.getByRole('button', { name: 'Create Order' }).click();

  const orderRow = page.getByRole('row', { name: /Camera/ });
  await orderRow.getByRole('combobox').click();
  await page.getByText('CONFIRMED', { exact: true }).click();

  const orderAudit = await page.request.get('/api/audit-logs');
  const orderLogs = (await orderAudit.json()) as { data: AuditLog[] };
  expect(orderLogs.data.some((log) => log.entityType === 'ORDER' && log.afterValue === 'CONFIRMED')).toBe(true);
});

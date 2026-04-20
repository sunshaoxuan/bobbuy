import { fireEvent, render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import PickingMaster from './PickingMaster';
import { I18nProvider } from '../i18n';

vi.mock('antd', async () => {
  const antd = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...antd,
    Select: ({ options = [], onChange, value }: any) => (
      <select
        aria-label="trip-select"
        data-testid="trip-select"
        value={value}
        onChange={(event) => onChange?.(Number(event.target.value))}
      >
        {options.map((option: any) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    )
  };
});

vi.mock('../api', () => ({
  api: {
    trips: () =>
      Promise.resolve([
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
      ]),
    procurementList: () =>
      Promise.resolve([
        {
          skuId: 'prd-1000',
          itemName: 'Matcha Kit',
          totalQuantity: 3,
          purchasedQuantity: 1,
          unitPrice: 32.5,
          businessIds: ['BIZ-001', 'BIZ-002']
        },
        {
          skuId: 'prd-1001',
          itemName: 'Tokyo Banana',
          totalQuantity: 1,
          purchasedQuantity: 1,
          unitPrice: 18,
          businessIds: ['BIZ-003']
        }
      ]),
    orders: () =>
      Promise.resolve([
        {
          id: 3000,
          businessId: 'BIZ-001',
          customerId: 1001,
          tripId: 2000,
          status: 'CONFIRMED',
          totalAmount: 65,
          lines: [{ skuId: 'prd-1000', itemName: 'Matcha Kit', quantity: 2, purchasedQuantity: 1, unitPrice: 32.5 }]
        },
        {
          id: 3001,
          businessId: 'BIZ-002',
          customerId: 1002,
          tripId: 2000,
          status: 'CONFIRMED',
          totalAmount: 32.5,
          lines: [{ skuId: 'prd-1000', itemName: 'Matcha Kit', quantity: 1, purchasedQuantity: 0, unitPrice: 32.5 }]
        },
        {
          id: 3002,
          businessId: 'BIZ-003',
          customerId: 1003,
          tripId: 2000,
          status: 'CONFIRMED',
          totalAmount: 18,
          lines: [{ skuId: 'prd-1001', itemName: 'Tokyo Banana', quantity: 1, purchasedQuantity: 1, unitPrice: 18 }]
        }
      ])
  }
}));

describe('PickingMaster', () => {
  beforeEach(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
  });

  afterEach(() => {
    window.localStorage.removeItem('bobbuy_locale');
  });

  it('renders procurement items from the real procurement contract', async () => {
    render(
      <I18nProvider>
        <BrowserRouter>
          <PickingMaster />
        </BrowserRouter>
      </I18nProvider>
    );

    expect((await screen.findAllByText(/Picking Master/i)).length).toBeGreaterThan(0);
    expect(await screen.findByText('Matcha Kit')).toBeInTheDocument();
    expect(await screen.findByText('Tokyo Banana')).toBeInTheDocument();
    expect(await screen.findByText('￥32.5')).toBeInTheDocument();
  });

  it('filters incomplete procurement rows', async () => {
    render(
      <I18nProvider>
        <BrowserRouter>
          <PickingMaster />
        </BrowserRouter>
      </I18nProvider>
    );

    expect(await screen.findByText('Matcha Kit')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('radio', { name: /To Pick/i }));

    expect(screen.getByText('Matcha Kit')).toBeInTheDocument();
    expect(screen.queryByText('Tokyo Banana')).not.toBeInTheDocument();
  });

  it('shows business drill-down in the details modal', async () => {
    render(
      <I18nProvider>
        <BrowserRouter>
          <PickingMaster />
        </BrowserRouter>
      </I18nProvider>
    );

    expect(await screen.findByText('Matcha Kit')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: /Details/i })[0]);

    expect(await screen.findByText(/Business ID: BIZ-001/i)).toBeInTheDocument();
    expect(await screen.findByText(/Business ID: BIZ-002/i)).toBeInTheDocument();
    expect(screen.getByText('1/2')).toBeInTheDocument();
  });
});

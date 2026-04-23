import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ClientBilling from './ClientBilling';
import { I18nProvider } from '../i18n';

const { confirmSpy, modalConfirmSpy } = vi.hoisted(() => ({
  confirmSpy: vi.fn(),
  modalConfirmSpy: vi.fn()
}));

vi.mock('antd', async () => {
  const antd = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...antd,
    Table: ({ dataSource = [] }: any) => <div>{dataSource.map((item: any) => item.itemName ?? '').join(' ')}</div>,
    Select: ({ options = [], onChange, value }: any) => (
      <select value={value} onChange={(event) => onChange?.(Number(event.target.value))}>
        {options.map((option: any) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    ),
    Modal: {
      ...antd.Modal,
      confirm: (config: any) => {
        modalConfirmSpy(config);
        return config.onOk?.();
      }
    },
    message: {
      success: vi.fn(),
      error: vi.fn()
    }
  };
});

vi.mock('../api', () => ({
  api: {
    trips: vi.fn(() => Promise.resolve([{ id: 2000, agentId: 1, origin: 'Tokyo', destination: 'Shanghai', departDate: '2026-01-01', capacity: 10, reservedCapacity: 1, status: 'PUBLISHED', settlementFrozen: false, settlementFreezeStage: 'ACTIVE', settlementFreezeReason: '' }])),
    customerBalanceLedger: vi.fn(() => Promise.resolve([
      {
        tripId: 2000,
        businessId: 'BIZ-1001',
        customerId: 1001,
        totalReceivable: 100,
        paidDeposit: 10,
        outstandingBalance: 90,
        settlementStatus: 'PENDING_CONFIRMATION',
        settlementFrozen: false,
        settlementFreezeStage: 'ACTIVE',
        settlementFreezeReason: '',
        orderLines: [{ skuId: 'SKU-1', itemName: 'Matcha', orderedQuantity: 2, purchasedQuantity: 1, unitPrice: 50, differenceNote: 'Short shipped 1' }]
      }
    ])),
    confirmCustomerLedger: confirmSpy
  }
}));

describe('ClientBilling', () => {
  beforeEach(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
    confirmSpy.mockReset();
    modalConfirmSpy.mockReset();
    confirmSpy.mockResolvedValue({});
  });

  it('renders detailed bill and triggers receipt confirmation', async () => {
    const user = userEvent.setup();
    render(
      <I18nProvider>
        <ClientBilling />
      </I18nProvider>
    );

    expect(await screen.findByText('BIZ-1001')).toBeInTheDocument();
    expect(await screen.findByText('Matcha')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Confirm receipt' }));
    await waitFor(() => expect(confirmSpy).toHaveBeenCalledWith(2000, 'BIZ-1001', 'RECEIPT'));
    expect(modalConfirmSpy).toHaveBeenCalled();
  });
});

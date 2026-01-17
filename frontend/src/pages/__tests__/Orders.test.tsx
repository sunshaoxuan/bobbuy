import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Orders from '../Orders';
import { I18nProvider } from '../../i18n';
import { api } from '../../api';
import { message } from 'antd';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('antd', async () => {
  const antd = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...antd,
    message: {
      success: vi.fn(),
      error: vi.fn()
    },
    Select: ({ value, onChange, options = [], disabled }: any) => (
      <select
        aria-label="select"
        value={value}
        onChange={(event) => onChange?.(event.target.value)}
        disabled={disabled}
      >
        {options.map((option: { value: string; label?: string }) => (
          <option key={option.value} value={option.value}>
            {option.label ?? option.value}
          </option>
        ))}
      </select>
    ),
    InputNumber: ({ value, onChange, ...props }: any) => {
      const safeValue = Number.isFinite(value) ? value : '';
      return (
        <input
          type="number"
          value={safeValue}
          onChange={(event) => {
            const next = event.target.value;
            onChange?.(next === '' ? undefined : Number(next));
          }}
          {...props}
        />
      );
    },
    Table: ({ dataSource = [], locale }: any) => (
      <div data-testid="table">
        {dataSource.length === 0 ? locale?.emptyText : (
          <table>
            <tbody>
              {dataSource.map((item: any) => (
                <tr key={item.id}>
                  <td>{item.businessKey}</td>
                  <td>{item.items.map((i: any) => i.itemName).join(', ')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    )
  };
});

vi.mock('../../api', () => ({
  api: {
    orders: vi.fn(),
    createOrder: vi.fn()
  }
}));

const mockApi = api as unknown as {
  orders: ReturnType<typeof vi.fn>;
  createOrder: ReturnType<typeof vi.fn>;
};

const renderOrders = () =>
  render(
    <I18nProvider>
      <Orders />
    </I18nProvider>
  );

const fillRequiredFields = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.type(screen.getByPlaceholderText('Enter customer ID'), '1001');
  await user.clear(screen.getByPlaceholderText('e.g. 2026011701'));
  await user.type(screen.getByPlaceholderText('e.g. 2026011701'), 'EV-01');
  await user.type(screen.getByPlaceholderText('Enter trip ID'), '2000');
  await user.type(screen.getByPlaceholderText('e.g. Limited Edition Sneakers'), 'Headphones');
  await user.clear(screen.getByPlaceholderText('Enter unit price'));
  await user.type(screen.getByPlaceholderText('Enter unit price'), '32');
  await user.clear(screen.getByPlaceholderText('Enter service fee'));
  await user.type(screen.getByPlaceholderText('Enter service fee'), '5');
  await user.clear(screen.getByPlaceholderText('Enter estimated tax'));
  await user.type(screen.getByPlaceholderText('Enter estimated tax'), '1');
};

describe('Orders page', () => {
  beforeEach(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
    mockApi.orders.mockReset();
    mockApi.createOrder.mockReset();
    (message.success as ReturnType<typeof vi.fn>).mockReset();
    (message.error as ReturnType<typeof vi.fn>).mockReset();
    mockApi.orders.mockResolvedValue([]);
  });

  it('submits the form and refreshes the list', async () => {
    const user = userEvent.setup();
    const createdOrder = {
      id: 4000,
      businessKey: '1001-EV-01',
      customerId: 1001,
      tripId: 2000,
      items: [{ itemName: 'Headphones', quantity: 1, unitPrice: 32, variable: false }],
      serviceFee: 5,
      estimatedTax: 1,
      currency: 'CNY',
      status: 'NEW'
    };
    mockApi.orders.mockResolvedValueOnce([]).mockResolvedValueOnce([createdOrder]);
    mockApi.createOrder.mockResolvedValue(createdOrder);

    renderOrders();
    await waitFor(() => expect(mockApi.orders).toHaveBeenCalled());

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Create Order' }));

    await waitFor(() => expect(mockApi.createOrder).toHaveBeenCalled());
    // Verify businessKey construction
    expect(mockApi.createOrder).toHaveBeenCalledWith(expect.objectContaining({
      businessKey: '1001-EV-01'
    }));
    expect(mockApi.orders).toHaveBeenCalledTimes(2);
  });

  it('validates required fields', async () => {
    const user = userEvent.setup();
    renderOrders();
    await waitFor(() => expect(mockApi.orders).toHaveBeenCalled());

    await user.click(screen.getByRole('button', { name: 'Create Order' }));

    expect(await screen.findByText('Please enter customer ID')).toBeInTheDocument();
    expect(await screen.findByText('Please enter trip ID')).toBeInTheDocument();
    expect(await screen.findByText('Please enter item name')).toBeInTheDocument();
  });

  it('shows success feedback after a successful submit', async () => {
    const user = userEvent.setup();
    mockApi.createOrder.mockResolvedValue({
      id: 4001,
      businessKey: '1001-EV-02',
      customerId: 1001,
      tripId: 2000,
      items: [{ itemName: 'Headphones', quantity: 1, unitPrice: 32, variable: false }],
      serviceFee: 5,
      estimatedTax: 1,
      currency: 'CNY',
      status: 'NEW'
    });

    renderOrders();
    await waitFor(() => expect(mockApi.orders).toHaveBeenCalled());

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Create Order' }));

    await waitFor(() =>
      expect(message.success).toHaveBeenCalledWith('Order created successfully')
    );
  });
});

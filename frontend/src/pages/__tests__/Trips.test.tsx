import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Trips from '../Trips';
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
        {dataSource.length === 0 ? locale?.emptyText : null}
      </div>
    )
  };
});

vi.mock('../../api', () => ({
  api: {
    trips: vi.fn(),
    createTrip: vi.fn()
  }
}));

const mockApi = api as unknown as {
  trips: ReturnType<typeof vi.fn>;
  createTrip: ReturnType<typeof vi.fn>;
};

const renderTrips = () =>
  render(
    <I18nProvider>
      <Trips />
    </I18nProvider>
  );

const fillRequiredFields = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.type(screen.getByPlaceholderText('Enter agent ID'), '1000');
  await user.type(screen.getByPlaceholderText('e.g. Tokyo'), 'Tokyo');
  await user.type(screen.getByPlaceholderText('e.g. Shanghai'), 'Seoul');
  await user.type(screen.getByLabelText('Departure Date'), '2026-02-01');
  await user.clear(screen.getByPlaceholderText('Enter capacity'));
  await user.type(screen.getByPlaceholderText('Enter capacity'), '3');
};

describe('Trips page', () => {
  beforeEach(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
    mockApi.trips.mockReset();
    mockApi.createTrip.mockReset();
    (message.success as ReturnType<typeof vi.fn>).mockReset();
    (message.error as ReturnType<typeof vi.fn>).mockReset();
    mockApi.trips.mockResolvedValue([]);
  });

  it('submits the form and refreshes the list', async () => {
    const user = userEvent.setup();
    const createdTrip = {
      id: 2001,
      agentId: 1000,
      origin: 'Tokyo',
      destination: 'Seoul',
      departDate: '2026-02-01',
      capacity: 3,
      reservedCapacity: 0,
      remainingCapacity: 3,
      status: 'DRAFT'
    };
    mockApi.trips.mockResolvedValueOnce([]).mockResolvedValueOnce([createdTrip]);
    mockApi.createTrip.mockResolvedValue(createdTrip);

    renderTrips();
    await waitFor(() => expect(mockApi.trips).toHaveBeenCalled());

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Save Trip' }));

    await waitFor(() => expect(mockApi.createTrip).toHaveBeenCalled());
    expect(mockApi.trips).toHaveBeenCalledTimes(2);
  });

  it('validates required fields', async () => {
    const user = userEvent.setup();
    renderTrips();
    await waitFor(() => expect(mockApi.trips).toHaveBeenCalled());

    await user.click(screen.getByRole('button', { name: 'Save Trip' }));

    expect(await screen.findByText('Please enter agent ID')).toBeInTheDocument();
    expect(await screen.findByText('Please enter origin')).toBeInTheDocument();
    expect(await screen.findByText('Please enter destination')).toBeInTheDocument();
  });

  it('shows success feedback after a successful submit', async () => {
    const user = userEvent.setup();
    mockApi.createTrip.mockResolvedValue({
      id: 2001,
      agentId: 1000,
      origin: 'Tokyo',
      destination: 'Seoul',
      departDate: '2026-02-01',
      capacity: 3,
      reservedCapacity: 0,
      remainingCapacity: 3,
      status: 'DRAFT'
    });

    renderTrips();
    await waitFor(() => expect(mockApi.trips).toHaveBeenCalled());

    await fillRequiredFields(user);
    await user.click(screen.getByRole('button', { name: 'Save Trip' }));

    await waitFor(() =>
      expect(message.success).toHaveBeenCalledWith('Trip created successfully')
    );
  });
});

import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Dashboard from '../pages/Dashboard';
import { BrowserRouter } from 'react-router-dom';
import { I18nProvider } from '../i18n';

// Mock antd
vi.mock('antd', async () => {
    const antd = await vi.importActual<typeof import('antd')>('antd');
    return {
        ...antd,
        Table: ({ dataSource = [] }: any) => (
            <div data-testid="table">{dataSource.length} rows</div>
        ),
        Card: ({ title, children }: any) => (
            <div data-testid="card">
                <h3>{title}</h3>
                {children}
            </div>
        ),
        Divider: () => <hr />
    };
});

// Mock API
vi.mock('../api', () => ({
    api: {
        metrics: () => Promise.resolve({
            users: 10,
            trips: 5,
            orders: 20,
            gmV: 1000,
            orderStatusCounts: { NEW: 10, CONFIRMED: 5, PURCHASED: 5 }
        }),
        trips: () => Promise.resolve([]),
        orders: () => Promise.resolve([])
    }
}));

describe('Dashboard Component', () => {
    it('renders dashboard correctly', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <Dashboard />
                </BrowserRouter>
            </I18nProvider>
        );

        expect(await screen.findByText('10')).toBeInTheDocument();
        expect(await screen.findByText('5')).toBeInTheDocument();
    });
});

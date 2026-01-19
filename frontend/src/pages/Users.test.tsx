import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Users from '../pages/Users';
import { BrowserRouter } from 'react-router-dom';
import { I18nProvider } from '../i18n';

// Mock antd
vi.mock('antd', async () => {
    const antd = await vi.importActual<typeof import('antd')>('antd');
    return {
        ...antd,
        Table: ({ dataSource = [] }: any) => (
            <div data-testid="table">
                {dataSource.map((item: any) => (
                    <div key={item.id}>{item.name}</div>
                ))}
            </div>
        ),
        Card: ({ title, children }: any) => (
            <div data-testid="card">
                <h3>{title}</h3>
                {children}
            </div>
        ),
        Rate: () => <div data-testid="rate" />
    };
});

// Mock API
vi.mock('../api', () => ({
    api: {
        users: () => Promise.resolve([
            { id: 1000, name: 'Test User', role: 'CUSTOMER', rating: 5.0 }
        ])
    }
}));

describe('Users Component', () => {
    it('renders user list correctly', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <Users />
                </BrowserRouter>
            </I18nProvider>
        );
        expect(await screen.findByText('Test User')).toBeInTheDocument();
    });
});

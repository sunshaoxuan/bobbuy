import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import OrderDesk from '../pages/OrderDesk';
import { BrowserRouter } from 'react-router-dom';
import { I18nProvider } from '../i18n';

// Mock antd
vi.mock('antd', async () => {
    const antd = await vi.importActual<typeof import('antd')>('antd');
    return {
        ...antd,
        message: {
            success: vi.fn(),
        }
    };
});

describe('OrderDesk Component', () => {
    it('renders chat messages and extracted items', () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <OrderDesk />
                </BrowserRouter>
            </I18nProvider>
        );

        // Check if chat messages exist
        expect(screen.getByText(/I need some items from Costco today/i)).toBeInTheDocument();
        
        // Check if AI extraction title exists
        expect(screen.getByText(/AI Intelligent Extraction/i)).toBeInTheDocument();
        
        // Check if detected items are visible (use getAllByText as they appear in both chat and panel)
        expect(screen.getAllByText(/Organic Milk/i).length).toBeGreaterThan(0);
        expect(screen.getAllByText(/Croissants/i).length).toBeGreaterThan(0);
    });

    it('toggles item status when "Add" is clicked', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <OrderDesk />
                </BrowserRouter>
            </I18nProvider>
        );

        const addButtons = screen.getAllByRole('button', { name: /Add/i });
        fireEvent.click(addButtons[0]);

        // After clicking, the button text should change to 'Added'
        expect(await screen.findByText(/Added/i)).toBeInTheDocument();
    });
});

import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ProcurementHUD from '../pages/ProcurementHUD';
import { BrowserRouter } from 'react-router-dom';
import { I18nProvider } from '../i18n';

describe('ProcurementHUD Component', () => {
    it('renders HUD metrics and customer panels', () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <ProcurementHUD />
                </BrowserRouter>
            </I18nProvider>
        );

        // Check metrics
        expect(screen.getByText(/Items/i)).toBeInTheDocument();
        expect(screen.getByText(/Progress/i)).toBeInTheDocument();
        
        // Check customer names
        expect(screen.getByText(/Alice Wang/i)).toBeInTheDocument();
        expect(screen.getByText(/Benjamin Tsui/i)).toBeInTheDocument();
    });

    it('updates progress metric when an item is toggled', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <ProcurementHUD />
                </BrowserRouter>
            </I18nProvider>
        );

        // Initial progress (based on mock data 1/3 items done = ~33%)
        expect(screen.getByText('33%')).toBeInTheDocument();

        // Find a pending item and toggle it
        const checkButtons = screen.getAllByRole('button').filter(btn => btn.querySelector('.anticon-check-circle'));
        // Find the one that isn't already active (the mock has 1/3 done)
        // Click the second button (for Fresh Spinach)
        fireEvent.click(checkButtons[1]);

        // Progress should update (2/3 items done = ~67%)
        expect(await screen.findByText('67%')).toBeInTheDocument();
    });
});

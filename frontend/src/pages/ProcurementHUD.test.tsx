import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import ProcurementHUD from '../pages/ProcurementHUD';
import { BrowserRouter } from 'react-router-dom';
import { I18nProvider } from '../i18n';

vi.mock('../api', () => ({
    api: {
        trips: () => Promise.resolve([
            { id: 2000, agentId: 1, origin: 'Tokyo', destination: 'Shanghai', departDate: '2026-01-01', capacity: 10, reservedCapacity: 1, status: 'PUBLISHED' }
        ]),
        procurementHud: () => Promise.resolve({
            tripId: 2000,
            totalEstimatedProfit: 20,
            currentPurchasedAmount: 80,
            currentWeight: 6,
            currentVolume: 2,
            categoryCompletionPercent: {}
        }),
        orders: () => Promise.resolve([
            {
                id: 3000,
                businessId: '20260117001',
                customerId: 1001,
                tripId: 2000,
                status: 'NEW',
                totalAmount: 100,
                lines: [{ skuId: 'prd-1000', itemName: 'Matcha Kit', quantity: 2, purchasedQuantity: 1, unitPrice: 50 }]
            }
        ])
    }
}));

describe('ProcurementHUD Component', () => {
    beforeEach(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
    });

    afterEach(() => {
        window.localStorage.removeItem('bobbuy_locale');
    });

    it('renders profit/load/reconcile sections', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <ProcurementHUD />
                </BrowserRouter>
            </I18nProvider>
        );

        expect(await screen.findByText(/Profit Insight|利润透视/i)).toBeInTheDocument();
        expect(await screen.findByText(/Capacity Redline|容积红线/i)).toBeInTheDocument();
        expect(await screen.findByText(/Reconcile Detail|对账详情/i)).toBeInTheDocument();
        expect(await screen.findByText('20260117001')).toBeInTheDocument();
    });
});

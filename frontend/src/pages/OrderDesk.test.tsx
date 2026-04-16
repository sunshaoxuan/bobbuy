import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
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
    beforeEach(() => {
        vi.stubGlobal('fetch', vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
            const url = String(input);
            if (url.includes('/api/ai/parse')) {
                return new Response(JSON.stringify({
                    status: 'success',
                    data: {
                        items: [
                            {
                                id: 'AI-1',
                                originalName: '马粪蛋糕',
                                matchedName: 'Muffin',
                                quantity: 2,
                                note: '',
                                price: 6.99,
                                confidence: 0.92
                            },
                            {
                                id: 'AI-2',
                                originalName: 'Tomato',
                                matchedName: 'Tomato',
                                quantity: 1,
                                note: '',
                                price: 2.99,
                                confidence: 0.99
                            }
                        ]
                    }
                }), { status: 200, headers: { 'Content-Type': 'application/json' } });
            }
            if (url.includes('/api/ai/experience/confirm')) {
                return new Response(JSON.stringify({ status: 'success', data: null }), {
                    status: 200,
                    headers: { 'Content-Type': 'application/json' }
                });
            }
            return new Response(JSON.stringify({ status: 'success', data: {} }), {
                status: 200,
                headers: { 'Content-Type': 'application/json' }
            });
        }));
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('sends text and renders parsed items in sidebar', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <OrderDesk />
                </BrowserRouter>
            </I18nProvider>
        );

        fireEvent.change(screen.getByPlaceholderText(/Type a message/i), {
            target: { value: '马粪蛋糕两个，还有 Tomato' }
        });
        fireEvent.click(screen.getByRole('button', { name: /send/i }));

        expect(screen.getByText(/AI Intelligent Extraction/i)).toBeInTheDocument();
        expect(await screen.findByText(/Detected: Muffin \(x2\), Tomato \(x1\)/i)).toBeInTheDocument();
        expect(screen.getByText(/Muffin/i)).toBeInTheDocument();
        expect(screen.getByText(/Tomato/i)).toBeInTheDocument();
    });

    it('toggles item status when "Add" is clicked', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <OrderDesk />
                </BrowserRouter>
            </I18nProvider>
        );

        fireEvent.change(screen.getByPlaceholderText(/Type a message/i), {
            target: { value: '马粪蛋糕两个，还有 Tomato' }
        });
        fireEvent.click(screen.getByRole('button', { name: /send/i }));
        await screen.findByText(/Muffin/i);

        const addButtons = screen.getAllByRole('button', { name: /Add/i });
        fireEvent.click(addButtons[0]);

        await waitFor(() => {
            expect(screen.getByText(/Added/i)).toBeInTheDocument();
        });
    });
});

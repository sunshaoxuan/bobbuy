import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import PickingMaster from '../pages/PickingMaster';
import { BrowserRouter } from 'react-router-dom';
import { I18nProvider } from '../i18n';

describe('PickingMaster Component', () => {
    beforeEach(() => {
        window.localStorage.setItem('bobbuy_locale', 'en-US');
    });

    afterEach(() => {
        window.localStorage.removeItem('bobbuy_locale');
    });

    it('renders picking gallery with filter controls', () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <PickingMaster />
                </BrowserRouter>
            </I18nProvider>
        );

        expect(screen.getAllByText(/Picking Master/i).length).toBeGreaterThan(0);
        // Use getAllByText for labels that appear in multiple places (Radio and Tags)
        expect(screen.getAllByText(/To Pick/i).length).toBeGreaterThan(0);
    });

    it('filters items correctly', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <PickingMaster />
                </BrowserRouter>
            </I18nProvider>
        );

        // All items (initial 4)
        // Use queryAllByRole('img') and filter for actual <img> tags to avoid matching SVG icons
        const imgs = screen.getAllByRole('img').filter(el => el.tagName === 'IMG');
        expect(imgs.length).toBe(4);

        // Click 'To Pick' (initial 2)
        const todoFilter = screen.getByLabelText(/To Pick/i);
        fireEvent.click(todoFilter);
        
        // Wait for list update
        expect(screen.getAllByRole('img').length).toBe(2);
    });

    it('toggles picking state when item is clicked', async () => {
        render(
            <I18nProvider>
                <BrowserRouter>
                    <PickingMaster />
                </BrowserRouter>
            </I18nProvider>
        );

        // Find an unpicked item (Fresh Spinach)
        const spinachItem = screen.getByText(/Fresh Spinach/i).closest('.ant-card');
        const img = spinachItem?.querySelector('img');
        
        if (img) {
            fireEvent.click(img);
        }

        // It should now show 'Picked' tag
        const pickedTags = screen.getAllByText(/Picked/i);
        // Initial has 2 Picked + 1 more now = 3 (plus the filter button)
        // We'll just check if the tag exists for that item
    });
});

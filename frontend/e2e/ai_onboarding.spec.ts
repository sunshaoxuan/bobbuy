import { test, expect } from '@playwright/test';
import path from 'path';
import { setAgentContext } from './responsive_helpers';

test.skip(!process.env.RUN_AI_VISION_E2E, 'Manual AI onboarding flow requires dedicated backend model/files.');

test.describe('AI Vision Onboarding E2E', () => {
    test.beforeEach(async ({ page }) => {
        await setAgentContext(page);
        // Navigate to the stock master page
        await page.goto('/stock-master');
    });

    test('should successfully onboard a product using a sample photo (IMG_1484)', async ({ page }) => {
        // 1. Trigger AI Quick Add Modal
        await page.click('button:has-text("AI Quick Snap")');
        await expect(page.locator('.ant-modal-title:has-text("AI Quick Add")')).toBeVisible();

        // 2. Upload Sample Image (IMG_1484 - Seafood)
        const fileChooserPromise = page.waitForEvent('filechooser');
        await page.locator('.ant-upload-drag').click();
        const fileChooser = await fileChooserPromise;
        await fileChooser.setFiles(path.resolve('..', 'sample', 'IMG_1484.jpg'));

        // 3. Monitor Progress Steps
        // Use stable stage marker for acceptance, not only UI copy.
        await expect(page.locator('[data-testid="ai-onboarding-steps"]')).toHaveAttribute('data-stage', 'SCANNING', { timeout: 10000 });
        
        // Wait for final success state (this drives the REAL backend AI flow)
        await expect(page.locator('[data-testid="ai-onboarding-result-subtitle"][data-ai-status="SUCCESS"]')).toBeVisible({ timeout: 30000 });

        // 4. Inspect Extracted Metadata & Tiers
        await page.click('button:has-text("Edit Details")');
        
        // Verify Item Number was extracted correctly (IMG_1484 should have 53432)
        // Note: This matches the value in the photo
        const itemNumberInput = page.locator('label:has-text("Item Number") + div input');
        await expect(itemNumberInput).toHaveValue(/53432/);

        // Verify Price Tiers tab is visible and has the extracted content
        await page.click('.ant-tabs-tab-btn:has-text("Price Tiers")');
        const priceTierCard = page.locator('.price-tier-card').first();
        await expect(priceTierCard).toBeVisible();
    });

    test('should detect existing product for incremental update (IMG_1638)', async ({ page }) => {
        // This test assumes IMG_1638 (Item 93963) is already scanned or present
        // In a real CI, we might need to seed the DB first
        
        await page.click('button:has-text("AI Quick Snap")');
        const fileChooserPromise = page.waitForEvent('filechooser');
        await page.locator('.ant-upload-drag').click();
        const fileChooser = await fileChooserPromise;
        await fileChooser.setFiles(path.resolve('..', 'sample', 'IMG_1638.jpg'));

        // Stable acceptance marker for existing-product path.
        await expect(page.locator('[data-testid="ai-existing-product-alert"]')).toBeVisible({ timeout: 30000 });
    });
});

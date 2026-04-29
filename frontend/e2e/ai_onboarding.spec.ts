import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { setAgentContext } from './responsive_helpers';

test.skip(!process.env.RUN_AI_VISION_E2E, 'Manual AI onboarding flow requires dedicated backend model/files.');

const BANNED_SOURCE_KEYWORDS = ['xiaohongshu', 'xhslink', 'rednote'];
const TRUSTED_SOURCE_TYPES = ['OFFICIAL_SITE', 'BRAND_SITE', 'OFFICIAL_STORE', 'TRUSTED_RETAIL'];
const sampleGolden = JSON.parse(
    fs.readFileSync(path.resolve('..', 'docs', 'fixtures', 'ai-onboarding-sample-golden.json'), 'utf-8')
) as Array<any>;
const goldenBySampleId = new Map(sampleGolden.map((entry) => [entry.sampleId, entry]));

const extractData = async <T>(response: any): Promise<T> => {
    const json = await response.json();
    return json?.data as T;
};

async function openQuickAddAndUpload(page: any, sampleFilename: string) {
    await expect(page.locator('button:has-text("AI Quick Snap")')).toBeVisible();
    await page.click('button:has-text("AI Quick Snap")');
    const fileChooserPromise = page.waitForEvent('filechooser');
    await page.locator('.ant-upload-drag').click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles(path.resolve('..', 'sample', sampleFilename));
}

test.describe('AI Vision Onboarding E2E', () => {
    test.beforeEach(async ({ page }) => {
        await setAgentContext(page);
        await page.goto('/stock-master');
    });

    test('creates a new product and verifies list consistency + compliant sources (IMG_1484)', async ({ page }) => {
        const golden = goldenBySampleId.get('IMG_1484.jpg');
        const scanResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/ai/onboard/scan') && response.request().method() === 'POST'
        );
        await openQuickAddAndUpload(page, 'IMG_1484.jpg');
        await expect(page.locator('[data-testid="ai-onboarding-steps"]')).toHaveAttribute('data-stage', 'SCANNING', { timeout: 10000 });
        await expect(page.locator('[data-testid="ai-onboarding-result-subtitle"][data-ai-status="SUCCESS"]')).toBeVisible({ timeout: 30000 });
        const scanData = await extractData<any>(await scanResponsePromise);
        expect(scanData.existingProductFound).toBeFalsy();
        expect(scanData.name).toBeTruthy();
        expect(scanData.itemNumber).toBeTruthy();
        expect(scanData.price).toBeGreaterThan(0);
        expect(scanData.description).toBeTruthy();
        expect(scanData.sourceDomains?.length ?? 0).toBeGreaterThan(0);
        expect(scanData.mediaGallery?.length ?? 0).toBeGreaterThan(0);
        expect(scanData.itemNumber).toBe(golden?.expected?.itemNumber);
        expect(scanData.price).toBeGreaterThanOrEqual((golden?.expected?.basePrice ?? 0) - (golden?.tolerance?.priceTolerance ?? 0));
        expect(scanData.price).toBeLessThanOrEqual((golden?.expected?.basePrice ?? 0) + (golden?.tolerance?.priceTolerance ?? 0));
        expect(scanData.categoryId).toBe(golden?.expected?.categoryId);
        expect(scanData.attributes?.pricePerUnit).toBeTruthy();

        const sourceTypes = (scanData.mediaGallery ?? []).map((item: any) => item.sourceType).filter(Boolean);
        expect(sourceTypes.some((type: string) => TRUSTED_SOURCE_TYPES.includes(type))).toBeTruthy();
        const sourceDomains = (scanData.sourceDomains ?? []).map((domain: string) => domain.toLowerCase());
        for (const banned of BANNED_SOURCE_KEYWORDS) {
            expect(sourceDomains.some((domain: string) => domain.includes(banned))).toBeFalsy();
        }

        const confirmResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/ai/onboard/confirm') && response.request().method() === 'POST'
        );
        await page.click('button:has-text("Edit Details")');
        const confirmData = await extractData<any>(await confirmResponsePromise);
        expect(confirmData.product?.id).toBeTruthy();
        expect(confirmData.product?.itemNumber).toBe(scanData.itemNumber);
        expect(confirmData.product?.brand).toBe(scanData.brand);
        expect(confirmData.product?.basePrice).toBe(scanData.price);
        expect(confirmData.onboardingTrace?.resultDecision).toBe('NEW_PRODUCT');
        expect(confirmData.onboardingTrace?.finalProductId).toBe(confirmData.product?.id);

        await expect(page.locator('table')).toContainText(scanData.itemNumber);
        await expect(page.locator('table')).toContainText(scanData.name);

        const listResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/mobile/products') && response.request().method() === 'GET'
        );
        await page.reload();
        const listData = await extractData<any[]>(await listResponsePromise);
        const created = listData.find((entry: any) => entry.product?.id === confirmData.product?.id);
        expect(created).toBeTruthy();
        expect(created.product.itemNumber).toBe(scanData.itemNumber);
        expect(created.product.brand).toBe(scanData.brand);
    });

    test('detects existing product and confirms target product remains queryable (IMG_1638)', async ({ page }) => {
        const golden = goldenBySampleId.get('IMG_1638.jpg');
        const scanResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/ai/onboard/scan') && response.request().method() === 'POST'
        );
        await openQuickAddAndUpload(page, 'IMG_1638.jpg');
        await expect(page.locator('[data-testid="ai-existing-product-alert"]')).toBeVisible({ timeout: 30000 });
        const scanData = await extractData<any>(await scanResponsePromise);
        expect(scanData.existingProductFound).toBeTruthy();
        expect(scanData.existingProductId).toBeTruthy();
        expect(scanData.categoryId).toBe(golden?.expected?.categoryId);

        const confirmResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/ai/onboard/confirm') && response.request().method() === 'POST'
        );
        await page.click('button:has-text("Edit Details")');
        const confirmData = await extractData<any>(await confirmResponsePromise);
        expect(confirmData.product?.id).toBe(scanData.existingProductId);
        expect(confirmData.onboardingTrace?.resultDecision).toBe('EXISTING_PRODUCT');
        expect(confirmData.onboardingTrace?.finalProductId).toBe(scanData.existingProductId);

        const listResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/mobile/products') && response.request().method() === 'GET'
        );
        await page.reload();
        const listData = await extractData<any[]>(await listResponsePromise);
        expect(listData.some((entry: any) => entry.product?.id === scanData.existingProductId)).toBeTruthy();
    });
});

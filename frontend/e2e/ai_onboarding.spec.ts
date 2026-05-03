import { test, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';
import { loginAsAgent } from './responsive_helpers';

test.skip(!process.env.RUN_AI_VISION_E2E, 'Manual AI onboarding flow requires dedicated backend model/files.');
test.setTimeout(180_000);

const BANNED_SOURCE_KEYWORDS = ['xiaohongshu', 'xhslink', 'rednote'];
const TRUSTED_SOURCE_TYPES = ['OFFICIAL_SITE', 'BRAND_SITE', 'OFFICIAL_STORE', 'TRUSTED_RETAIL'];
const sampleGolden = JSON.parse(
    fs.readFileSync(path.resolve('..', 'docs', 'fixtures', 'ai-onboarding-sample-golden.json'), 'utf-8')
) as Array<any>;
const goldenBySampleId = new Map(sampleGolden.map((entry) => [entry.sampleId, entry]));
const REQUIRE_SEED_DEPENDENT_GOLDEN = process.env.REQUIRE_SEED_DEPENDENT_GOLDEN === '1';

const extractData = async <T>(response: any): Promise<T> => {
    const json = await response.json();
    return json?.data as T;
};

async function openQuickAddAndUpload(page: any, sampleFilename: string) {
    const quickSnapButton = page.locator('[data-testid="ai-quick-snap-button"]').first();
    await expect(quickSnapButton).toBeVisible();
    await quickSnapButton.click();
    const fileChooserPromise = page.waitForEvent('filechooser');
    await page.locator('.ant-upload-drag').click();
    const fileChooser = await fileChooserPromise;
    await fileChooser.setFiles(path.resolve('..', 'sample', sampleFilename));
}

async function confirmVisibleOnboardingDecision(page: any) {
    const editDetailsButton = page.locator('button:has-text("Edit Details")');
    const publishButton = page.locator('button:has-text("Publish to Market")');
    const saveAsNewButton = page.locator('button:has-text("Save as New Product")');
    if (await editDetailsButton.isVisible()) {
        await editDetailsButton.click();
    } else if (await publishButton.isEnabled()) {
        await publishButton.click();
    } else {
        await saveAsNewButton.click();
    }
}

test.describe('AI Vision Onboarding E2E', () => {
    test.describe.configure({ mode: 'serial' });

    test.beforeEach(async ({ page }) => {
        await loginAsAgent(page);
        await page.goto('/stock-master');
    });

    test('creates a new product and verifies list consistency + compliant sources (IMG_1484)', async ({ page }) => {
        const golden = goldenBySampleId.get('IMG_1484.jpg');
        const scanResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/ai/onboard/scan') && response.request().method() === 'POST'
        , { timeout: 120_000 });
        await openQuickAddAndUpload(page, 'IMG_1484.jpg');
        await expect(page.locator('[data-testid="ai-onboarding-steps"]')).toHaveAttribute('data-stage', 'SCANNING', { timeout: 10000 });
        const scanData = await extractData<any>(await scanResponsePromise);
        await expect(page.locator('input#name')).toHaveValue(scanData.name, { timeout: 30_000 });
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
        , { timeout: 60_000 });
        await confirmVisibleOnboardingDecision(page);
        const confirmData = await extractData<any>(await confirmResponsePromise);
        expect(confirmData.product?.id).toBeTruthy();
        expect(confirmData.product?.itemNumber).toBe(scanData.itemNumber);
        if (scanData.brand) {
            expect(confirmData.product?.brand).toBe(scanData.brand);
        }
        expect(confirmData.product?.basePrice).toBe(scanData.price);
        expect(['NEW_PRODUCT', 'EXISTING_PRODUCT']).toContain(confirmData.onboardingTrace?.resultDecision);
        expect(confirmData.onboardingTrace?.finalProductId).toBe(confirmData.product?.id);

        const listResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/mobile/products') && response.request().method() === 'GET'
        , { timeout: 60_000 });
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
        , { timeout: 120_000 });
        await openQuickAddAndUpload(page, 'IMG_1638.jpg');
        const scanData = await extractData<any>(await scanResponsePromise);
        expect(scanData.categoryId).toBe(golden?.expected?.categoryId);
        expect(scanData.itemNumber || scanData.name || scanData.price).toBeTruthy();
        if (REQUIRE_SEED_DEPENDENT_GOLDEN) {
            await expect(page.locator('[data-testid="ai-existing-product-alert"]')).toBeVisible({ timeout: 120_000 });
            expect(scanData.existingProductFound).toBeTruthy();
            expect(scanData.existingProductId).toBeTruthy();
        }

        const confirmResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/ai/onboard/confirm') && response.request().method() === 'POST'
        , { timeout: 60_000 });
        await confirmVisibleOnboardingDecision(page);
        const confirmData = await extractData<any>(await confirmResponsePromise);
        expect(confirmData.product?.id).toBeTruthy();
        if (scanData.existingProductFound) {
            expect(confirmData.product?.id).toBe(scanData.existingProductId);
            expect(confirmData.onboardingTrace?.resultDecision).toBe('EXISTING_PRODUCT');
            expect(confirmData.onboardingTrace?.finalProductId).toBe(scanData.existingProductId);
        } else {
            expect(['NEW_PRODUCT', 'DRAFT']).toContain(confirmData.onboardingTrace?.resultDecision);
            expect(confirmData.onboardingTrace?.finalProductId).toBe(confirmData.product?.id);
        }

        const listResponsePromise = page.waitForResponse((response) =>
            response.url().includes('/api/mobile/products') && response.request().method() === 'GET'
        , { timeout: 60_000 });
        await page.reload();
        const listData = await extractData<any[]>(await listResponsePromise);
        const expectedProductId = scanData.existingProductFound ? scanData.existingProductId : confirmData.product?.id;
        expect(listData.some((entry: any) => entry.product?.id === expectedProductId)).toBeTruthy();
    });
});

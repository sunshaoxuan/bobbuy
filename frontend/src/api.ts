import { message } from 'antd';
import { getStoredLocale, translate } from './i18n';

export type Metrics = {
    users: number;
    trips: number;
    orders: number;
    gmV: number;
    orderStatusCounts?: Record<string, number>;
    latencyP95Ms?: Record<string, number>;
    latencyP99Ms?: Record<string, number>;
    slowEndpoints?: string[];
};

export type Trip = {
    id: number;
    agentId: number;
    origin: string;
    destination: string;
    departDate: string;
    capacity: number;
    reservedCapacity: number;
    remainingCapacity?: number;
    status: string;
    statusUpdatedAt?: string;
    settlementFrozen?: boolean;
    settlementFreezeStage?: string;
    settlementFreezeReason?: string;
};

export type OrderLine = {
    id?: number;
    skuId: string;
    itemName: string;
    spec?: string;
    quantity: number;
    purchasedQuantity?: number;
    unitPrice: number;
};

export type Order = {
    id: number;
    businessId: string;
    customerId: number;
    tripId?: number | null;
    desiredDeliveryWindow?: string;
    status: string;
    statusUpdatedAt?: string;
    paymentMethod?: string;
    paymentStatus?: string;
    totalAmount: number;
    receiptConfirmedAt?: string;
    receiptConfirmedBy?: string;
    billingConfirmedAt?: string;
    billingConfirmedBy?: string;
    lines: OrderLine[];
};

export type User = {
    id: number;
    name: string;
    role: string;
    rating: number;
};

export type AiExtractedItem = {
    id: string;
    originalName: string;
    matchedName: string;
    quantity: number;
    note: string;
    price: number;
    confidence: number;
};

export type AiParseResponse = {
    items: AiExtractedItem[];
};

export type AiTranslateResponse = {
    translatedText: string;
};

export type CategoryAttributeTemplateField = {
    key: string;
    labelKey?: string;
    label?: string;
    type: 'text' | 'number' | 'select';
    options?: string[];
};

export type MobileCategory = {
    id: string;
    name: Record<string, string>;
    description?: Record<string, string>;
    attributeTemplate: CategoryAttributeTemplateField[];
};

export type MobileSupplier = {
    id: string;
    name: Record<string, string>;
    description?: Record<string, string>;
    contactInfo?: string;
};

export type MobileProductResponse = {
        product: {
            id: string;
            name: Record<string, string>;
        description?: Record<string, string>;
        brand?: string;
        basePrice: number;
        weight?: number;
        volume?: number;
        categoryId?: string;
            itemNumber?: string;
            isRecommended?: boolean;
            isTemporary?: boolean;
            visibilityStatus?: string;
            updatedAt?: string;
            storageCondition?: string;
        orderMethod?: string;
        mediaGallery?: { url: string; title?: Record<string, string>; type: string; sourceUrl?: string; sourceDomain?: string; sourceType?: string }[];
        priceTiers?: PriceTier[];
    };
    displayName: string;
    displayDescription: string;
    reconciledQuantity?: number;
    reconciledTripId?: number;
    allocatedBusinessIds?: string[];
    onboardingTrace?: {
        inputSampleId?: string;
        recognitionSummary?: string;
        sourceDomains?: string[];
        resultDecision?: string;
        finalProductId?: string;
    };
};

export type ProcurementHudStats = {
    tripId: number;
    totalEstimatedProfit: number;
    currentPurchasedAmount: number;
    currentFxRate: number;
    referenceFxRate: number;
    totalTripExpenses: number;
    currentWeight: number;
    currentVolume: number;
    categoryCompletionPercent: Record<string, number>;
    partnerShares: PartnerProfitShare[];
};

export type TripExpense = {
    id: number;
    tripId: number;
    cost: number;
    category: string;
    receiptThumbnailUrl?: string;
    ocrStatus?: string;
    createdAt: string;
};

export type PartnerProfitShare = {
    partnerRole: string;
    ratioPercent: number;
    amount: number;
};

export type ProfitSharingConfig = {
    tripId: number;
    purchaserRatioPercent: number;
    promoterRatioPercent: number;
    shares: PartnerProfitShare[];
};

export type LogisticsTracking = {
    id: number;
    tripId: number;
    trackingNumber: string;
    channel: string;
    provider: string;
    status: string;
    lastMessage: string;
    settlementReminderTriggered: boolean;
    lastCheckedAt: string;
};

export type ManualReconcileResponse = {
    skuId: string;
    fromBusinessId: string;
    toBusinessId: string;
    transferredQuantity: number;
};

export type CustomerBalanceLedgerEntry = {
    tripId: number;
    businessId: string;
    customerId: number;
    totalReceivable: number;
    paidDeposit: number;
    outstandingBalance: number;
    settlementStatus: string;
    settlementFrozen: boolean;
    settlementFreezeStage: string;
    settlementFreezeReason: string;
    receiptConfirmedAt?: string;
    receiptConfirmedBy?: string;
    billingConfirmedAt?: string;
    billingConfirmedBy?: string;
    orderLines: {
        skuId: string;
        itemName: string;
        orderedQuantity: number;
        unitPrice: number;
        purchasedQuantity: number;
        differenceNote: string;
    }[];
};

export type ProcurementReceipt = {
    id: number;
    tripId: number;
    fileName?: string;
    originalImageUrl?: string;
    thumbnailUrl?: string;
    processingStatus: string;
    uploadedAt: string;
    updatedAt?: string;
    reconciliationResult: {
        recognitionMode?: string;
        summary?: string;
        merchantName?: string;
        receiptDate?: string;
        currency?: string;
        receiptItems: Array<Record<string, any>>;
        matchedOrderLines: Array<Record<string, any>>;
        unmatchedReceiptItems: Array<Record<string, any>>;
        missingOrderedItems: Array<Record<string, any>>;
        selfUseItems: Array<Record<string, any>>;
        [key: string]: any;
    };
};

export type ProcurementItemResponse = {
    skuId: string;
    itemName: string;
    totalQuantity: number;
    purchasedQuantity: number;
    unitPrice: number;
    businessIds: string[];
};

export type ProcurementDeficitItemResponse = {
    skuId: string;
    itemName: string;
    deficitQuantity: number;
    completionPercent: number;
    priority: string;
    isTemporary: boolean;
    visibilityStatus: string;
};

export type ChatImageFlowStatus =
    | 'PENDING_CONFIRMATION'
    | 'MATCHED_EXISTING_PRODUCT'
    | 'CANDIDATE_SELECTED'
    | 'TEMP_PRODUCT_CREATED'
    | 'PUBLISHED_TO_MARKET'
    | 'PUBLISH_FAILED';

export type ChatRecoveryAction =
    | 'RETRY_PUBLISH'
    | 'REQUEST_ATTACHMENT_REUPLOAD'
    | 'RETRY_IMAGE_SCAN';

export type ChatCandidateSummary = {
    brand?: string;
    categoryId?: string;
    matchedFragments?: string[];
    aliasSources?: string[];
    presentedCount?: number;
    rejectedCount?: number;
};

export type ChatCandidateAudit = {
    decision: 'EXACT_MATCH' | 'SELECTED_CANDIDATE' | 'CREATED_TEMP_PRODUCT';
    selectedProductId?: string;
    recommendedProductIds?: string[];
    rejectedProductIds?: string[];
    selectedReason?: string;
    confirmedAt: string;
    reviewedBy?: string;
    triggerEntry?: string;
    publishedAt?: string;
};

export type ChatMessageMetadata = {
    conversationType?: 'PRIVATE' | 'ORDER' | 'TRIP';
    source?: 'CHAT_WIDGET' | 'SYSTEM';
    orderId?: number | null;
    tripId?: number | null;
    relatedOrderId?: number | null;
    relatedTripId?: number | null;
    url?: string;
    attachmentUrl?: string;
    attachmentName?: string;
    productId?: string;
    productName?: string;
    itemNumber?: string;
    visibilityStatus?: string;
    isTemporary?: boolean;
    existingProductFound?: boolean;
    matchedBy?: string;
    imageFlowStatus?: ChatImageFlowStatus;
    recoveryAction?: ChatRecoveryAction;
    decisionAt?: string;
    publishedAt?: string;
    operatorId?: string;
    auditVersion?: string;
    candidateSelectionResult?: string;
    candidateAudit?: ChatCandidateAudit;
    candidateSummary?: ChatCandidateSummary;
    candidateReason?: string;
    candidateReasons?: string[];
    [key: string]: any;
};

export type ChatMessage = {
    id?: number;
    orderId?: number | null;
    tripId?: number | null;
    senderId: string;
    recipientId: string;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'SYSTEM';
    metadata?: ChatMessageMetadata;
    createdAt?: string;
};

export type AiProductCandidate = {
    productId: string;
    displayName: string;
    itemNumber?: string;
    matchReason: string;
    matchSignals?: string[];
    score?: number;
    brand?: string;
    categoryId?: string;
    matchedFragments?: string[];
    aliasSources?: string[];
};

export type WalletSummary = {
    partnerId: string;
    balance: number;
    currency: string;
    updatedAt: string;
};

export type WalletTransaction = {
    id: number;
    partnerId: string;
    amount: number;
    type: string;
    tripId: number;
    createdAt: string;
};

export type ProductPatch = Partial<{
    name: Record<string, string | null>;
    description: Record<string, string | null>;
    brand: string;
    basePrice: number;
    weight: number;
    volume: number;
    mediaGallery: { url: string; title?: Record<string, string>; type: string }[];
    storageCondition: string;
    orderMethod: string;
    categoryId: string;
    merchantSkus: Record<string, string>;
    priceTiers: PriceTier[];
    isRecommended: boolean;
    isTemporary: boolean;
    visibilityStatus: string;
}>;

export type FinancialAuditLog = {
    id: number;
    tripId: number;
    actionType: string;
    operatorName: string;
    originalValue: string;
    modifiedValue: string;
    previousHash: string;
    currentHash: string;
    createdAt: string;
};

const fallbackStockCategories: MobileCategory[] = [
// ... (rest of file)
    {
        id: 'cat-1001',
        name: { 'zh-CN': '服装', 'ja-JP': '衣料品', 'en-US': 'Clothing' },
        description: { 'zh-CN': '服装类商品', 'ja-JP': '衣料品カテゴリ', 'en-US': 'Clothing products' },
        attributeTemplate: [
            { key: 'size', labelKey: 'stock.dynamic.size', type: 'select', options: ['XS', 'S', 'M', 'L', 'XL'] },
            { key: 'material', labelKey: 'stock.dynamic.material', type: 'text' },
            { key: 'color', labelKey: 'stock.dynamic.color', type: 'text' }
        ]
    },
    {
        id: 'cat-1000',
        name: { 'zh-CN': '食品', 'ja-JP': '食品', 'en-US': 'Food' },
        description: { 'zh-CN': '食品类商品', 'ja-JP': '食品カテゴリ', 'en-US': 'Food products' },
        attributeTemplate: [
            { key: 'shelfLifeDays', labelKey: 'stock.dynamic.shelf_life_days', type: 'number' },
            { key: 'storageTemp', labelKey: 'stock.dynamic.storage_temp', type: 'text' },
            { key: 'flavor', labelKey: 'stock.dynamic.flavor', type: 'text' }
        ]
    }
];

const fallback = {
    metrics: { users: 2, trips: 1, orders: 1, gmV: 65, orderStatusCounts: { CONFIRMED: 1 } },
    trips: [
        {
            id: 2000,
            agentId: 1000,
            origin: 'Tokyo',
            destination: 'Shanghai',
            departDate: new Date().toISOString().slice(0, 10),
            capacity: 6,
            reservedCapacity: 1,
            remainingCapacity: 5,
            status: 'PUBLISHED'
        }
    ],
    orders: [
        {
            id: 3000,
            businessId: '20260117001',
            customerId: 1001,
            tripId: 2000,
            status: 'CONFIRMED',
            paymentMethod: 'ALIPAY',
            paymentStatus: 'UNPAID',
            totalAmount: 65,
            lines: [
                {
                    skuId: 'SKU001',
                    itemName: 'Matcha Kit',
                    quantity: 2,
                    unitPrice: 32.5
                }
            ]
        }
    ],
    users: [
        { id: 1000, name: 'Aiko Tan', role: 'AGENT', rating: 4.8 },
        { id: 1001, name: 'Chen Li', role: 'CUSTOMER', rating: 4.6 }
    ],
    stockCategories: fallbackStockCategories,
    products: [
        {
            product: {
                id: 'prd-1000',
                name: { 'zh-CN': 'Organic Milk', 'ja-JP': 'Organic Milk', 'en-US': 'Organic Milk' },
                description: { 'zh-CN': 'Fresh milk', 'ja-JP': 'Fresh milk', 'en-US': 'Fresh milk' },
                brand: 'BOBBuy Select',
                basePrice: 12.99,
                categoryId: 'cat-1000',
                itemNumber: 'SKU-20934',
                mediaGallery: []
            },
            displayName: 'Organic Milk',
            displayDescription: 'Fresh milk'
        },
        {
            product: {
                id: 'prd-1001',
                name: { 'zh-CN': 'Fresh Spinach', 'ja-JP': 'Fresh Spinach', 'en-US': 'Fresh Spinach' },
                description: { 'zh-CN': 'Leafy greens', 'ja-JP': 'Leafy greens', 'en-US': 'Leafy greens' },
                brand: 'BOBBuy Select',
                basePrice: 4.49,
                categoryId: 'cat-1000',
                itemNumber: 'SKU-88210',
                mediaGallery: []
            },
            displayName: 'Fresh Spinach',
            displayDescription: 'Leafy greens'
        }
    ] as MobileProductResponse[],
    walletSummary: {
        partnerId: 'PURCHASER',
        balance: 1250.5,
        currency: 'CNY',
        updatedAt: new Date().toISOString()
    } as WalletSummary,
    walletTransactions: [
        {
            id: 9000,
            partnerId: 'PURCHASER',
            amount: 45.2,
            type: 'TRIP_PAYOUT',
            tripId: 2000,
            createdAt: new Date().toISOString()
        }
    ] as WalletTransaction[]
};

type ApiResponse<T> = {
    status: 'success' | 'error';
    data?: T;
    meta?: { total?: number };
};

type ApiErrorResponse = {
    status: 'error';
    errorCode?: string;
    message?: string;
};

const genericErrorMessage = () => translate(getStoredLocale(), 'errors.request_failed');

const isMockApiEnabled = () =>
    typeof window !== 'undefined' && window.localStorage.getItem('bobbuy_enable_mock_api') === 'true';

type ApiRole = 'CUSTOMER' | 'AGENT' | 'MERCHANT';
const ROLE_STORAGE_KEY = 'bobbuy_user_role';
const ROLE_TEST_INJECT_KEY = 'bobbuy_test_role';
const USER_STORAGE_KEY = 'bobbuy_user_id';
const USER_TEST_INJECT_KEY = 'bobbuy_test_user';

function getEffectiveRole(): ApiRole {
    if (typeof window === 'undefined') {
        return 'CUSTOMER';
    }
    const queryRole = new URLSearchParams(window.location.search).get('role');
    if (queryRole === 'CUSTOMER' || queryRole === 'AGENT' || queryRole === 'MERCHANT') {
        return queryRole;
    }
    const injectedRole = window.localStorage.getItem(ROLE_TEST_INJECT_KEY);
    if (injectedRole === 'CUSTOMER' || injectedRole === 'AGENT' || injectedRole === 'MERCHANT') {
        return injectedRole;
    }
    const storedRole = window.localStorage.getItem(ROLE_STORAGE_KEY);
    if (storedRole === 'CUSTOMER' || storedRole === 'AGENT' || storedRole === 'MERCHANT') {
        return storedRole;
    }
    return 'CUSTOMER';
}

function getEffectiveUser(): string | null {
    if (typeof window === 'undefined') {
        return null;
    }
    const queryUser = new URLSearchParams(window.location.search).get('user');
    if (queryUser && queryUser.trim()) {
        return queryUser.trim();
    }
    const injectedUser = window.localStorage.getItem(USER_TEST_INJECT_KEY);
    if (injectedUser && injectedUser.trim()) {
        return injectedUser.trim();
    }
    const storedUser = window.localStorage.getItem(USER_STORAGE_KEY);
    if (storedUser && storedUser.trim()) {
        return storedUser.trim();
    }
    return null;
}

function createRequestHeaders(initHeaders?: HeadersInit, withJsonContentType = false): Headers {
    const headers = new Headers(initHeaders);
    if (!headers.has('Accept-Language')) {
        headers.set('Accept-Language', getStoredLocale());
    }
    headers.set('X-BOBBUY-ROLE', getEffectiveRole());
    const user = getEffectiveUser();
    if (user) {
        headers.set('X-BOBBUY-USER', user);
    }
    if (withJsonContentType && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }
    return headers;
}

async function parseErrorMessage(response: Response): Promise<string> {
    try {
        const payload = (await response.json()) as ApiErrorResponse;
        if (payload?.message) {
            return payload.message;
        }
    } catch {
        // Ignore parse errors and fall back to status text.
    }
    return response.statusText || genericErrorMessage();
}

async function fetchJson<T>(url: string, fallbackValue: T, init?: RequestInit): Promise<T> {
    try {
        const response = await fetch(url, {
            ...init,
            headers: createRequestHeaders(init?.headers)
        });
        if (!response.ok) {
            const errorMessage = await parseErrorMessage(response);
            message.error(errorMessage);
            if (isMockApiEnabled()) {
                return fallbackValue;
            }
            throw new Error(errorMessage);
        }
        const payload = (await response.json()) as ApiResponse<T> | T;
        if (typeof payload === 'object' && payload !== null && 'status' in payload && 'data' in payload) {
            if (payload.data !== undefined) {
                return payload.data as T;
            }
            if (isMockApiEnabled()) {
                return fallbackValue;
            }
            throw new Error(genericErrorMessage());
        }
        return payload as T;
    } catch (error) {
        console.error(`[API ERROR] FETCH failure at ${url}:`, error);
        if (isMockApiEnabled()) {
            message.warning('[Mock API] Falling back to local sample data.');
            return fallbackValue;
        }
        const errorMessage = error instanceof Error ? error.message : genericErrorMessage();
        message.error(errorMessage);
        throw error instanceof Error ? error : new Error(errorMessage);
    }
}

async function postJson<TResponse, TBody>(url: string, body: TBody): Promise<TResponse> {
    const response = await fetch(url, {
        method: 'POST',
        headers: createRequestHeaders(undefined, true),
        body: JSON.stringify(body)
    });
    if (!response.ok) {
        console.error(`[API ERROR] POST failure at ${url} (Status: ${response.status})`);
        const errorMessage = await parseErrorMessage(response);
        message.error(errorMessage);
        throw new Error(errorMessage);
    }
    const payload = (await response.json()) as ApiResponse<TResponse> | TResponse;
    if (typeof payload === 'object' && payload !== null && 'status' in payload && 'data' in payload) {
        return (payload.data ?? ({} as TResponse)) as TResponse;
    }
    return payload as TResponse;
}

async function patchJson<TResponse, TBody>(url: string, body: TBody): Promise<TResponse> {
    const response = await fetch(url, {
        method: 'PATCH',
        headers: createRequestHeaders(undefined, true),
        body: JSON.stringify(body)
    });
    if (!response.ok) {
        const errorMessage = await parseErrorMessage(response);
        message.error(errorMessage);
        throw new Error(errorMessage);
    }
    const payload = (await response.json()) as ApiResponse<TResponse> | TResponse;
    if (typeof payload === 'object' && payload !== null && 'status' in payload && 'data' in payload) {
        return (payload.data ?? ({} as TResponse)) as TResponse;
    }
    return payload as TResponse;
}

export type PriceTier = {
    tierName: string;
    price: number;
    currency: string;
    agentOnly: boolean;
    note?: string;
    updatedAt?: string;
};

export type AiOnboardingSuggestion = {
    name: string;
    brand?: string;
    description?: string;
    price?: number;
    categoryId?: string;
    itemNumber?: string;
    storageCondition?: string;
    orderMethod?: string;
    mediaGallery?: { url: string; title?: Record<string, string>; type: string; visible?: boolean; sourceUrl?: string; sourceDomain?: string; sourceType?: string }[];
    attributes?: Record<string, string>;
    existingProductFound?: boolean;
    existingProductId?: string;
    similarProductCandidates?: AiProductCandidate[];
    visibilityStatus?: string;
    detectedPriceTiers?: PriceTier[];
    originalPhotoBase64?: string;
    inputSampleId?: string;
    recognitionSummary?: string;
    sourceDomains?: string[];
    rejectedSourceDomains?: string[];
    sourcePolicyVersion?: string;
    trace?: {
        inputSampleId?: string;
        recognitionSummary?: string;
        sourceDomains?: string[];
        resultDecision?: string;
        finalProductId?: string;
    };
};

export const api = {
    metrics: () => fetchJson<Metrics>('/api/metrics', fallback.metrics),
    trips: () => fetchJson<Trip[]>('/api/trips', fallback.trips),
    orders: (tripId?: number) =>
        fetchJson<Order[]>(
            typeof tripId === 'number' ? `/api/orders?tripId=${tripId}` : '/api/orders',
            fallback.orders
        ),
    users: () => fetchJson<User[]>('/api/users', fallback.users),
    createTrip: (trip: Omit<Trip, 'id' | 'statusUpdatedAt' | 'remainingCapacity'>) =>
        postJson<Trip, Omit<Trip, 'id' | 'statusUpdatedAt' | 'remainingCapacity'>>('/api/trips', trip),
    createOrder: (order: Omit<Order, 'id' | 'statusUpdatedAt' | 'totalAmount'>) =>
        postJson<Order, Omit<Order, 'id' | 'statusUpdatedAt' | 'totalAmount'>>('/api/orders', order),
    parseOrderText: (text: string) =>
        postJson<AiParseResponse, { text: string }>('/api/ai/parse', { text }),
    confirmAiMapping: (originalName: string, matchedName: string) =>
        postJson<void, { originalName: string; matchedName: string }>('/api/ai/experience/confirm', {
            originalName,
            matchedName
        }),
    updateOrderStatus: (orderId: number, status: string) =>
        patchJson<Order, { status: string }>(`/api/orders/${orderId}/status`, { status }),
    bulkUpdateTripOrderStatus: (tripId: number, targetStatus: string) =>
        patchJson<Order[], { targetStatus: string }>(`/api/trips/${tripId}/orders/bulk-status`, { targetStatus }),
    stockCategories: () => fetchJson<MobileCategory[]>('/api/mobile/categories', fallback.stockCategories),
    suppliers: () => fetchJson<MobileSupplier[]>('/api/mobile/suppliers', []),
    translate: (text: string, targetLocale: string) =>
        postJson<AiTranslateResponse, { text: string; targetLocale: string }>('/api/ai/translate', {
            text,
            targetLocale
        }),
    onboardScan: (base64Image: string, sampleId?: string) =>
        postJson<AiOnboardingSuggestion, { base64Image: string; sampleId?: string }>('/api/ai/onboard/scan', { base64Image, sampleId }),
    onboardConfirm: (suggestion: AiOnboardingSuggestion) =>
        postJson<MobileProductResponse, AiOnboardingSuggestion>('/api/ai/onboard/confirm', suggestion),
    products: () => fetchJson<MobileProductResponse[]>('/api/mobile/products', fallback.products),
    patchProduct: (id: string, patch: ProductPatch) =>
        patchJson<MobileProductResponse, ProductPatch>(`/api/mobile/products/${id}`, patch),
    procurementHud: (tripId: number) =>
        fetchJson<ProcurementHudStats>(`/api/procurement/${tripId}/hud`, {
            tripId,
            totalEstimatedProfit: 0,
            currentPurchasedAmount: 0,
            currentFxRate: 1,
            referenceFxRate: 1,
            totalTripExpenses: 0,
            currentWeight: 0,
            currentVolume: 0,
            categoryCompletionPercent: {},
            partnerShares: []
        }),
    procurementExpenses: (tripId: number) => fetchJson<TripExpense[]>(`/api/procurement/${tripId}/expenses`, []),
    createProcurementExpense: (
        tripId: number,
        payload: { cost: number; category: string; receiptImageBase64?: string }
    ) =>
        postJson<TripExpense, { cost: number; category: string; receiptImageBase64?: string }>(
            `/api/procurement/${tripId}/expenses`,
            payload
        ),
    expenseReceiptPreview: (tripId: number, expenseId: number) =>
        fetchJson<{ expenseId: number; previewUrl: string }>(
            `/api/procurement/${tripId}/expenses/${expenseId}/receipt-preview`,
            { expenseId, previewUrl: '' }
        ),
    procurementProfitSharing: (tripId: number) =>
        fetchJson<ProfitSharingConfig>(`/api/procurement/${tripId}/profit-sharing`, {
            tripId,
            purchaserRatioPercent: 70,
            promoterRatioPercent: 30,
            shares: []
        }),
    updateProcurementProfitSharing: (
        tripId: number,
        payload: { purchaserRatioPercent: number; promoterRatioPercent: number }
    ) =>
        patchJson<ProfitSharingConfig, { purchaserRatioPercent: number; promoterRatioPercent: number }>(
            `/api/procurement/${tripId}/profit-sharing`,
            payload
        ),
    procurementLogistics: (tripId: number) =>
        fetchJson<LogisticsTracking[]>(`/api/procurement/${tripId}/logistics`, []),
    createProcurementLogistics: (
        tripId: number,
        payload: { trackingNumber: string; channel: string; provider: string }
    ) =>
        postJson<LogisticsTracking, { trackingNumber: string; channel: string; provider: string }>(
            `/api/procurement/${tripId}/logistics`,
            payload
        ),
    refreshProcurementLogistics: (tripId: number, trackingId: number) =>
        postJson<LogisticsTracking, Record<string, never>>(
            `/api/procurement/${tripId}/logistics/${trackingId}/refresh`,
            {}
        ),
    procurementAuditLogs: (tripId: number) =>
        fetchJson<FinancialAuditLog[]>(`/api/procurement/${tripId}/audit-logs`, []),
    customerBalanceLedger: (tripId: number) =>
        fetchJson<CustomerBalanceLedgerEntry[]>(`/api/procurement/${tripId}/ledger`, []),
    confirmCustomerLedger: (
        tripId: number,
        businessId: string,
        action: 'RECEIPT' | 'BILLING'
    ) =>
        postJson<CustomerBalanceLedgerEntry, { action: 'RECEIPT' | 'BILLING' }>(
            `/api/procurement/${tripId}/ledger/${encodeURIComponent(businessId)}/confirm`,
            { action }
        ),
    manualReconcile: (
        tripId: number,
        payload: { skuId: string; fromBusinessId: string; toBusinessId: string; quantity: number }
    ) =>
        postJson<ManualReconcileResponse, { skuId: string; fromBusinessId: string; toBusinessId: string; quantity: number }>(
            `/api/procurement/${tripId}/manual-reconcile`,
            payload
        ),
    exportProcurementSettlement: async (tripId: number, format: 'csv' | 'pdf') => {
        const response = await fetch(`/api/procurement/${tripId}/export?format=${format}`, {
            headers: createRequestHeaders()
        });
        if (!response.ok) {
            const errorMessage = await parseErrorMessage(response);
            message.error(errorMessage);
            throw new Error(errorMessage);
        }
        return response.blob();
    },
    exportCustomerStatement: async (tripId: number, businessId: string) => {
        const response = await fetch(`/api/procurement/${tripId}/customers/${encodeURIComponent(businessId)}/statement`, {
            headers: createRequestHeaders()
        });
        if (!response.ok) {
            const errorMessage = await parseErrorMessage(response);
            message.error(errorMessage);
            throw new Error(errorMessage);
        }
        return response.blob();
    },
    finalizeProcurementSettlement: (tripId: number) =>
        postJson<void, Record<string, never>>(`/api/procurement/${tripId}/finalize-settlement`, {}),
    procurementReceipts: (tripId: number) =>
        fetchJson<ProcurementReceipt[]>(`/api/procurement/${tripId}/receipts`, []),
    uploadProcurementReceipts: (
        tripId: number,
        payload: { receipts: Array<{ imageBase64: string; fileName?: string }> }
    ) =>
        postJson<ProcurementReceipt[], { receipts: Array<{ imageBase64: string; fileName?: string }> }>(
            `/api/procurement/${tripId}/receipts`,
            payload
        ),
    saveProcurementReceipt: (
        tripId: number,
        receiptId: number,
        payload: { processingStatus?: string; reconciliationResult: ProcurementReceipt['reconciliationResult'] }
    ) =>
        patchJson<ProcurementReceipt, { processingStatus?: string; reconciliationResult: ProcurementReceipt['reconciliationResult'] }>(
            `/api/procurement/${tripId}/receipts/${receiptId}`,
            payload
        ),
    getWallet: (partnerId: string) =>
        fetchJson<WalletSummary>(`/api/procurement/wallets/${partnerId}`, fallback.walletSummary),
    getWalletTransactions: (partnerId: string) =>
        fetchJson<WalletTransaction[]>(`/api/procurement/wallets/${partnerId}/transactions`, fallback.walletTransactions),
    quickOrder: (tripId: number, payload: { skuId: string; quantity: number; businessId: string }) =>
        postJson<Order, { skuId: string; quantity: number; businessId: string }>(`/api/orders/${tripId}/quick-order`, payload),
    getFinancialAuditLogs: (tripId: number) =>
        fetchJson<FinancialAuditLog[]>(`/api/financial/audit/${tripId}`, []),
    checkFinancialAuditIntegrity: (tripId: number) =>
        fetchJson<{ isValid: boolean }>(`/api/financial/audit/${tripId}/check-integrity`, { isValid: false }),
    procurementDeficitItems: (tripId: number) =>
        fetchJson<ProcurementDeficitItemResponse[]>(`/api/procurement/${tripId}/deficit`, []),
    procurementList: (tripId: number) =>
        fetchJson<ProcurementItemResponse[]>(`/api/trips/${tripId}/procurement-list`, []),
    sendChatMessage: (message: ChatMessage) =>
        postJson<ChatMessage, ChatMessage>('/api/chat/send', message),
    getOrderChat: (orderId: number) =>
        fetchJson<ChatMessage[]>(`/api/chat/orders/${orderId}`, []),
    getTripChat: (tripId: number) =>
        fetchJson<ChatMessage[]>(`/api/chat/trips/${tripId}`, []),
    getPrivateChat: (userA: string, userB: string) =>
        fetchJson<ChatMessage[]>(`/api/chat/private?userA=${userA}&userB=${userB}`, [])
};

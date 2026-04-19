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
            updatedAt?: string;
            storageCondition?: string;
        orderMethod?: string;
        mediaGallery?: { url: string; title?: Record<string, string>; type: string }[];
        priceTiers?: PriceTier[];
    };
    displayName: string;
    displayDescription: string;
    reconciledQuantity?: number;
    reconciledTripId?: number;
    allocatedBusinessIds?: string[];
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
    businessId: string;
    customerId: number;
    totalReceivable: number;
    paidDeposit: number;
    outstandingBalance: number;
};

export type ProcurementItemResponse = {
    skuId: string;
    itemName: string;
    totalQuantity: number;
    purchasedQuantity: number;
    unitPrice: number;
    customerIdList: number[];
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

export type ChatMessage = {
    id?: number;
    orderId?: number | null;
    senderId: string;
    recipientId: string;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'SYSTEM';
    metadata?: Record<string, any>;
    createdAt?: string;
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

async function fetchJson<T>(url: string, fallbackValue: T): Promise<T> {
    try {
        const response = await fetch(url, {
            headers: { 'Accept-Language': getStoredLocale() }
        });
        if (!response.ok) {
            const errorMessage = await parseErrorMessage(response);
            message.error(errorMessage);
            return fallbackValue;
        }
        const payload = (await response.json()) as ApiResponse<T> | T;
        if (typeof payload === 'object' && payload !== null && 'status' in payload && 'data' in payload) {
            return (payload.data ?? fallbackValue) as T;
        }
        return payload as T;
    } catch (error) {
        console.error(`[API ERROR] FETCH failure at ${url}:`, error);
        message.error(genericErrorMessage());
        return fallbackValue;
    }
}

async function postJson<TResponse, TBody>(url: string, body: TBody): Promise<TResponse> {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept-Language': getStoredLocale()
        },
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
        headers: {
            'Content-Type': 'application/json',
            'Accept-Language': getStoredLocale()
        },
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
    mediaGallery?: { url: string; title?: Record<string, string>; type: string; visible?: boolean }[];
    attributes?: Record<string, string>;
    existingProductFound?: boolean;
    existingProductId?: string;
    detectedPriceTiers?: PriceTier[];
    originalPhotoBase64?: string;
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
    onboardScan: (base64Image: string) =>
        postJson<AiOnboardingSuggestion, { base64Image: string }>('/api/ai/onboard/scan', { base64Image }),
    onboardConfirm: (suggestion: AiOnboardingSuggestion) =>
        postJson<MobileProductResponse, AiOnboardingSuggestion>('/api/ai/onboard/confirm', suggestion),
    products: () => fetchJson<MobileProductResponse[]>('/api/mobile/products', fallback.products),
    patchProduct: (id: string, patch: Partial<Product>) =>
        fetchJson<MobileProductResponse>(`/api/mobile/products/${id}`, {}, { method: 'PATCH', body: JSON.stringify(patch) }),
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
            headers: { 'Accept-Language': getStoredLocale() }
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
            headers: { 'Accept-Language': getStoredLocale() }
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
    getPrivateChat: (userA: string, userB: string) =>
        fetchJson<ChatMessage[]>(`/api/chat/private?userA=${userA}&userB=${userB}`, [])
};

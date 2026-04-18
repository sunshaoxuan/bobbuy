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
    unitPrice: number;
};

export type Order = {
    id: number;
    businessId: string;
    customerId: number;
    tripId: number;
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

export type AiTranslateResponse = {
    translatedText: string;
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
    stockCategories: fallbackStockCategories
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
    mediaGallery?: { url: string; title?: string; type: string }[];
    attributes?: Record<string, string>;
    existingProductFound?: boolean;
    existingProductId?: string;
    detectedPriceTiers?: PriceTier[];
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
        postJson<unknown, AiOnboardingSuggestion>('/api/ai/onboard/confirm', suggestion)
};

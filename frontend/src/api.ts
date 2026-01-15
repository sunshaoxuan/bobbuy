import { message } from 'antd';
import { getStoredLocale, translate } from './i18n';

export type Metrics = {
  users: number;
  trips: number;
  orders: number;
  gmV: number;
  orderStatusCounts?: Record<string, number>;
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

export type Order = {
  id: number;
  customerId: number;
  tripId: number;
  itemName: string;
  quantity: number;
  unitPrice: number;
  serviceFee: number;
  estimatedTax: number;
  currency: string;
  status: string;
  statusUpdatedAt?: string;
};

export type User = {
  id: number;
  name: string;
  role: string;
  rating: number;
};

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
      customerId: 1001,
      tripId: 2000,
      itemName: 'Matcha Kit',
      quantity: 2,
      unitPrice: 32.5,
      serviceFee: 6,
      estimatedTax: 2.3,
      currency: 'CNY',
      status: 'CONFIRMED'
    }
  ],
  users: [
    { id: 1000, name: 'Aiko Tan', role: 'AGENT', rating: 4.8 },
    { id: 1001, name: 'Chen Li', role: 'CUSTOMER', rating: 4.6 }
  ]
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
  } catch {
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

export const api = {
  metrics: () => fetchJson<Metrics>('/api/metrics', fallback.metrics),
  trips: () => fetchJson<Trip[]>('/api/trips', fallback.trips),
  orders: () => fetchJson<Order[]>('/api/orders', fallback.orders),
  users: () => fetchJson<User[]>('/api/users', fallback.users),
  createTrip: (trip: Omit<Trip, 'id' | 'statusUpdatedAt' | 'remainingCapacity'>) =>
    postJson<Trip, Omit<Trip, 'id' | 'statusUpdatedAt' | 'remainingCapacity'>>('/api/trips', trip),
  createOrder: (order: Omit<Order, 'id' | 'statusUpdatedAt'>) =>
    postJson<Order, Omit<Order, 'id' | 'statusUpdatedAt'>>('/api/orders', order)
};

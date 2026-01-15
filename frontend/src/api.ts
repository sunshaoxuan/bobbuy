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

async function fetchJson<T>(url: string, fallbackValue: T): Promise<T> {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      return fallbackValue;
    }
    const payload = (await response.json()) as ApiResponse<T> | T;
    if (typeof payload === 'object' && payload !== null && 'status' in payload && 'data' in payload) {
      return (payload.data ?? fallbackValue) as T;
    }
    return payload as T;
  } catch {
    return fallbackValue;
  }
}

export const api = {
  metrics: () => fetchJson<Metrics>('/api/metrics', fallback.metrics),
  trips: () => fetchJson<Trip[]>('/api/trips', fallback.trips),
  orders: () => fetchJson<Order[]>('/api/orders', fallback.orders),
  users: () => fetchJson<User[]>('/api/users', fallback.users)
};

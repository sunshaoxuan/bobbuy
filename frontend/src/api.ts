export type Metrics = {
  users: number;
  trips: number;
  orders: number;
  gmV: number;
};

export type Trip = {
  id: number;
  agentId: number;
  origin: string;
  destination: string;
  departDate: string;
  capacity: number;
  status: string;
};

export type Order = {
  id: number;
  customerId: number;
  tripId: number;
  itemName: string;
  quantity: number;
  unitPrice: number;
  status: string;
};

export type User = {
  id: number;
  name: string;
  role: string;
  rating: number;
};

const fallback = {
  metrics: { users: 2, trips: 1, orders: 1, gmV: 65 },
  trips: [
    {
      id: 2000,
      agentId: 1000,
      origin: 'Tokyo',
      destination: 'Shanghai',
      departDate: new Date().toISOString().slice(0, 10),
      capacity: 6,
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
      status: 'CONFIRMED'
    }
  ],
  users: [
    { id: 1000, name: 'Aiko Tan', role: 'AGENT', rating: 4.8 },
    { id: 1001, name: 'Chen Li', role: 'CUSTOMER', rating: 4.6 }
  ]
};

async function fetchJson<T>(url: string, fallbackValue: T): Promise<T> {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      return fallbackValue;
    }
    return (await response.json()) as T;
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

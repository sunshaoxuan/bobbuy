import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useChatWebSocket } from './useChatWebSocket';

const websocketMocks = vi.hoisted(() => {
  const clients: Array<{
    active: boolean;
    config: Record<string, any>;
    subscribe: ReturnType<typeof vi.fn>;
    deactivate: ReturnType<typeof vi.fn>;
    activate: ReturnType<typeof vi.fn>;
    configure: ReturnType<typeof vi.fn>;
  }> = [];

  class MockClient {
    public active = false;
    public config: Record<string, any>;
    public subscribe = vi.fn(() => ({ unsubscribe: vi.fn() }));
    public deactivate = vi.fn(async () => {
      this.active = false;
    });
    public activate = vi.fn(() => {
      this.active = true;
    });
    public configure = vi.fn((nextConfig: Record<string, any>) => {
      this.config = { ...this.config, ...nextConfig };
    });

    constructor(config: Record<string, any>) {
      this.config = config;
      clients.push(this);
    }
  }

  return { MockClient, clients };
});

vi.mock('@stomp/stompjs', () => ({
  Client: websocketMocks.MockClient
}));

describe('useChatWebSocket', () => {
  beforeEach(() => {
    websocketMocks.clients.length = 0;
    window.localStorage.clear();
    window.localStorage.setItem('bobbuy_locale', 'en-US');
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('connects with bearer token in STOMP headers', () => {
    window.localStorage.setItem('bobbuy_access_token', 'token-123');

    renderHook(() =>
      useChatWebSocket({
        enabled: true,
        destination: '/topic/trip/2000'
      })
    );

    expect(websocketMocks.clients).toHaveLength(1);
    expect(websocketMocks.clients[0].config.connectHeaders).toEqual({
      Authorization: 'Bearer token-123'
    });
    expect(websocketMocks.clients[0].activate).toHaveBeenCalled();
  });

  it('degrades to REST-only path when no token exists', () => {
    renderHook(() =>
      useChatWebSocket({
        enabled: true,
        destination: '/topic/trip/2000'
      })
    );

    expect(websocketMocks.clients).toHaveLength(0);
  });

  it('stops reconnecting after auth rejection', async () => {
    window.localStorage.setItem('bobbuy_access_token', 'token-123');

    renderHook(() =>
      useChatWebSocket({
        enabled: true,
        destination: '/topic/trip/2000'
      })
    );

    websocketMocks.clients[0].config.onStompError?.({});
    websocketMocks.clients[0].config.onWebSocketClose?.({});

    await waitFor(() => {
      expect(websocketMocks.clients[0].configure).toHaveBeenCalledWith({ reconnectDelay: 0 });
      expect(websocketMocks.clients[0].deactivate).toHaveBeenCalled();
    });
  });
});

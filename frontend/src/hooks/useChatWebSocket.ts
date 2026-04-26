import { useEffect } from 'react';
import { Client } from '@stomp/stompjs';

type ConnectionEvent = {
  reconnected: boolean;
};

interface UseChatWebSocketOptions {
  enabled: boolean;
  destination?: string;
  onConnect?: (event: ConnectionEvent) => void;
  onMessage?: () => void;
}

export function useChatWebSocket({ enabled, destination, onConnect, onMessage }: UseChatWebSocketOptions) {
  useEffect(() => {
    if (!enabled || !destination || typeof window === 'undefined' || !isChatWebSocketAllowed()) {
      return;
    }

    let subscription: ReturnType<Client['subscribe']> | undefined;
    let hasConnected = false;
    const client = new Client({
      brokerURL: buildWebSocketUrl(),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
      onConnect: () => {
        subscription?.unsubscribe();
        subscription = client.subscribe(destination, () => {
          onMessage?.();
        });
        onConnect?.({ reconnected: hasConnected });
        hasConnected = true;
      }
    });

    client.activate();

    return () => {
      subscription?.unsubscribe();
      void client.deactivate();
    };
  }, [destination, enabled, onConnect, onMessage]);
}

function buildWebSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

function isChatWebSocketAllowed() {
  return window.localStorage.getItem('bobbuy_disable_chat_websocket') !== 'true';
}

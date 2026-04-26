import { useEffect, useRef } from 'react';
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
  const onConnectRef = useRef(onConnect);
  const onMessageRef = useRef(onMessage);

  useEffect(() => {
    onConnectRef.current = onConnect;
    onMessageRef.current = onMessage;
  }, [onConnect, onMessage]);

  useEffect(() => {
    if (!enabled || !destination || typeof window === 'undefined' || !isChatWebSocketAllowed()) {
      return;
    }

    let subscription: ReturnType<Client['subscribe']> | undefined;
    let hasConnected = false;
    let reconnectDelay = 1000;
    const client = new Client({
      brokerURL: buildWebSocketUrl(),
      reconnectDelay,
      heartbeatIncoming: 5000,
      heartbeatOutgoing: 5000,
      debug: () => undefined,
      onConnect: () => {
        subscription?.unsubscribe();
        subscription = client.subscribe(destination, () => {
          onMessageRef.current?.();
        });
        reconnectDelay = 1000;
        client.configure({ reconnectDelay });
        onConnectRef.current?.({ reconnected: hasConnected });
        hasConnected = true;
      },
      onWebSocketClose: () => {
        reconnectDelay = Math.min(reconnectDelay * 2, 5000);
        client.configure({ reconnectDelay });
      }
    });

    const handleOnline = () => {
      reconnectDelay = 250;
      client.configure({ reconnectDelay });
      if (client.active) {
        void client.deactivate().finally(() => client.activate());
        return;
      }
      client.activate();
    };

    const handleOffline = () => {
      void client.deactivate();
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    client.activate();

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
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

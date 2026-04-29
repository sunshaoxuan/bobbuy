import { useEffect, useRef, useSyncExternalStore } from 'react';
import { Client } from '@stomp/stompjs';
import { getStoredAccessToken, subscribeToAuthChanges } from '../authStorage';
import { refreshAuthSession } from '../api';

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
  const INITIAL_RECONNECT_DELAY_MS = 1000;
  const onConnectRef = useRef(onConnect);
  const onMessageRef = useRef(onMessage);
  const accessToken = useSyncExternalStore(subscribeToAuthChanges, getStoredAccessToken, () => null);

  useEffect(() => {
    onConnectRef.current = onConnect;
    onMessageRef.current = onMessage;
  }, [onConnect, onMessage]);

  useEffect(() => {
    if (!enabled || !destination || !accessToken || typeof window === 'undefined' || !isChatWebSocketAllowed()) {
      return;
    }

    let subscription: ReturnType<Client['subscribe']> | undefined;
    let hasConnected = false;
    let authRejected = false;
    let refreshInFlight = false;
    let currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;
    const client = new Client({
      brokerURL: buildWebSocketUrl(),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      reconnectDelay: currentReconnectDelay,
      heartbeatIncoming: 5000,
      heartbeatOutgoing: 5000,
      debug: () => undefined,
      onConnect: () => {
        authRejected = false;
        subscription?.unsubscribe();
        subscription = client.subscribe(destination, () => {
          onMessageRef.current?.();
        });
        currentReconnectDelay = INITIAL_RECONNECT_DELAY_MS;
        client.configure({ reconnectDelay: currentReconnectDelay });
        onConnectRef.current?.({ reconnected: hasConnected });
        hasConnected = true;
      },
      onStompError: () => {
        if (refreshInFlight) {
          return;
        }
        refreshInFlight = true;
        authRejected = true;
        void refreshAuthSession()
          .then((session) => {
            if (session) {
              authRejected = false;
            }
          })
          .finally(() => {
            refreshInFlight = false;
            void client.deactivate();
          });
      },
      onWebSocketClose: () => {
        if (authRejected || refreshInFlight) {
          return;
        }
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, 5000);
        client.configure({ reconnectDelay: currentReconnectDelay });
      }
    });

    const handleOnline = () => {
      currentReconnectDelay = 250;
      client.configure({ reconnectDelay: currentReconnectDelay });
      if (client.active) {
        // Network switches often leave the existing socket half-open, so force a fresh STOMP session.
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
  }, [accessToken, destination, enabled, onConnect, onMessage]);
}

function buildWebSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

function isChatWebSocketAllowed() {
  return window.localStorage.getItem('bobbuy_disable_chat_websocket') !== 'true';
}

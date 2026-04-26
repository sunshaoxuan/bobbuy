import { useEffect, useMemo, useState } from 'react';
import type { ChatMessage } from '../api';

type PersistedChatState = {
  messages: ChatMessage[];
  pendingMessages: ChatMessage[];
  inputValue: string;
  unreadCount: number;
  lastSuccessfulSyncAt?: string;
};

const DEFAULT_STATE: PersistedChatState = {
  messages: [],
  pendingMessages: [],
  inputValue: '',
  unreadCount: 0
};

export function useChatPersistence(conversationKey: string) {
  const storageKey = useMemo(() => `bobbuy_chat_${conversationKey}`, [conversationKey]);
  const [state, setState] = useState<PersistedChatState>(() => readState(storageKey));

  useEffect(() => {
    setState(readState(storageKey));
  }, [storageKey]);

  useEffect(() => {
    const handle = window.setTimeout(() => {
      window.localStorage.setItem(storageKey, JSON.stringify(state));
    }, 300);
    return () => window.clearTimeout(handle);
  }, [state, storageKey]);

  return {
    persistedState: state,
    setMessages: (messages: ChatMessage[] | ((value: ChatMessage[]) => ChatMessage[])) =>
      setState((current) => ({
        ...current,
        messages: typeof messages === 'function' ? messages(current.messages) : messages
      })),
    setPendingMessages: (pendingMessages: ChatMessage[] | ((value: ChatMessage[]) => ChatMessage[])) =>
      setState((current) => ({
        ...current,
        pendingMessages: typeof pendingMessages === 'function' ? pendingMessages(current.pendingMessages) : pendingMessages
      })),
    setInputValue: (inputValue: string | ((value: string) => string)) =>
      setState((current) => ({
        ...current,
        inputValue: typeof inputValue === 'function' ? inputValue(current.inputValue) : inputValue
      })),
    setUnreadCount: (unreadCount: number | ((value: number) => number)) =>
      setState((current) => ({
        ...current,
        unreadCount: typeof unreadCount === 'function' ? unreadCount(current.unreadCount) : unreadCount
      })),
    setLastSuccessfulSyncAt: (lastSuccessfulSyncAt?: string) =>
      setState((current) => ({ ...current, lastSuccessfulSyncAt }))
  };
}

function readState(storageKey: string): PersistedChatState {
  if (typeof window === 'undefined') {
    return DEFAULT_STATE;
  }
  try {
    const raw = window.localStorage.getItem(storageKey);
    if (!raw) {
      return DEFAULT_STATE;
    }
    const parsed = JSON.parse(raw) as Partial<PersistedChatState>;
    return {
      messages: Array.isArray(parsed.messages) ? parsed.messages : [],
      pendingMessages: Array.isArray(parsed.pendingMessages) ? parsed.pendingMessages : [],
      inputValue: typeof parsed.inputValue === 'string' ? parsed.inputValue : '',
      unreadCount: typeof parsed.unreadCount === 'number' ? parsed.unreadCount : 0,
      lastSuccessfulSyncAt: typeof parsed.lastSuccessfulSyncAt === 'string' ? parsed.lastSuccessfulSyncAt : undefined
    };
  } catch {
    return DEFAULT_STATE;
  }
}

import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { Dictionary } from './locales/types';
import zhCN from './locales/zh-CN';
import enUS from './locales/en-US';
import jaJP from './locales/ja-JP';

export type Locale = 'zh-CN' | 'ja-JP' | 'en-US';

const STORAGE_KEY = 'bobbuy_locale';
const DEFAULT_LOCALE: Locale = 'zh-CN';
const LOCALE_FALLBACKS: Locale[] = ['zh-CN', 'en-US'];

const translations: Record<Locale, Dictionary> = {
  'zh-CN': zhCN,
  'ja-JP': jaJP,
  'en-US': enUS
};

export const supportedLocales: Locale[] = ['zh-CN', 'ja-JP', 'en-US'];

export const getStoredLocale = (): Locale => {
  if (typeof window === 'undefined') {
    return DEFAULT_LOCALE;
  }
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return supportedLocales.includes(stored as Locale) ? (stored as Locale) : DEFAULT_LOCALE;
};

export const translate = (locale: Locale, key: string): string => {
  const localized = translations[locale][key];
  if (localized) {
    return localized;
  }
  for (const fallbackLocale of LOCALE_FALLBACKS) {
    const fallbackValue = translations[fallbackLocale][key];
    if (fallbackValue) {
      return fallbackValue;
    }
  }
  return key;
};

type I18nContextValue = {
  locale: Locale;
  setLocale: (next: Locale) => void;
  t: (key: string) => string;
};

const I18nContext = createContext<I18nContextValue | undefined>(undefined);

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(getStoredLocale());

  const setLocale = useCallback((next: Locale) => {
    setLocaleState(next);
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(STORAGE_KEY, next);
    }
  }, []);

  const t = useCallback((key: string) => translate(locale, key), [locale]);
  const value = useMemo(() => ({ locale, setLocale, t }), [locale, setLocale, t]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within I18nProvider');
  }
  return context;
}

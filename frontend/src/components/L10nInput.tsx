import React, { useEffect, useMemo, useState } from 'react';
import { Button, Input, Space, Typography } from 'antd';

const { Text } = Typography;

export type L10nValues = Record<string, string>;

type TranslateSuggestionFn = (sourceText: string, sourceLocale: string, targetLocale: string) => Promise<string>;

export interface L10nInputProps {
  value?: L10nValues;
  onChange?: (next: L10nValues) => void;
  locales?: string[];
  defaultLocale?: string;
  placeholder?: string;
  requestTranslation?: TranslateSuggestionFn;
  disabled?: boolean;
  loadingSuggestionText?: string;
  suggestionPrefixText?: string;
  applySuggestionText?: string;
}

const DEFAULT_LOCALES = ['zh-CN', 'en-US'];

const getDisplayValue = (value: string | undefined): string => value ?? '';

export default function L10nInput({
  value = {},
  onChange,
  locales = DEFAULT_LOCALES,
  defaultLocale = 'zh-CN',
  placeholder,
  requestTranslation,
  disabled,
  loadingSuggestionText = 'AI suggestion is generating...',
  suggestionPrefixText = 'AI Suggestion',
  applySuggestionText = 'Apply'
}: L10nInputProps) {
  const [activeLocale, setActiveLocale] = useState(defaultLocale);
  const [suggestions, setSuggestions] = useState<Record<string, string>>({});
  const [loadingLocale, setLoadingLocale] = useState<string>();

  const normalizedLocales = useMemo(() => {
    if (!locales.length) {
      return DEFAULT_LOCALES;
    }
    return locales;
  }, [locales]);

  useEffect(() => {
    if (!normalizedLocales.includes(activeLocale)) {
      setActiveLocale(normalizedLocales[0]);
    }
  }, [activeLocale, normalizedLocales]);

  useEffect(() => {
    if (!requestTranslation) {
      return;
    }

    const activeValue = value[activeLocale]?.trim();
    if (activeValue) {
      return;
    }

    if (suggestions[activeLocale]) {
      return;
    }

    const sourceLocale = normalizedLocales.find((locale) => locale !== activeLocale && value[locale]?.trim());
    if (!sourceLocale) {
      return;
    }
    const sourceText = value[sourceLocale]?.trim();
    if (!sourceText) {
      return;
    }

    let isMounted = true;
    setLoadingLocale(activeLocale);
    requestTranslation(sourceText, sourceLocale, activeLocale)
      .then((translated) => {
        if (!isMounted) {
          return;
        }
        const next = translated?.trim();
        if (!next) {
          return;
        }
        setSuggestions((prev) => ({ ...prev, [activeLocale]: next }));
      })
      .catch((error) => {
        console.debug('L10nInput suggestion failed', error);
      })
      .finally(() => {
        if (isMounted) {
          setLoadingLocale(undefined);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [activeLocale, normalizedLocales, requestTranslation, suggestions, value]);

  const updateLocaleValue = (locale: string, nextValue: string) => {
    const next = { ...value, [locale]: nextValue };
    onChange?.(next);
    if (nextValue.trim()) {
      setSuggestions((prev) => {
        if (!prev[locale]) {
          return prev;
        }
        const next = { ...prev };
        delete next[locale];
        return next;
      });
    }
  };

  const applySuggestion = () => {
    const suggestion = suggestions[activeLocale];
    if (!suggestion) {
      return;
    }
    updateLocaleValue(activeLocale, suggestion);
  };

  const currentValue = getDisplayValue(value[activeLocale]);
  const suggestion = suggestions[activeLocale];
  const isLoadingSuggestion = loadingLocale === activeLocale;

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={8}>
      <Space wrap>
        {normalizedLocales.map((locale) => (
          <Button
            key={locale}
            size="small"
            type={locale === activeLocale ? 'primary' : 'default'}
            onClick={() => setActiveLocale(locale)}
            disabled={disabled}
          >
            {locale}
          </Button>
        ))}
      </Space>

      <Input
        data-testid="l10n-input-field"
        value={currentValue}
        onChange={(event) => updateLocaleValue(activeLocale, event.target.value)}
        placeholder={placeholder}
        disabled={disabled}
      />

      {!currentValue && (suggestion || isLoadingSuggestion) ? (
        <Space size={8}>
          <Text type="secondary" italic>
            {isLoadingSuggestion ? loadingSuggestionText : `${suggestionPrefixText}: ${suggestion}`}
          </Text>
          {!isLoadingSuggestion ? (
            <Button type="link" size="small" onClick={applySuggestion} style={{ paddingInline: 0 }}>
              {applySuggestionText}
            </Button>
          ) : null}
        </Space>
      ) : null}
    </Space>
  );
}

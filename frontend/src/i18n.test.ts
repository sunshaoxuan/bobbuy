import { describe, it, expect } from 'vitest';
import zhCN from './locales/zh-CN';
import enUS from './locales/en-US';
import jaJP from './locales/ja-JP';
import { translate } from './i18n';

const BARE_KEY_REGEX = /^[a-z][a-z0-9]*(\.[a-z][a-z0-9_]*)+$/;

describe('i18n locale coverage', () => {
  const zhKeys = Object.keys(zhCN);
  const enKeys = Object.keys(enUS);
  const jaKeys = Object.keys(jaJP);

  it('zh-CN has no empty translation values', () => {
    const empty = zhKeys.filter((k) => !zhCN[k] || zhCN[k].trim() === '');
    expect(empty).toEqual([]);
  });

  it('en-US has no empty translation values', () => {
    const empty = enKeys.filter((k) => !enUS[k] || enUS[k].trim() === '');
    expect(empty).toEqual([]);
  });

  it('ja-JP has no empty translation values', () => {
    const empty = jaKeys.filter((k) => !jaJP[k] || jaJP[k].trim() === '');
    expect(empty).toEqual([]);
  });

  it('translate() falls back to zh-CN for keys missing in en-US', () => {
    const zhOnlyKey = zhKeys.find((k) => !(k in enUS));
    if (!zhOnlyKey) {
      // All keys exist in en-US – still a valid pass
      return;
    }
    const result = translate('en-US', zhOnlyKey);
    expect(result).toBe(zhCN[zhOnlyKey]);
    expect(result).not.toMatch(BARE_KEY_REGEX);
  });

  it('translate() falls back to zh-CN for keys missing in ja-JP', () => {
    const zhOnlyKey = zhKeys.find((k) => !(k in jaJP));
    if (!zhOnlyKey) {
      return;
    }
    const result = translate('ja-JP', zhOnlyKey);
    expect(result).toBe(zhCN[zhOnlyKey]);
    expect(result).not.toMatch(BARE_KEY_REGEX);
  });

  it('translate() returns the key itself only when the key is not in any locale', () => {
    const missing = 'this.key.does.not.exist';
    expect(translate('zh-CN', missing)).toBe(missing);
    expect(translate('en-US', missing)).toBe(missing);
    expect(translate('ja-JP', missing)).toBe(missing);
  });

  it('all zh-CN nav keys exist in en-US (no bare keys on nav)', () => {
    const navKeys = zhKeys.filter((k) => k.startsWith('nav.'));
    const missing = navKeys.filter((k) => !(k in enUS));
    expect(missing).toEqual([]);
  });

  it('all zh-CN stock keys exist in en-US (no bare keys on stock page)', () => {
    const stockKeys = zhKeys.filter((k) => k.startsWith('stock.'));
    const missing = stockKeys.filter((k) => !(k in enUS));
    expect(missing).toEqual([]);
  });

  it('translate() zh-CN never returns a raw dot-separated key for known keys', () => {
    const rawKeys = zhKeys.filter((k) => {
      const translated = translate('zh-CN', k);
      return BARE_KEY_REGEX.test(translated);
    });
    expect(rawKeys).toEqual([]);
  });

  it('translate() en-US never returns a raw dot-separated key for known zh-CN keys', () => {
    const rawKeys = zhKeys.filter((k) => {
      const translated = translate('en-US', k);
      return BARE_KEY_REGEX.test(translated);
    });
    expect(rawKeys).toEqual([]);
  });
});

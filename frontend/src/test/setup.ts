import { afterEach, expect } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

expect.extend(matchers);

afterEach(() => {
  cleanup();
});

if (!window.matchMedia) {
  window.matchMedia = (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false
  });
}

const suppressedWarnings = [/not wrapped in act/i, /uncontrolled input/i];
const originalError = console.error;
const originalWarn = console.warn;

console.error = (...args: unknown[]) => {
  if (typeof args[0] === 'string' && suppressedWarnings.some((pattern) => pattern.test(args[0]))) {
    return;
  }
  originalError(...args);
};

console.warn = (...args: unknown[]) => {
  if (typeof args[0] === 'string' && suppressedWarnings.some((pattern) => pattern.test(args[0]))) {
    return;
  }
  originalWarn(...args);
};

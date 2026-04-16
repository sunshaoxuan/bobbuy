import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import L10nInput, { type L10nValues } from './L10nInput';

describe('L10nInput', () => {
  it('updates localized value and applies AI suggestion for missing locale', async () => {
    const onChange = vi.fn();
    const requestTranslation = vi.fn().mockResolvedValue('Milk');
    const value: L10nValues = { 'zh-CN': '牛奶' };

    render(
      <L10nInput
        value={value}
        onChange={onChange}
        locales={['zh-CN', 'en-US']}
        requestTranslation={requestTranslation}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'en-US' }));

    await waitFor(() => {
      expect(screen.getByText(/AI 建议：Milk/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /应用建议/i }));

    expect(onChange).toHaveBeenCalledWith({
      'zh-CN': '牛奶',
      'en-US': 'Milk'
    });
  });

  it('edits value in active locale', () => {
    const onChange = vi.fn();

    render(
      <L10nInput
        value={{ 'zh-CN': '原文' }}
        onChange={onChange}
        locales={['zh-CN', 'en-US']}
      />
    );

    const input = screen.getByTestId('l10n-input-field');
    fireEvent.change(input, { target: { value: '新文案' } });

    expect(onChange).toHaveBeenCalledWith({
      'zh-CN': '新文案'
    });
  });
});

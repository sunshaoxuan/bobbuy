import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import L10nInput, { type L10nValues } from './L10nInput';

describe('L10nInput', () => {
  it('requests AI suggestion for missing locale', async () => {
    const requestTranslation = vi.fn().mockImplementation(
      () => new Promise<string>(() => {})
    );
    const value: L10nValues = { 'zh-CN': '牛奶' };

    render(
      <L10nInput
        value={value}
        locales={['zh-CN', 'en-US']}
        requestTranslation={requestTranslation}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: 'en-US' }));

    await waitFor(() => {
      expect(requestTranslation).toHaveBeenCalledWith('牛奶', 'zh-CN', 'en-US');
    });

    expect(screen.getByText(/AI suggestion is generating/i)).toBeInTheDocument();
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

import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { I18nProvider } from '../i18n';
import AiQuickAddModal from './AiQuickAddModal';

const onboardScanMock = vi.fn();

vi.mock('../api', () => ({
  api: {
    onboardScan: (...args: unknown[]) => onboardScanMock(...args),
  },
}));

class MockFileReader {
  result = 'data:image/png;base64,ZmFrZQ==';
  onload: null | ((event: ProgressEvent<FileReader>) => void) = null;

  readAsDataURL() {
    setTimeout(() => {
      this.onload?.(new ProgressEvent('load') as ProgressEvent<FileReader>);
    }, 0);
  }
}

const renderModal = (onSuccess = vi.fn()) =>
  render(
    <I18nProvider>
      <AiQuickAddModal visible onCancel={vi.fn()} onSuccess={onSuccess} />
    </I18nProvider>
  );

describe('AiQuickAddModal', () => {
  beforeEach(() => {
    onboardScanMock.mockResolvedValue({
      name: 'Acme Orange Box 1kg',
      brand: 'Acme',
      price: 10,
      matchScore: 45,
      semanticReasoning: '品名与口味不一致，建议另存为新产品。',
      fieldDiffs: [
        { field: 'flavor', label: '口味', oldValue: 'apple', newValue: 'orange', different: true, identityField: true },
      ],
      verificationTarget: {
        productId: 'prd-1000',
        displayName: 'Acme Apple Box 1kg',
        mediaGallery: [{ url: 'https://example.com/history.jpg', type: 'image' }],
      },
    });
    vi.stubGlobal('FileReader', MockFileReader);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.clearAllMocks();
  });

  it('shows a strong warning and disables overwrite for low-confidence matches', async () => {
    renderModal();
    const input = document.querySelector('.ant-upload-wrapper input[type="file"], input[type="file"]') as HTMLInputElement | null;
    expect(input).not.toBeNull();
    fireEvent.change(input!, { target: { files: [new File(['x'], 'product.png', { type: 'image/png' })] } });

    expect(await screen.findByTestId('ai-low-match-warning', {}, { timeout: 4000 })).toBeInTheDocument();
    expect(document.querySelector('.ant-btn-primary[disabled]')).not.toBeNull();
    expect(screen.getByTestId('attribute-diff-table')).toBeInTheDocument();
  });

  it('allows saving as a new product when overwrite is blocked', async () => {
    const onSuccess = vi.fn();
    renderModal(onSuccess);
    const input = document.querySelector('.ant-upload-wrapper input[type="file"], input[type="file"]') as HTMLInputElement | null;
    expect(input).not.toBeNull();
    fireEvent.change(input!, { target: { files: [new File(['x'], 'product.png', { type: 'image/png' })] } });

    await waitFor(() => expect(screen.getByRole('button', { name: /另存为新产品|Save as New Product/i })).toBeInTheDocument(), {
      timeout: 4000,
    });

    fireEvent.click(screen.getByRole('button', { name: /另存为新产品|Save as New Product/i }));

    expect(onSuccess).toHaveBeenCalledWith(expect.objectContaining({
      existingProductFound: false,
      existingProductId: undefined,
    }));
  });
});

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { I18nProvider } from '../i18n';
import ChatWidget from './ChatWidget';

const apiMocks = vi.hoisted(() => ({
  getTripChat: vi.fn(),
  onboardScan: vi.fn(),
  onboardConfirm: vi.fn(),
  sendChatMessage: vi.fn(),
  patchProduct: vi.fn()
}));

vi.mock('../api', () => ({
  api: {
    getTripChat: apiMocks.getTripChat,
    getOrderChat: vi.fn(),
    getPrivateChat: vi.fn(),
    onboardScan: apiMocks.onboardScan,
    onboardConfirm: apiMocks.onboardConfirm,
    sendChatMessage: apiMocks.sendChatMessage,
    patchProduct: apiMocks.patchProduct
  }
}));

const renderWidget = () =>
  render(
    <I18nProvider>
      <ChatWidget tripId={2000} senderId="PURCHASER" recipientId="DEMO-CUST" />
    </I18nProvider>
  );

describe('ChatWidget', () => {
  let messages: any[] = [];

  beforeEach(() => {
    window.localStorage.setItem('bobbuy_locale', 'en-US');
    messages = [];
    apiMocks.getTripChat.mockReset();
    apiMocks.getTripChat.mockImplementation(() => Promise.resolve([...messages]));
    apiMocks.onboardScan.mockReset();
    apiMocks.onboardConfirm.mockReset();
    apiMocks.sendChatMessage.mockReset();
    apiMocks.patchProduct.mockReset();

    class MockFileReader {
      public result: string | ArrayBuffer | null = 'data:image/png;base64,abc';
      public onload: null | (() => void) = null;
      public onerror: null | (() => void) = null;

      readAsDataURL() {
        this.onload?.();
      }
    }

    vi.stubGlobal('FileReader', MockFileReader);
  });

  afterEach(() => {
    cleanup();
    window.localStorage.clear();
    vi.unstubAllGlobals();
  });

  it('retries failed image scans and completes candidate-confirmation publish flow', async () => {
    apiMocks.onboardScan
      .mockRejectedValueOnce(new Error('scan failed'))
      .mockResolvedValueOnce({
        name: '抹茶礼盒',
        brand: 'BOBBuy',
        description: '',
        price: 88,
        categoryId: 'tea',
        itemNumber: 'SKU-1',
        storageCondition: 'AMBIENT',
        orderMethod: 'DIRECT_BUY',
        mediaGallery: [],
        attributes: {},
        existingProductFound: false,
        existingProductId: null,
        similarProductCandidates: [
          {
            productId: 'prd-existing',
            displayName: '抹茶礼盒大包装',
            itemNumber: 'SKU-998',
            matchReason: 'BRAND_AND_NAME',
            matchSignals: ['BRAND_EXACT', 'ITEM_NUMBER_FRAGMENT'],
            brand: 'BOBBuy',
            categoryId: 'tea',
            matchedFragments: ['bobbuy', '抹茶'],
            aliasSources: ['matcha']
          }
        ],
        visibilityStatus: 'DRAFTER_ONLY',
        detectedPriceTiers: [],
        originalPhotoBase64: 'data:image/png;base64,abc'
      });
    apiMocks.onboardConfirm.mockResolvedValue({
      product: {
        id: 'prd-existing',
        itemNumber: 'SKU-998',
        visibilityStatus: 'DRAFTER_ONLY',
        isTemporary: true,
        mediaGallery: [{ url: 'https://img.example/matcha.png', type: 'IMAGE' }]
      },
      displayName: '抹茶礼盒大包装',
      displayDescription: ''
    });
    apiMocks.sendChatMessage.mockImplementation(async (payload) => {
      messages = [
        ...messages,
        {
          ...payload,
          id: messages.length + 1,
          createdAt: '2026-04-20T22:00:00'
        }
      ];
      return payload;
    });
    apiMocks.patchProduct.mockResolvedValue({
      product: {
        id: 'prd-existing',
        visibilityStatus: 'PUBLIC'
      }
    });

    renderWidget();

    fireEvent.click(screen.getByRole('button', { name: 'Open chat' }));
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [new File(['x'], 'matcha.png', { type: 'image/png' })] } });

    expect(await screen.findByText('Image context is kept locally after a failed scan.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Retry image scan/ }));

    expect(await screen.findByText('Confirm image context')).toBeInTheDocument();
    expect(screen.getByText('Pending confirmation')).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText(/抹茶礼盒大包装/i));
    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(apiMocks.sendChatMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          metadata: expect.objectContaining({
            imageFlowStatus: 'CANDIDATE_SELECTED',
            candidateReasons: ['BRAND_EXACT', 'ITEM_NUMBER_FRAGMENT'],
            attachmentName: 'matcha.png',
            operatorId: 'PURCHASER',
            candidateSummary: expect.objectContaining({
              brand: 'BOBBuy',
              rejectedCount: 0
            }),
            candidateAudit: expect.objectContaining({
              reviewedBy: 'PURCHASER',
              rejectedProductIds: []
            })
          })
        })
      );
    });

    expect(await screen.findByText('Candidate selected')).toBeInTheDocument();
    expect((await screen.findAllByText(/Brand: BOBBuy/i)).length).toBeGreaterThan(0);
    fireEvent.click(screen.getByRole('button', { name: /Publish to Mall/ }));

    await waitFor(() => {
      expect(apiMocks.patchProduct).toHaveBeenCalledWith('prd-existing', { visibilityStatus: 'PUBLIC' });
    });
    expect((await screen.findAllByText('Published to mall')).length).toBeGreaterThan(0);
  });

  it('keeps the last successful snapshot when refresh fails', async () => {
    messages = [
      {
        id: 1,
        senderId: 'PURCHASER',
        recipientId: 'DEMO-CUST',
        content: 'hello',
        type: 'TEXT',
        createdAt: '2026-04-20T22:00:00'
      }
    ];
    renderWidget();
    fireEvent.click(screen.getByRole('button', { name: 'Open chat' }));

    expect(await screen.findByText('hello')).toBeInTheDocument();
    apiMocks.getTripChat.mockRejectedValue(new Error('refresh failed'));
    fireEvent.click(screen.getByRole('button', { name: 'Refresh chat' }));

    expect(await screen.findByText('Refresh failed. Showing the last successful message snapshot.')).toBeInTheDocument();
    expect(screen.getByText('hello')).toBeInTheDocument();
  });
});

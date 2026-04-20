import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { I18nProvider } from '../i18n';
import ChatWidget from './ChatWidget';

const getTripChat = vi.fn();
const onboardScan = vi.fn();
const onboardConfirm = vi.fn();
const sendChatMessage = vi.fn();
const patchProduct = vi.fn();

vi.mock('../api', () => ({
  api: {
    getTripChat,
    getOrderChat: vi.fn(),
    getPrivateChat: vi.fn(),
    onboardScan,
    onboardConfirm,
    sendChatMessage,
    patchProduct
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
    messages = [];
    getTripChat.mockImplementation(() => Promise.resolve([...messages]));
    onboardScan.mockReset();
    onboardConfirm.mockReset();
    sendChatMessage.mockReset();
    patchProduct.mockReset();

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
    vi.unstubAllGlobals();
  });

  it('retries failed image scans and completes candidate-confirmation publish flow', async () => {
    onboardScan
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
            matchSignals: ['BRAND_EXACT', 'ITEM_NUMBER_FRAGMENT']
          }
        ],
        visibilityStatus: 'DRAFTER_ONLY',
        detectedPriceTiers: [],
        originalPhotoBase64: 'data:image/png;base64,abc'
      });
    onboardConfirm.mockResolvedValue({
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
    sendChatMessage.mockImplementation(async (payload) => {
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
    patchProduct.mockResolvedValue({
      product: {
        id: 'prd-existing',
        visibilityStatus: 'PUBLIC'
      }
    });

    renderWidget();

    fireEvent.click(screen.getByRole('button'));
    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(fileInput, { target: { files: [new File(['x'], 'matcha.png', { type: 'image/png' })] } });

    expect(await screen.findByText('Image context is kept locally after a failed scan.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Retry image scan' }));

    expect(await screen.findByText('Confirm image context')).toBeInTheDocument();
    expect(screen.getByText('Pending confirmation')).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText(/抹茶礼盒大包装/i));
    fireEvent.click(screen.getByRole('button', { name: 'OK' }));

    await waitFor(() => {
      expect(sendChatMessage).toHaveBeenCalledWith(
        expect.objectContaining({
          metadata: expect.objectContaining({
            imageFlowStatus: 'CANDIDATE_SELECTED',
            candidateReasons: ['BRAND_EXACT', 'ITEM_NUMBER_FRAGMENT'],
            attachmentName: 'matcha.png'
          })
        })
      );
    });

    expect(await screen.findByText('Candidate selected')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Publish to Mall' }));

    await waitFor(() => {
      expect(patchProduct).toHaveBeenCalledWith('prd-existing', { visibilityStatus: 'PUBLIC' });
    });
    expect(await screen.findByText('Published to mall')).toBeInTheDocument();
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
    getTripChat
      .mockResolvedValueOnce([...messages])
      .mockRejectedValueOnce(new Error('refresh failed'));

    renderWidget();
    fireEvent.click(screen.getByRole('button'));

    expect(await screen.findByText('hello')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '' }));

    expect(await screen.findByText('Refresh failed. Showing the last successful message snapshot.')).toBeInTheDocument();
    expect(screen.getByText('hello')).toBeInTheDocument();
  });
});

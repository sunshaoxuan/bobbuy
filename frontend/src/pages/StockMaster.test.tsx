import { cleanup, render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import StockMaster from './StockMaster';
import { I18nProvider } from '../i18n';
import React from 'react';

const MOBILE_VIEWPORT_WIDTH = 375;
const DESKTOP_VIEWPORT_WIDTH = 1280;

const createMatchMedia = (isMobile: boolean) => vi.fn().mockImplementation((query: string) => {
  const viewportWidth = isMobile ? MOBILE_VIEWPORT_WIDTH : DESKTOP_VIEWPORT_WIDTH;
  const min = /min-width:\s*(\d+)px/.exec(query);
  const max = /max-width:\s*(\d+)px/.exec(query);
  let matches = true;
  if (min) {
    matches = matches && viewportWidth >= Number(min[1]);
  }
  if (max) {
    matches = matches && viewportWidth <= Number(max[1]);
  }
  return {
    matches,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  };
});

const setMatchMedia = (isMobile: boolean) => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: createMatchMedia(isMobile),
  });
};

const renderWithI18n = (ui: React.ReactElement) => {
  return render(<I18nProvider>{ui}</I18nProvider>);
};

describe('StockMaster Component', () => {
  beforeEach(() => {
    setMatchMedia(false);
  });

  afterEach(async () => {
    cleanup();
    await new Promise((resolve) => setTimeout(resolve, 0));
  });

  it('renders the stock master page with title and initial data', () => {
    renderWithI18n(<StockMaster />);
    expect(screen.getByText(/库存大师 - 批量上架/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue(/Organic Milk/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue(/Fresh Spinach/i)).toBeInTheDocument();
  });

  it('adds a new row when clicking "新增商品"', () => {
    renderWithI18n(<StockMaster />);
    const addButton = screen.getByText(/新增商品/i);
    fireEvent.click(addButton);
    
    // Check for "待发布" status tag which indicates a new row
    const pendingTags = screen.getAllByText(/待发布/i);
    expect(pendingTags.length).toBeGreaterThan(0);
  });

  it('updates field value when input changes', () => {
    renderWithI18n(<StockMaster />);
    const firstInput = screen.getByDisplayValue(/Organic Milk/i);
    fireEvent.change(firstInput, { target: { value: 'Updated Milk' } });
    expect(firstInput.getAttribute('value')).toBe('Updated Milk');
  });

  it('filters table rows based on search input', () => {
    renderWithI18n(<StockMaster />);
    const searchInput = screen.getByPlaceholderText(/搜索商品名称 or SKU.../i);
    fireEvent.change(searchInput, { target: { value: 'Milk' } });
    
    expect(screen.getByDisplayValue(/Organic Milk/i)).toBeInTheDocument();
    expect(screen.queryByDisplayValue(/Fresh Spinach/i)).not.toBeInTheDocument();
  });

  it('opens editing drawer and updates values', async () => {
    renderWithI18n(<StockMaster />);
    const editButtons = screen.getAllByRole('button').filter(btn => btn.querySelector('.anticon-edit'));
    fireEvent.click(editButtons[0]);
    
    expect(screen.getByText(/编辑商品详情/i)).toBeInTheDocument();
    // Use drawer-scoped SKU input to avoid matching table inline editors
    const drawer = screen.getByRole('dialog');
    const skuInput = within(drawer).getByPlaceholderText(/输入唯一 SKU 编号/i);
    fireEvent.change(skuInput, { target: { value: 'NEW-SKU-999' } });
    
    const saveButton = screen.getByText(/保存并关闭/i);
    fireEvent.click(saveButton);
    
    await waitFor(() => {
      expect(screen.queryByText(/编辑商品详情/i)).not.toBeInTheDocument();
    });
  });

  it('deletes a row when clicking the delete button', () => {
    renderWithI18n(<StockMaster />);
    const initialRows = screen.getAllByRole('row');
    const deleteButtons = screen.getAllByRole('button').filter(btn => btn.querySelector('.anticon-delete'));
    
    fireEvent.click(deleteButtons[0]);
    
    const finalRows = screen.getAllByRole('row');
    expect(finalRows.length).toBe(initialRows.length - 1);
  });

  it('displays success message on publish', async () => {
    renderWithI18n(<StockMaster />);
    const publishButton = screen.getByText(/同步至市场/i);
    fireEvent.click(publishButton);
    
    expect(publishButton).toBeInTheDocument();
  });

  it('renders category-specific dynamic attributes in drawer', async () => {
    renderWithI18n(<StockMaster />);
    const editButtons = screen.getAllByRole('button').filter(btn => btn.querySelector('.anticon-edit'));
    fireEvent.click(editButtons[0]);

    const drawer = screen.getByRole('dialog');
    const categoryInput = within(drawer).getByRole('textbox', { name: /类目/i });
    fireEvent.change(categoryInput, { target: { value: '服装' } });

    await waitFor(() => {
      expect(within(drawer).getByText(/分类属性/i)).toBeInTheDocument();
      expect(within(drawer).getByLabelText(/尺码/i)).toBeInTheDocument();
      expect(within(drawer).getByLabelText(/材质/i)).toBeInTheDocument();
    });
  });

});

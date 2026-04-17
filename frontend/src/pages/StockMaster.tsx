import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Table,
  Input,
  InputNumber,
  Button,
  Space,
  Typography,
  Tag,
  message,
  Breadcrumb,
  Card,
  Drawer,
  Form,
  Grid,
  Select,
  Tabs,
  Row,
  Col
} from 'antd';
import {
  PlusOutlined,
  SaveOutlined,
  DeleteOutlined,
  InboxOutlined,
  EditOutlined,
  SearchOutlined,
  DollarOutlined,
  DatabaseOutlined
} from '@ant-design/icons';
import { useI18n } from '../i18n';
import L10nInput, { type L10nValues } from '../components/L10nInput';
import MediaGallery, { type MediaItem } from '../components/MediaGallery';
import { api, type CategoryAttributeTemplateField, type MobileCategory } from '../api';

const { Title, Text } = Typography;

interface StockItem {
  key: string;
  name: string;
  nameL10n?: L10nValues;
  category: string;
  price: number;
  stock: number;
  unit: string;
  description?: string;
  descriptionL10n?: L10nValues;
  brand?: string;
  sku?: string;
  mediaGallery?: MediaItem[];
  dynamicAttributes?: Record<string, string | number>;
  merchantSkus?: Record<string, string>;
  storageCondition?: 'AMBIENT' | 'CHILLED';
  orderMethod?: 'PRE_ORDER' | 'DIRECT_BUY';
  isNew?: boolean;
}

type MerchantSkuEntry = {
  merchant?: string;
  sku?: string;
};

const AUTOSAVE_DELAY_MS = 500;
const MOBILE_DRAWER_HEIGHT = '82vh';
const MOBILE_TOOLBAR_COMPACT_SCROLL_THRESHOLD = 24;
const MOBILE_DYNAMIC_GUTTER = 16;
const DESKTOP_DYNAMIC_GUTTER = 8;
const DEFAULT_STOCK_THUMBNAIL = '/assets/products/milk.png';
const CURRENCY_BY_LOCALE: Record<string, string> = {
  'zh-CN': 'CNY',
  'en-US': 'USD'
};
const PRICE_FRACTION_DIGITS = 2;
const MOBILE_BOTTOM_PADDING = '7rem';
const DESKTOP_BOTTOM_PADDING = '5rem';
const normalizeTokens = (value: string) => value.toLowerCase().split(/[^a-z0-9\u4e00-\u9fa5]+/i).filter(Boolean);

const matchesByName = (input: string, candidate: string) => {
  const normalizedInput = input.trim().toLowerCase();
  const normalizedCandidate = candidate.trim().toLowerCase();
  if (!normalizedInput || !normalizedCandidate) {
    return false;
  }
  if (normalizedInput === normalizedCandidate || normalizedInput.includes(normalizedCandidate)) {
    return true;
  }
  const inputTokens = normalizeTokens(normalizedInput);
  const candidateTokens = normalizeTokens(normalizedCandidate);
  return candidateTokens.every((token) => inputTokens.includes(token));
};

const resolveCategoryTemplate = (categoryInput: string | undefined, categories: MobileCategory[]): CategoryAttributeTemplateField[] => {
  if (!categoryInput) {
    return [];
  }
  const normalized = categoryInput.trim().toLowerCase();
  const matchedCategory = categories.find((category) => {
    if (category.id.toLowerCase() === normalized) {
      return true;
    }
    return Object.values(category.name ?? {}).some((name) => matchesByName(normalized, name));
  });
  return matchedCategory?.attributeTemplate ?? [];
};

const mapMerchantSkusToEntries = (merchantSkus?: Record<string, string>): MerchantSkuEntry[] => {
  if (!merchantSkus) {
    return [];
  }
  return Object.entries(merchantSkus).map(([merchant, sku]) => ({ merchant, sku }));
};

const mapEntriesToMerchantSkus = (entries?: MerchantSkuEntry[]): Record<string, string> => {
  if (!entries) {
    return {};
  }
  return entries.reduce<Record<string, string>>((acc, entry) => {
    const merchant = entry.merchant?.trim();
    const sku = entry.sku?.trim();
    if (merchant && sku) {
      acc[merchant] = sku;
    }
    return acc;
  }, {});
};

const getFieldLabel = (field: CategoryAttributeTemplateField, translate: (key: string) => string) => {
  if (field.labelKey) {
    return translate(field.labelKey);
  }
  return field.label ?? field.key;
};

const CATEGORY_FIELD_TYPE_OPTIONS = ['text', 'number', 'select'];

const isValidCategoryFieldType = (value: string): value is CategoryAttributeTemplateField['type'] =>
  CATEGORY_FIELD_TYPE_OPTIONS.includes(value);

const normalizeCategoryTemplate = (template: CategoryAttributeTemplateField[] | undefined): CategoryAttributeTemplateField[] => {
  if (!template) {
    return [];
  }
  return template
    .filter((field): field is CategoryAttributeTemplateField => Boolean(field?.key && field?.type && isValidCategoryFieldType(field.type)))
    .map((field) => ({
      key: field.key,
      type: field.type,
      labelKey: field.labelKey,
      label: field.label,
      options: field.options
    }));
};

const normalizeCategories = (categories: MobileCategory[]): MobileCategory[] =>
  categories.map((category) => ({
    ...category,
    attributeTemplate: normalizeCategoryTemplate(category.attributeTemplate)
  }));

export default function StockMaster() {
  const { t, locale } = useI18n();
  const [categories, setCategories] = useState<MobileCategory[]>([]);
  const [searchText, setSearchText] = useState('');
  const [toolbarCompact, setToolbarCompact] = useState(false);
  const [isDrawerVisible, setIsDrawerVisible] = useState(false);
  const [editingItem, setEditingItem] = useState<StockItem | null>(null);
  const [syncStatus, setSyncStatus] = useState<'idle' | 'saving' | 'saved'>('idle');
  const [form] = Form.useForm();
  const screens = Grid.useBreakpoint();
  const isMobile = screens.md === false;
  const autosaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isDrawerOpenRef = useRef(false);
  const watchedCategory = Form.useWatch('category', form);
  const activeCategoryAttributes = useMemo(
    () => resolveCategoryTemplate(watchedCategory, categories),
    [watchedCategory, categories]
  );

  useEffect(() => {
    let cancelled = false;
    api.stockCategories().then((result) => {
      if (cancelled) {
        return;
      }
      setCategories(normalizeCategories(result));
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const clearAutosaveTimer = () => {
    if (autosaveTimerRef.current) {
      clearTimeout(autosaveTimerRef.current);
      autosaveTimerRef.current = null;
    }
  };

  useEffect(() => {
    isDrawerOpenRef.current = isDrawerVisible;
  }, [isDrawerVisible]);

  useEffect(() => {
    if (!isMobile) {
      setToolbarCompact(false);
      return;
    }
    const onScroll = () => setToolbarCompact(window.scrollY > MOBILE_TOOLBAR_COMPACT_SCROLL_THRESHOLD);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, [isMobile]);

  const [dataSource, setDataSource] = useState<StockItem[]>([
    {
      key: '1',
      name: 'Organic Milk',
      nameL10n: { 'zh-CN': '有机牛奶', 'en-US': 'Organic Milk' },
      category: 'Dairy',
      price: 12.99,
      stock: 50,
      unit: '3pk',
      brand: 'Organic Valley',
      sku: 'OM-001',
      description: 'Fresh organic milk from local farms.',
      descriptionL10n: { 'zh-CN': '来自本地农场的新鲜有机牛奶。', 'en-US': 'Fresh organic milk from local farms.' },
      mediaGallery: [
        {
          id: 'milk-image-1',
          url: 'https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=800&q=80',
          type: 'image',
          title: { 'zh-CN': '牛奶正面图', 'en-US': 'Milk front view' }
        }
      ]
    },
    {
      key: '2',
      name: 'Fresh Spinach',
      nameL10n: { 'zh-CN': '新鲜菠菜', 'en-US': 'Fresh Spinach' },
      category: 'Produce',
      price: 4.5,
      stock: 100,
      unit: 'bag',
      brand: 'Green Garden',
      sku: 'FS-002',
      description: 'Pre-washed baby spinach leaves.',
      descriptionL10n: { 'zh-CN': '免洗嫩菠菜叶。', 'en-US': 'Pre-washed baby spinach leaves.' },
      mediaGallery: [
        {
          id: 'spinach-video-1',
          url: 'https://www.w3schools.com/html/mov_bbb.mp4',
          type: 'video',
          title: { 'zh-CN': '菠菜展示视频', 'en-US': 'Spinach showcase video' }
        }
      ]
    }
  ]);

  const getLocalizedFallback = (values?: L10nValues, fallback?: string) => {
    if (!values) {
      return fallback ?? '';
    }
    return values[locale] || values['zh-CN'] || values['en-US'] || Object.values(values).find(Boolean) || fallback || '';
  };

  const formatPrice = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: CURRENCY_BY_LOCALE[locale] ?? 'USD',
        minimumFractionDigits: PRICE_FRACTION_DIGITS,
        maximumFractionDigits: PRICE_FRACTION_DIGITS
      }),
    [locale]
  );

  const getStockThumbnail = (item: StockItem): string => item.mediaGallery?.find((media) => media.type === 'image')?.url ?? DEFAULT_STOCK_THUMBNAIL;

  const requestTranslationSuggestion = async (sourceText: string, _sourceLocale: string, targetLocale: string) => {
    if (!sourceText.trim()) {
      return '';
    }
    return Promise.resolve(`[AI:${targetLocale}] ${sourceText}`);
  };

  const handleAddRow = () => {
    const newData: StockItem = {
      key: Date.now().toString(),
      name: '',
      category: '',
      price: 0,
      stock: 0,
      unit: 'pc',
      isNew: true
    };
    setDataSource([...dataSource, newData]);
  };

  const handleDelete = (key: string) => {
    setDataSource(dataSource.filter((item) => item.key !== key));
  };

  const handleFieldChange = (key: string, field: keyof StockItem, value: any) => {
    const newData = [...dataSource];
    const index = newData.findIndex((item) => key === item.key);
    if (index > -1) {
      newData[index] = { ...newData[index], [field]: value };
      setDataSource(newData);
    }
  };

  const handlePublish = () => {
    message.success(t('stock.msg.published'));
    setDataSource(dataSource.map((item) => ({ ...item, isNew: false })));
  };

  const openDrawer = (record: StockItem) => {
    setEditingItem(record);
    form.setFieldsValue({
      ...record,
      nameL10n: record.nameL10n ?? { 'zh-CN': record.name, 'en-US': record.name },
      descriptionL10n: record.descriptionL10n ?? { 'zh-CN': record.description ?? '', 'en-US': record.description ?? '' },
      mediaGallery: record.mediaGallery ?? [],
      dynamicAttributes: record.dynamicAttributes ?? {},
      merchantSkuEntries: mapMerchantSkusToEntries(record.merchantSkus)
    });
    setIsDrawerVisible(true);
    setSyncStatus('idle');
  };

  const closeDrawer = () => {
    clearAutosaveTimer();
    setIsDrawerVisible(false);
    setEditingItem(null);
    setSyncStatus('idle');
    form.resetFields();
  };

  const saveEditingItem = (values: Partial<StockItem>, options: { closeAfterSave: boolean; showMessage: boolean }) => {
    if (!editingItem) {
      return;
    }
    setDataSource((prevData) => {
      const index = prevData.findIndex((item) => editingItem.key === item.key);
      if (index < 0) {
        return prevData;
      }
      const nextData = [...prevData];
      const currentItem = nextData[index];
      const rawValues = values as Partial<StockItem> & { merchantSkuEntries?: MerchantSkuEntry[] };
      const { merchantSkuEntries, ...safeValues } = rawValues;
      const merchantSkus = mapEntriesToMerchantSkus(merchantSkuEntries);
      const nextNameL10n: L10nValues = values.nameL10n ?? currentItem.nameL10n ?? {};
      const nextDescriptionL10n: L10nValues = values.descriptionL10n ?? currentItem.descriptionL10n ?? {};
      nextData[index] = {
        ...currentItem,
        ...safeValues,
        nameL10n: nextNameL10n,
        descriptionL10n: nextDescriptionL10n,
        name: getLocalizedFallback(nextNameL10n, currentItem.name),
        description: getLocalizedFallback(nextDescriptionL10n, currentItem.description),
        mediaGallery: values.mediaGallery ?? currentItem.mediaGallery ?? [],
        dynamicAttributes: values.dynamicAttributes ?? currentItem.dynamicAttributes ?? {},
        merchantSkus
      };
      return nextData;
    });
    if (options.showMessage) {
      message.success(t('stock.drawer.update_success'));
    }
    if (options.closeAfterSave) {
      closeDrawer();
    }
  };

  const handleDrawerSave = () => {
    form.validateFields().then((values) => {
      saveEditingItem(values, { closeAfterSave: true, showMessage: true });
    });
  };

  const handleDrawerValuesChange = (_changedValues: Record<string, unknown>, _allValues: Record<string, unknown>) => {
    if (!isMobile || !editingItem) {
      return;
    }
    setSyncStatus('saving');
    clearAutosaveTimer();
    autosaveTimerRef.current = setTimeout(() => {
      if (!isDrawerOpenRef.current) {
        clearAutosaveTimer();
        return;
      }
      saveEditingItem(form.getFieldsValue(true), { closeAfterSave: false, showMessage: false });
      setSyncStatus('saved');
      clearAutosaveTimer();
    }, AUTOSAVE_DELAY_MS);
  };

  useEffect(() => {
    return () => {
      clearAutosaveTimer();
    };
  }, []);

  const filteredData = dataSource.filter(
    (item) => item.name.toLowerCase().includes(searchText.toLowerCase()) || (item.sku && item.sku.toLowerCase().includes(searchText.toLowerCase()))
  );

  const columns = [
    {
      title: t('stock.item.name'),
      dataIndex: 'name',
      render: (text: string, record: StockItem) => (
        <Input value={text} placeholder="e.g. Fuji Apple" onChange={(e) => handleFieldChange(record.key, 'name', e.target.value)} />
      )
    },
    {
      title: t('stock.item.category'),
      dataIndex: 'category',
      render: (text: string, record: StockItem) => (
        <Input value={text} placeholder="e.g. Fruits" onChange={(e) => handleFieldChange(record.key, 'category', e.target.value)} />
      )
    },
    {
      title: t('stock.item.price'),
      dataIndex: 'price',
      width: '8rem',
      render: (val: number, record: StockItem) => (
        <InputNumber value={val} prefix="$" style={{ width: '100%' }} onChange={(v) => handleFieldChange(record.key, 'price', v)} />
      )
    },
    {
      title: t('stock.item.quantity'),
      dataIndex: 'stock',
      width: '7rem',
      render: (val: number, record: StockItem) => (
        <InputNumber value={val} style={{ width: '100%' }} onChange={(v) => handleFieldChange(record.key, 'stock', v)} />
      )
    },
    {
      title: 'Status',
      key: 'status',
      width: '7rem',
      render: (_: any, record: StockItem) =>
        record.isNew ? <Tag color="orange">{t('stock.status.new')}</Tag> : <Tag color="blue">{t('stock.status.modified')}</Tag>
    },
    {
      title: 'Action',
      key: 'action',
      width: '8rem',
      render: (_: any, record: StockItem) => (
        <Space>
          <Button type="text" icon={<EditOutlined />} onClick={() => openDrawer(record)} />
          <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDelete(record.key)} />
        </Space>
      )
    }
  ];

  const renderMobileCards = () => (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      {filteredData.map((item) => {
        const image = getStockThumbnail(item);
        const localizedTitle = getLocalizedFallback(item.nameL10n, item.name);
        return (
          <Card key={item.key} className="stock-mobile-card app-shadow-low">
            <div className="stock-mobile-card-body">
              <img src={image} alt={localizedTitle} className="stock-mobile-thumb" />
              <div className="stock-mobile-main">
                <Text strong className="stock-mobile-title">
                  {localizedTitle}
                </Text>
                <Space size={8} wrap>
                  <Tag color={item.isNew ? 'orange' : 'blue'}>{item.isNew ? t('stock.status.new') : t('stock.status.modified')}</Tag>
                  {item.sku ? <Text type="secondary">SKU: {item.sku}</Text> : null}
                </Space>
              </div>
              <div className="stock-mobile-side">
                <Text className="stock-mobile-metric">
                  <DollarOutlined /> {formatPrice.format(item.price)}
                </Text>
                <Text className="stock-mobile-metric">
                  <DatabaseOutlined /> {item.stock}
                </Text>
                <Space size={4}>
                  <Button type="text" icon={<EditOutlined />} onClick={() => openDrawer(item)} aria-label={t('stock.master.edit_detail')} />
                  <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDelete(item.key)} aria-label={t('stock.master.discard')} />
                </Space>
              </div>
            </div>
          </Card>
        );
      })}
      {filteredData.length === 0 ? (
        <Card className="stock-mobile-card app-shadow-low">
          <Space direction="vertical" align="center" style={{ width: '100%', padding: '1.5rem 0' }}>
            <InboxOutlined style={{ fontSize: '1.75rem', color: '#d9d9d9' }} />
            <Text type="secondary">{t('stock.empty')}</Text>
          </Space>
        </Card>
      ) : null}
    </Space>
  );

  return (
    <div style={{ padding: isMobile ? `0 0 ${MOBILE_BOTTOM_PADDING} 0` : `0 0 ${DESKTOP_BOTTOM_PADDING} 0` }}>
      <Breadcrumb style={{ marginBottom: '1rem' }}>
        <Breadcrumb.Item>{t('nav.dashboard')}</Breadcrumb.Item>
        <Breadcrumb.Item>{t('nav.stock_master')}</Breadcrumb.Item>
      </Breadcrumb>

      <div className={`stock-toolbar ${isMobile ? 'stock-toolbar-mobile' : ''} ${toolbarCompact ? 'stock-toolbar-compact' : ''}`}>
        <div style={{ minWidth: 0 }}>
          <Title level={3} style={{ margin: 0 }} className="stock-page-title">
            {t('stock.master.title')}
          </Title>
          <Text type="secondary">{t('stock.master.subtitle')}</Text>
        </div>

        {isMobile ? (
          <Space direction="vertical" style={{ width: '100%' }} size={8}>
            <Input
              placeholder={t('stock.master.search_placeholder')}
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
            <Button type="default" icon={<PlusOutlined />} onClick={handleAddRow} block>
              {t('stock.master.add_row')}
            </Button>
          </Space>
        ) : (
          <Space direction="vertical" align="end">
            <Input
              placeholder={t('stock.master.search_placeholder')}
              prefix={<SearchOutlined />}
              style={{ width: '20rem' }}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAddRow}>
              {t('stock.master.add_row')}
            </Button>
          </Space>
        )}
      </div>

      {isMobile ? (
        renderMobileCards()
      ) : (
        <Card bodyStyle={{ padding: 0 }} style={{ overflow: 'hidden', borderRadius: '0.75rem' }} className="app-shadow-medium">
          <Table
            dataSource={filteredData}
            columns={columns}
            pagination={false}
            locale={{
              emptyText: (
                <div style={{ padding: '2.5rem' }}>
                  <InboxOutlined style={{ fontSize: '2rem', color: '#d9d9d9' }} />
                  <p>{t('stock.empty')}</p>
                </div>
              )
            }}
          />
        </Card>
      )}

      <Drawer
        rootClassName="stock-drawer"
        title={
          <Space direction="vertical" size={0}>
            <span>{t('stock.drawer.title')}</span>
            {isMobile && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {syncStatus === 'saving'
                  ? t('stock.drawer.sync.saving')
                  : syncStatus === 'saved'
                  ? t('stock.drawer.sync.saved')
                  : t('stock.drawer.sync.ready')}
              </Text>
            )}
          </Space>
        }
        width={isMobile ? undefined : 400}
        height={isMobile ? MOBILE_DRAWER_HEIGHT : undefined}
        placement={isMobile ? 'bottom' : 'right'}
        onClose={closeDrawer}
        open={isDrawerVisible}
        destroyOnClose={true}
        extra={
          isMobile ? null : (
            <Space>
              <Button onClick={closeDrawer}>Cancel</Button>
              <Button onClick={handleDrawerSave} type="primary">
                {t('stock.drawer.save')}
              </Button>
            </Space>
          )
        }
      >
        <Form form={form} layout="vertical" onValuesChange={handleDrawerValuesChange}>
          {isMobile && (
            <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
              {t('stock.drawer.autosave_hint')}
            </Text>
          )}
          <Tabs
            items={[
              {
                key: 'basic',
                label: t('stock.tabs.basic'),
                children: (
                  <>
                    <Form.Item name="nameL10n" label={`${t('stock.item.name')} (L10n)`} rules={[{ required: true }]}>
                      <L10nInput
                        locales={['zh-CN', 'en-US']}
                        requestTranslation={requestTranslationSuggestion}
                        placeholder={t('stock.l10n.name_placeholder')}
                        loadingSuggestionText={t('stock.l10n.ai_loading')}
                        suggestionPrefixText={t('stock.l10n.ai_prefix')}
                        applySuggestionText={t('stock.l10n.ai_apply')}
                      />
                    </Form.Item>
                    <Form.Item name="sku" label={t('stock.item.sku')}>
                      <Input placeholder="Unique SKU ID" />
                    </Form.Item>
                    <Form.Item name="brand" label={t('stock.item.brand')}>
                      <Input placeholder="Brand Name" />
                    </Form.Item>
                    <Form.Item name="category" label={t('stock.item.category')}>
                      <Input />
                    </Form.Item>
                    {activeCategoryAttributes.length > 0 && (
                      <Text strong style={{ display: 'block', marginBottom: 8 }}>
                        {t('stock.dynamic.section')}
                      </Text>
                    )}
                    {activeCategoryAttributes.length > 0 ? (
                      <Row gutter={[MOBILE_DYNAMIC_GUTTER, isMobile ? MOBILE_DYNAMIC_GUTTER : DESKTOP_DYNAMIC_GUTTER]}>
                        {activeCategoryAttributes.map((field) => (
                          <Col key={field.key} xs={24} sm={24} md={12} lg={12}>
                            <Form.Item name={['dynamicAttributes', field.key]} label={getFieldLabel(field, t)}>
                              {field.type === 'number' ? (
                                <InputNumber style={{ width: '100%' }} />
                              ) : field.type === 'select' ? (
                                <Select options={field.options?.map((option) => ({ value: option, label: option }))} />
                              ) : (
                                <Input />
                              )}
                            </Form.Item>
                          </Col>
                        ))}
                      </Row>
                    ) : null}
                    <Form.Item name="storageCondition" label={t('stock.item.storage_condition')}>
                      <Select
                        allowClear
                        options={[
                          { value: 'AMBIENT', label: t('stock.storage.ambient') },
                          { value: 'CHILLED', label: t('stock.storage.chilled') }
                        ]}
                      />
                    </Form.Item>
                    <Form.Item name="orderMethod" label={t('stock.item.order_method')}>
                      <Select
                        allowClear
                        options={[
                          { value: 'PRE_ORDER', label: t('stock.order.pre_order') },
                          { value: 'DIRECT_BUY', label: t('stock.order.direct_buy') }
                        ]}
                      />
                    </Form.Item>
                    <Form.Item name="price" label={t('stock.item.price')}>
                      <InputNumber prefix="$" style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="stock" label={t('stock.item.quantity')}>
                      <InputNumber style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="descriptionL10n" label={`${t('stock.item.description')} (L10n)`}>
                      <L10nInput
                        locales={['zh-CN', 'en-US']}
                        requestTranslation={requestTranslationSuggestion}
                        placeholder={t('stock.l10n.description_placeholder')}
                        loadingSuggestionText={t('stock.l10n.ai_loading')}
                        suggestionPrefixText={t('stock.l10n.ai_prefix')}
                        applySuggestionText={t('stock.l10n.ai_apply')}
                      />
                    </Form.Item>
                    <Form.Item name="mediaGallery" label={t('stock.media.label')}>
                      <MediaGallery
                        locales={['zh-CN', 'en-US']}
                        requestTranslation={requestTranslationSuggestion}
                        emptyDescriptionText={t('stock.media.empty')}
                        mediaTitlePrefixText={t('stock.media.item_prefix')}
                        addMediaText={t('stock.media.add')}
                        urlPlaceholderText={t('stock.media.url_placeholder')}
                        imageLabelText={t('stock.media.image')}
                        videoLabelText={t('stock.media.video')}
                        titlePlaceholderText={t('stock.media.title_placeholder')}
                      />
                    </Form.Item>
                  </>
                )
              },
              {
                key: 'merchantCodes',
                label: t('stock.tabs.merchant_codes'),
                children: (
                  <Form.List name="merchantSkuEntries">
                    {(fields, { add, remove }) => (
                      <Space direction="vertical" style={{ width: '100%' }}>
                        {fields.map((field) => (
                          <Row key={field.key} gutter={8} align="middle">
                            <Col span={10}>
                              <Form.Item
                                name={[field.name, 'merchant']}
                                label={t('stock.merchant_codes.merchant')}
                                style={{ marginBottom: 8 }}
                              >
                                <Input placeholder={t('stock.merchant_codes.merchant_placeholder')} />
                              </Form.Item>
                            </Col>
                            <Col span={10}>
                              <Form.Item
                                name={[field.name, 'sku']}
                                label={t('stock.merchant_codes.sku')}
                                style={{ marginBottom: 8 }}
                              >
                                <Input placeholder={t('stock.merchant_codes.sku_placeholder')} />
                              </Form.Item>
                            </Col>
                            <Col span={4}>
                              <Button onClick={() => remove(field.name)} danger style={{ marginTop: 30 }}>
                                {t('stock.merchant_codes.remove')}
                              </Button>
                            </Col>
                          </Row>
                        ))}
                        <Button onClick={() => add()} type="dashed" block>
                          {t('stock.merchant_codes.add')}
                        </Button>
                      </Space>
                    )}
                  </Form.List>
                )
              }
            ]}
          />
        </Form>
      </Drawer>

      {isMobile ? (
        <div className="stock-mobile-fab-group">
          <Button
            shape="circle"
            icon={<PlusOutlined />}
            aria-label={t('stock.master.add_row')}
            size="large"
            className="app-shadow-medium"
            onClick={handleAddRow}
          />
          <Button
            type="primary"
            shape="circle"
            icon={<SaveOutlined />}
            aria-label={t('stock.master.publish')}
            size="large"
            className="app-shadow-high"
            onClick={handlePublish}
            id="publish-btn"
          />
        </div>
      ) : (
        <div className="stock-desktop-actionbar app-shadow-low">
          <Button size="large">{t('stock.master.discard')}</Button>
          <Button type="primary" size="large" icon={<SaveOutlined />} onClick={handlePublish} id="publish-btn">
            {t('stock.master.publish')}
          </Button>
        </div>
      )}
    </div>
  );
}

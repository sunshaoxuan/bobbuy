import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Table, Input, InputNumber, Button, Space, Typography, Tag, message, Breadcrumb, Card, Drawer, Form, Grid, Select } from 'antd';
import { PlusOutlined, SaveOutlined, DeleteOutlined, InboxOutlined, EditOutlined, SearchOutlined } from '@ant-design/icons';
import { useI18n } from '../i18n';
import L10nInput, { type L10nValues } from '../components/L10nInput';
import MediaGallery, { type MediaItem } from '../components/MediaGallery';

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
  isNew?: boolean;
}

interface CategoryAttributeDefinition {
  key: string;
  labelKey: string;
  type: 'text' | 'number' | 'select';
  options?: string[];
}

const CATEGORY_ATTRIBUTE_TEMPLATES: Record<'clothing' | 'food', CategoryAttributeDefinition[]> = {
  clothing: [
    { key: 'size', labelKey: 'stock.dynamic.size', type: 'select', options: ['XS', 'S', 'M', 'L', 'XL'] },
    { key: 'material', labelKey: 'stock.dynamic.material', type: 'text' },
    { key: 'color', labelKey: 'stock.dynamic.color', type: 'text' }
  ],
  food: [
    { key: 'shelfLifeDays', labelKey: 'stock.dynamic.shelf_life_days', type: 'number' },
    { key: 'storageTemp', labelKey: 'stock.dynamic.storage_temp', type: 'text' },
    { key: 'flavor', labelKey: 'stock.dynamic.flavor', type: 'text' }
  ]
};

const resolveCategoryTemplate = (category?: string): CategoryAttributeDefinition[] => {
  if (!category) {
    return [];
  }
  const normalized = category.trim().toLowerCase();
  const clothingAliases = ['clothing', 'apparel', 'fashion', '服装', '时尚', '鞋包'];
  const foodAliases = ['food', 'grocery', 'snack', '食品', '零食', '生鲜'];
  if (clothingAliases.some((alias) => normalized.includes(alias))) {
    return CATEGORY_ATTRIBUTE_TEMPLATES.clothing;
  }
  if (foodAliases.some((alias) => normalized.includes(alias))) {
    return CATEGORY_ATTRIBUTE_TEMPLATES.food;
  }
  return [];
};

export default function StockMaster() {
  const { t, locale } = useI18n();
  const [searchText, setSearchText] = useState('');
  const [isDrawerVisible, setIsDrawerVisible] = useState(false);
  const [editingItem, setEditingItem] = useState<StockItem | null>(null);
  const [syncStatus, setSyncStatus] = useState<'idle' | 'saving' | 'saved'>('idle');
  const [form] = Form.useForm();
  const screens = Grid.useBreakpoint();
  const isMobile = screens.md === false;
  const autosaveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const watchedCategory = Form.useWatch('category', form);
  const activeCategoryAttributes = useMemo(() => resolveCategoryTemplate(watchedCategory), [watchedCategory]);

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
      price: 4.50,
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
      isNew: true,
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
    setDataSource(dataSource.map(item => ({ ...item, isNew: false })));
  };

  const openDrawer = (record: StockItem) => {
    setEditingItem(record);
    form.setFieldsValue({
      ...record,
      nameL10n: record.nameL10n ?? { 'zh-CN': record.name, 'en-US': record.name },
      descriptionL10n: record.descriptionL10n ?? { 'zh-CN': record.description ?? '', 'en-US': record.description ?? '' },
      mediaGallery: record.mediaGallery ?? [],
      dynamicAttributes: record.dynamicAttributes ?? {}
    });
    setIsDrawerVisible(true);
    setSyncStatus('idle');
  };

  const closeDrawer = () => {
    if (autosaveTimerRef.current) {
      clearTimeout(autosaveTimerRef.current);
      autosaveTimerRef.current = null;
    }
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
      const nextNameL10n: L10nValues = values.nameL10n ?? currentItem.nameL10n ?? {};
      const nextDescriptionL10n: L10nValues = values.descriptionL10n ?? currentItem.descriptionL10n ?? {};
      nextData[index] = {
        ...currentItem,
        ...values,
        nameL10n: nextNameL10n,
        descriptionL10n: nextDescriptionL10n,
        name: getLocalizedFallback(nextNameL10n, currentItem.name),
        description: getLocalizedFallback(nextDescriptionL10n, currentItem.description),
        mediaGallery: values.mediaGallery ?? currentItem.mediaGallery ?? [],
        dynamicAttributes: values.dynamicAttributes ?? currentItem.dynamicAttributes ?? {}
      };
      return nextData;
    });
    if (options.showMessage) {
      message.success('Item updated');
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

  const handleDrawerValuesChange = (_changedValues: unknown, allValues: StockItem) => {
    if (!isMobile || !editingItem) {
      return;
    }
    setSyncStatus('saving');
    if (autosaveTimerRef.current) {
      clearTimeout(autosaveTimerRef.current);
    }
    autosaveTimerRef.current = setTimeout(() => {
      saveEditingItem(allValues, { closeAfterSave: false, showMessage: false });
      setSyncStatus('saved');
      autosaveTimerRef.current = null;
    }, 500);
  };

  useEffect(() => {
    return () => {
      if (autosaveTimerRef.current) {
        clearTimeout(autosaveTimerRef.current);
      }
    };
  }, []);

  const filteredData = dataSource.filter(item => 
    item.name.toLowerCase().includes(searchText.toLowerCase()) || 
    (item.sku && item.sku.toLowerCase().includes(searchText.toLowerCase()))
  );

  const columns = [
    {
      title: t('stock.item.name'),
      dataIndex: 'name',
      render: (text: string, record: StockItem) => (
        <Input 
          value={text} 
          placeholder="e.g. Fuji Apple"
          onChange={(e) => handleFieldChange(record.key, 'name', e.target.value)} 
        />
      ),
    },
    {
      title: t('stock.item.category'),
      dataIndex: 'category',
      render: (text: string, record: StockItem) => (
        <Input 
          value={text} 
          placeholder="e.g. Fruits"
          onChange={(e) => handleFieldChange(record.key, 'category', e.target.value)} 
        />
      ),
    },
    {
      title: t('stock.item.price'),
      dataIndex: 'price',
      width: 120,
      render: (val: number, record: StockItem) => (
        <InputNumber
          value={val}
          prefix="$"
          style={{ width: '100%' }}
          onChange={(v) => handleFieldChange(record.key, 'price', v)}
        />
      ),
    },
    {
      title: t('stock.item.quantity'),
      dataIndex: 'stock',
      width: 100,
      render: (val: number, record: StockItem) => (
        <InputNumber
          value={val}
          style={{ width: '100%' }}
          onChange={(v) => handleFieldChange(record.key, 'stock', v)}
        />
      ),
    },
    {
      title: 'Status',
      key: 'status',
      width: 100,
      render: (_: any, record: StockItem) => (
        record.isNew ? <Tag color="orange">{t('stock.status.new')}</Tag> : <Tag color="blue">{t('stock.status.modified')}</Tag>
      ),
    },
    {
      title: 'Action',
      key: 'action',
      width: 120,
      render: (_: any, record: StockItem) => (
        <Space>
          <Button 
            type="text" 
            icon={<EditOutlined />} 
            onClick={() => openDrawer(record)}
          />
          <Button 
            type="text" 
            danger 
            icon={<DeleteOutlined />} 
            onClick={() => handleDelete(record.key)}
          />
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: '0 0 80px 0' }}>
      <Breadcrumb style={{ marginBottom: 16 }}>
        <Breadcrumb.Item>{t('nav.dashboard')}</Breadcrumb.Item>
        <Breadcrumb.Item>{t('nav.stock_master')}</Breadcrumb.Item>
      </Breadcrumb>

      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Title level={3} style={{ margin: 0 }}>{t('stock.master.title')}</Title>
          <Text type="secondary">{t('stock.master.subtitle')}</Text>
        </div>
        <Space direction="vertical" align="end">
          <Input 
            placeholder={t('stock.master.search_placeholder')} 
            prefix={<SearchOutlined />} 
            style={{ width: 300, marginBottom: 16 }}
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAddRow}>
            {t('stock.master.add_row')}
          </Button>
        </Space>
      </div>

      <Card bodyStyle={{ padding: 0 }} style={{ overflow: 'hidden', borderRadius: 12, boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
        <Table 
          dataSource={filteredData} 
          columns={columns} 
          pagination={false} 
          locale={{ emptyText: <div style={{ padding: 40 }}><InboxOutlined style={{ fontSize: 32, color: '#d9d9d9' }} /><p>No data</p></div> }}
        />
      </Card>

      <Drawer
        title={
          <Space direction="vertical" size={0}>
            <span>{t('stock.drawer.title')}</span>
            {isMobile && (
              <Text type="secondary" style={{ fontSize: 12 }}>
                {syncStatus === 'saving' ? t('stock.drawer.sync.saving') : syncStatus === 'saved' ? t('stock.drawer.sync.saved') : t('stock.drawer.sync.ready')}
              </Text>
            )}
          </Space>
        }
        width={isMobile ? undefined : 400}
        height={isMobile ? '82vh' : undefined}
        placement={isMobile ? 'bottom' : 'right'}
        onClose={closeDrawer}
        open={isDrawerVisible}
        destroyOnClose={true}
        extra={isMobile ? null : (
          <Space>
            <Button onClick={closeDrawer}>Cancel</Button>
            <Button onClick={handleDrawerSave} type="primary">
              {t('stock.drawer.save')}
            </Button>
          </Space>
        )}
      >
        <Form form={form} layout="vertical" onValuesChange={handleDrawerValuesChange}>
          {isMobile && (
            <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
              {t('stock.drawer.autosave_hint')}
            </Text>
          )}
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
          {activeCategoryAttributes.map((field) => (
            <Form.Item key={field.key} name={['dynamicAttributes', field.key]} label={t(field.labelKey)}>
              {field.type === 'number' ? (
                <InputNumber style={{ width: '100%' }} />
              ) : field.type === 'select' ? (
                <Select options={field.options?.map((option) => ({ value: option, label: option }))} />
              ) : (
                <Input />
              )}
            </Form.Item>
          ))}
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
        </Form>
      </Drawer>

      <div style={{ 
        position: 'fixed', 
        bottom: 0, 
        right: 0, 
        left: 220, 
        background: 'rgba(255, 255, 255, 0.8)', 
        backdropFilter: 'blur(8px)',
        padding: '16px 24px',
        borderTop: '1px solid #f0f0f0',
        display: 'flex',
        justifyContent: 'flex-end',
        gap: 12,
        zIndex: 100
      }}>
        <Button size="large">{t('stock.master.discard')}</Button>
        <Button type="primary" size="large" icon={<SaveOutlined />} onClick={handlePublish} id="publish-btn">
          {t('stock.master.publish')}
        </Button>
      </div>
    </div>
  );
}

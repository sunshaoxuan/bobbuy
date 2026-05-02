import React, { useEffect, useState } from 'react';
import { Table, Button, Card, Modal, Form, Input, Space, Typography, message, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, EditOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { api, type MobileSupplier } from '../api';
import { useI18n } from '../i18n';

const { Text, Paragraph } = Typography;

const Suppliers: React.FC = () => {
  const { t } = useI18n();
  const [suppliers, setSuppliers] = useState<MobileSupplier[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<MobileSupplier | null>(null);
  const [form] = Form.useForm();

  const loadSuppliers = async () => {
    setLoading(true);
    try {
      const data = await api.suppliers();
      setSuppliers(data);
    } catch (error) {
      console.error('Failed to load suppliers:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSuppliers();
  }, []);

  const handleAdd = () => {
    setEditingSupplier(null);
    form.resetFields();
    setIsModalVisible(true);
  };

  const handleEdit = (record: MobileSupplier) => {
    setEditingSupplier(record);
    form.setFieldsValue({
      id: record.id,
      name_zh: record.name['zh-CN'],
      name_en: record.name['en-US'],
      description_zh: record.description?.['zh-CN'],
      contactInfo: record.contactInfo,
      onboardingRules: record.onboardingRules ? JSON.stringify(record.onboardingRules, null, 2) : ''
    });
    setIsModalVisible(true);
  };

  const handleCancel = () => {
    setIsModalVisible(false);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      let rulesObj = {};
      if (values.onboardingRules) {
        try {
          rulesObj = JSON.parse(values.onboardingRules);
        } catch (e) {
          message.error('Invalid JSON for onboarding rules');
          return;
        }
      }

      const payload: MobileSupplier = {
        id: values.id,
        name: { 'zh-CN': values.name_zh, 'en-US': values.name_en || values.name_zh },
        description: { 'zh-CN': values.description_zh || '' },
        contactInfo: values.contactInfo,
        onboardingRules: rulesObj
      };

      if (editingSupplier) {
        await api.updateSupplier(editingSupplier.id, payload);
        message.success('Supplier updated');
      } else {
        await api.createSupplier(payload);
        message.success('Supplier created');
      }
      setIsModalVisible(false);
      loadSuppliers();
    } catch (error) {
      console.error('Save failed:', error);
    }
  };

  const columns: ColumnsType<MobileSupplier> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
    },
    {
      title: t('stock.item.brand') || 'Brand / Name',
      key: 'name',
      render: (_, record) => record.name['zh-CN'] || record.id,
    },
    {
      title: 'Rules',
      key: 'rules',
      render: (_, record) => (
        record.onboardingRules && Object.keys(record.onboardingRules).length > 0 ? (
          <Tag color="blue" icon={<SafetyCertificateOutlined />}>
            {Object.keys(record.onboardingRules).length} Rules Defined
          </Tag>
        ) : <Text type="secondary">-</Text>
      )
    },
    {
      title: t('common.actions') || 'Actions',
      key: 'actions',
      width: 100,
      render: (_, record) => (
        <Button icon={<EditOutlined />} size="small" onClick={() => handleEdit(record)}>
          {t('common.edit')}
        </Button>
      ),
    },
  ];

  return (
    <div className="page-card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <div className="section-title" data-testid="suppliers-title">供应商檔案与 AI 規範</div>
          <Text type="secondary">定義不同商家的商品識別規則，例如 Costco 的 5 位數品番規範。</Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} data-testid="suppliers-submit">
          新增供应商
        </Button>
      </div>

      <Table
        dataSource={suppliers}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingSupplier ? '編輯供应商' : '新增供应商'}
        open={isModalVisible}
        onOk={handleSave}
        onCancel={handleCancel}
        width={600}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="id" label="供應商 ID (唯一標識)" rules={[{ required: true }]}>
            <Input placeholder="例如: costco" disabled={!!editingSupplier} />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="baseline">
            <Form.Item name="name_zh" label="中文名稱" rules={[{ required: true }]}>
              <Input placeholder="例如: Costco" />
            </Form.Item>
            <Form.Item name="name_en" label="英文名稱">
              <Input placeholder="Costco Wholesale" />
            </Form.Item>
          </Space>
          <Form.Item name="description_zh" label="簡介">
            <Input />
          </Form.Item>
          <Form.Item name="contactInfo" label="聯繫方式">
            <Input />
          </Form.Item>
          <Form.Item
            name="onboardingRules"
            label="AI 識別規則 (JSON)"
            extra='例如: {"itemNumberPattern": "5-6 digits only", "preferredLanguage": "JP"}'
          >
            <Input.TextArea rows={6} placeholder='{ "itemNumberRule": "Must be 5 or 6 digits" }' />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Suppliers;

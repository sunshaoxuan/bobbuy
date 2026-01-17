import { Button, Card, Form, Input, InputNumber, Select, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, Order } from '../api';
import { getStoredLocale, useI18n } from '../i18n';

const { Text } = Typography;

const statusOptions = ['NEW', 'CONFIRMED', 'PURCHASED', 'DELIVERED', 'SETTLED'];
const currencyOptions = ['CNY', 'JPY', 'USD', 'EUR'];

export default function Orders() {
  const { t } = useI18n();
  const [orders, setOrders] = useState<Order[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const refreshOrders = async () => {
    const refreshed = await api.orders();
    setOrders(refreshed);
  };

  const handleStatusChange = async (orderId: number, newStatus: string, currentStatus: string) => {
    if (newStatus === currentStatus) {
      return;
    }
    try {
      const response = await fetch(`/api/orders/${orderId}/status`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'Accept-Language': getStoredLocale()
        },
        body: JSON.stringify({ status: newStatus })
      });
      if (!response.ok) {
        throw new Error('request_failed');
      }
      message.success(t('orders.status.updated'));
      await refreshOrders();
    } catch {
      message.error(t('errors.request_failed'));
    }
  };

  const columns: ColumnsType<Order> = [
    { title: 'Business Key', dataIndex: 'businessKey' },
    {
      title: t('orders.table.item_name'),
      render: (_, record: Order) => record.items.map(i => i.itemName).join(', ')
    },
    {
      title: t('orders.table.quantity'),
      render: (_, record: Order) => record.items.reduce((sum: number, i) => sum + i.quantity, 0)
    },
    {
      title: t('orders.table.total'),
      render: (_value: unknown, record: Order) => {
        const itemsTotal = record.items.reduce((sum: number, i) => sum + i.unitPrice * i.quantity, 0);
        return (itemsTotal + record.serviceFee + record.estimatedTax).toFixed(2);
      }
    },
    { title: t('orders.table.currency'), dataIndex: 'currency' },
    {
      title: t('orders.table.status'),
      dataIndex: 'status',
      render: (status: Order['status']) => <Tag color="gold">{status}</Tag>
    },
    {
      title: t('orders.table.actions'),
      render: (_value: unknown, record) => (
        <Select
          value={record.status}
          onChange={(newStatus) => handleStatusChange(record.id, newStatus, record.status)}
          options={statusOptions.map((status) => ({ value: status, label: status }))}
          disabled={record.status === 'SETTLED'}
          style={{ width: 140 }}
        />
      )
    }
  ];

  useEffect(() => {
    refreshOrders();
  }, []);

  const handleSubmit = async (values: any) => {
    if (submitting) {
      return;
    }
    try {
      setSubmitting(true);
      // Map flat form values to Order + OrderItem structure
      const orderPayload = {
        businessKey: `${values.customerId}-${values.eventId}`,
        customerId: values.customerId,
        tripId: values.tripId,
        items: [
          {
            itemName: values.itemName,
            quantity: values.quantity,
            unitPrice: values.unitPrice,
            variable: !!values.variable
          }
        ],
        serviceFee: values.serviceFee,
        estimatedTax: values.estimatedTax,
        currency: values.currency,
        status: values.status
      };

      await api.createOrder(orderPayload as any);
      message.success(t('orders.form.success'));
      form.resetFields();
      await refreshOrders();
    } catch {
      // Errors are surfaced in the API layer.
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page-card">
      <div className="section-title">{t('orders.title')}</div>
      <Text className="helper-text">{t('orders.helper')}</Text>

      <Card style={{ marginTop: 16, marginBottom: 24 }} bordered={false}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ currency: 'CNY', status: 'NEW', quantity: 1, unitPrice: 0, serviceFee: 0, estimatedTax: 0, eventId: new Date().toISOString().slice(0, 10).replace(/-/g, ''), variable: false }}
        >
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <Form.Item
              label={t('orders.form.customer_id.label')}
              name="customerId"
              rules={[{ required: true, message: t('orders.form.customer_id.required') }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} placeholder={t('orders.form.customer_id.placeholder')} />
            </Form.Item>
            <Form.Item
              label="Event ID (Idempotency Key)"
              name="eventId"
              rules={[{ required: true, message: 'Please enter event ID' }]}
            >
              <Input placeholder="e.g. 2026011701" />
            </Form.Item>
          </div>

          <Form.Item
            label={t('orders.form.trip_id.label')}
            name="tripId"
            rules={[{ required: true, message: t('orders.form.trip_id.required') }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('orders.form.trip_id.placeholder')} />
          </Form.Item>
          <Form.Item
            label={t('orders.form.item_name.label')}
            name="itemName"
            rules={[{ required: true, message: t('orders.form.item_name.required') }]}
          >
            <Input placeholder={t('orders.form.item_name.placeholder')} />
          </Form.Item>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
            <Form.Item
              label={t('orders.form.quantity.label')}
              name="quantity"
              rules={[{ required: true, message: t('orders.form.quantity.required') }]}
            >
              <InputNumber min={1} style={{ width: '100%' }} placeholder={t('orders.form.quantity.placeholder')} />
            </Form.Item>
            <Form.Item
              label={t('orders.form.unit_price.label')}
              name="unitPrice"
              rules={[{ required: true, message: t('orders.form.unit_price.required') }]}
            >
              <InputNumber min={0} style={{ width: '100%' }} placeholder={t('orders.form.unit_price.placeholder')} />
            </Form.Item>
            <Form.Item label="Variable (Weight?)" name="variable" valuePropName="checked">
              <Select options={[{ value: false, label: 'Standard' }, { value: true, label: 'Weight/Variable' }]} />
            </Form.Item>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
            <Form.Item
              label={t('orders.form.service_fee.label')}
              name="serviceFee"
              rules={[{ required: true, message: t('orders.form.service_fee.required') }]}
            >
              <InputNumber min={0} style={{ width: '100%' }} placeholder={t('orders.form.service_fee.placeholder')} />
            </Form.Item>
            <Form.Item
              label={t('orders.form.estimated_tax.label')}
              name="estimatedTax"
              rules={[{ required: true, message: t('orders.form.estimated_tax.required') }]}
            >
              <InputNumber min={0} style={{ width: '100%' }} placeholder={t('orders.form.estimated_tax.placeholder')} />
            </Form.Item>
            <Form.Item
              label={t('orders.form.currency.label')}
              name="currency"
              rules={[{ required: true, message: t('orders.form.currency.required') }]}
            >
              <Select
                options={currencyOptions.map((currency) => ({ value: currency }))}
                placeholder={t('orders.form.currency.placeholder')}
              />
            </Form.Item>
          </div>

          <Form.Item
            label={t('orders.form.status.label')}
            name="status"
            rules={[{ required: true, message: t('orders.form.status.required') }]}
          >
            <Select options={statusOptions.map((status) => ({ value: status }))} placeholder={t('orders.form.status.placeholder')} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting} disabled={submitting}>
            {t('orders.form.submit')}
          </Button>
        </Form>
      </Card>

      <Card bordered={false} title={t('orders.list.title')}>
        <Table<Order>
          dataSource={orders}
          rowKey="id"
          columns={columns}
          locale={{ emptyText: t('table.empty') }}
        />
      </Card>
    </div>
  );
}

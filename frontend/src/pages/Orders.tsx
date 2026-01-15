import { Button, Card, Form, Input, InputNumber, Select, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, Order } from '../api';

const { Text } = Typography;

const statusOptions = ['NEW', 'CONFIRMED', 'PURCHASED', 'DELIVERED', 'SETTLED'];
const currencyOptions = ['CNY', 'JPY', 'USD', 'EUR'];

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);

  const columns: ColumnsType<Order> = [
    { title: '商品', dataIndex: 'itemName' },
    { title: '数量', dataIndex: 'quantity' },
    { title: '单价', dataIndex: 'unitPrice' },
    { title: '服务费', dataIndex: 'serviceFee' },
    { title: '税费预估', dataIndex: 'estimatedTax' },
    { title: '币种', dataIndex: 'currency' },
    {
      title: '总价',
      render: (_value: unknown, record) =>
        (record.quantity * record.unitPrice + record.serviceFee + record.estimatedTax).toFixed(2)
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (status: Order['status']) => <Tag color="gold">{status}</Tag>
    }
  ];

  useEffect(() => {
    api.orders().then(setOrders);
  }, []);

  return (
    <div className="page-card">
      <div className="section-title">订单确认与采购执行</div>
      <Text className="helper-text">记录委托需求，跟踪采购与交付状态。</Text>

      <Card style={{ marginTop: 16, marginBottom: 24 }} bordered={false}>
        <Form layout="vertical">
          <Form.Item label="商品名称" required>
            <Input placeholder="例如：Limited Edition Sneakers" />
          </Form.Item>
          <Form.Item label="数量">
            <InputNumber min={1} style={{ width: '100%' }} placeholder="填写需求数量" />
          </Form.Item>
          <Form.Item label="单价">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="填写单价" />
          </Form.Item>
          <Form.Item label="服务费">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="填写服务费" />
          </Form.Item>
          <Form.Item label="税费预估">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="填写税费预估" />
          </Form.Item>
          <Form.Item label="币种">
            <Select options={currencyOptions.map((currency) => ({ value: currency }))} placeholder="选择币种" />
          </Form.Item>
          <Form.Item label="状态">
            <Select options={statusOptions.map((status) => ({ value: status }))} placeholder="选择状态" />
          </Form.Item>
          <Button type="primary">创建订单</Button>
        </Form>
      </Card>

      <Card bordered={false} title="订单列表">
        <Table<Order>
          dataSource={orders}
          rowKey="id"
          columns={columns}
        />
      </Card>
    </div>
  );
}

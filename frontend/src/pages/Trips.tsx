import { Button, Card, Form, Input, InputNumber, Select, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, Trip } from '../api';

const { Text } = Typography;

const statusOptions = ['DRAFT', 'PUBLISHED', 'IN_PROGRESS', 'COMPLETED'];

export default function Trips() {
  const [trips, setTrips] = useState<Trip[]>([]);

  const columns: ColumnsType<Trip> = [
    { title: '出发地', dataIndex: 'origin' },
    { title: '目的地', dataIndex: 'destination' },
    { title: '出发日期', dataIndex: 'departDate' },
    { title: '容量', dataIndex: 'capacity' },
    { title: '剩余容量', dataIndex: 'remainingCapacity' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (status: Trip['status']) => <Tag color="blue">{status}</Tag>
    }
  ];

  useEffect(() => {
    api.trips().then(setTrips);
  }, []);

  return (
    <div className="page-card">
      <div className="section-title">行程发布与费用定价</div>
      <Text className="helper-text">快速创建新的代购行程，支持设置容量与状态。</Text>

      <Card style={{ marginTop: 16, marginBottom: 24 }} bordered={false}>
        <Form layout="vertical">
          <Form.Item label="出发地" required>
            <Input placeholder="例如：Tokyo" />
          </Form.Item>
          <Form.Item label="目的地" required>
            <Input placeholder="例如：Shanghai" />
          </Form.Item>
          <Form.Item label="出发日期" required>
            <Input type="date" />
          </Form.Item>
          <Form.Item label="可承载数量">
            <InputNumber min={1} style={{ width: '100%' }} placeholder="填写可承载订单数" />
          </Form.Item>
          <Form.Item label="状态">
            <Select options={statusOptions.map((status) => ({ value: status }))} placeholder="选择状态" />
          </Form.Item>
          <Button type="primary">保存行程</Button>
        </Form>
      </Card>

      <Card bordered={false} title="已发布行程">
        <Table<Trip>
          dataSource={trips}
          rowKey="id"
          columns={columns}
        />
      </Card>
    </div>
  );
}

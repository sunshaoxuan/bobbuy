import { Card, Col, Divider, Row, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, Metrics, Order, Trip } from '../api';
import StatCard from '../components/StatCard';

const { Text } = Typography;

export default function Dashboard() {
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);

  const tripColumns: ColumnsType<Trip> = [
    { title: '出发地', dataIndex: 'origin' },
    { title: '目的地', dataIndex: 'destination' },
    { title: '日期', dataIndex: 'departDate' },
    { title: '剩余容量', dataIndex: 'remainingCapacity' },
    {
      title: '状态',
      dataIndex: 'status',
      render: (status: Trip['status']) => <Tag color="blue">{status}</Tag>
    }
  ];

  const orderColumns: ColumnsType<Order> = [
    { title: '商品', dataIndex: 'itemName' },
    { title: '数量', dataIndex: 'quantity' },
    {
      title: '金额',
      render: (_value: unknown, record) => `¥ ${(record.unitPrice * record.quantity).toFixed(2)}`
    },
    {
      title: '状态',
      dataIndex: 'status',
      render: (status: Order['status']) => <Tag color="gold">{status}</Tag>
    }
  ];

  useEffect(() => {
    api.metrics().then(setMetrics);
    api.trips().then(setTrips);
    api.orders().then(setOrders);
  }, []);

  return (
    <div className="page-card">
      <div className="section-title">今日概览</div>
      <div className="dashboard-grid">
        <StatCard label="活跃参与者" value={metrics ? String(metrics.users) : '--'} helper="过去30天活跃" />
        <StatCard label="可承接行程" value={metrics ? String(metrics.trips) : '--'} helper="已发布行程" />
        <StatCard label="待履约订单" value={metrics ? String(metrics.orders) : '--'} helper="确认中/采购中" />
        <StatCard
          label="GMV"
          value={metrics ? `¥ ${metrics.gmV.toFixed(2)}` : '--'}
          helper="含税预估"
        />
      </div>

      <Divider />

      <Row gutter={16}>
        <Col span={12}>
          <Card bordered={false} title="最新行程">
            <Table<Trip>
              size="small"
              pagination={false}
              dataSource={trips}
              rowKey="id"
              columns={tripColumns}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card bordered={false} title="关键订单">
            <Table<Order>
              size="small"
              pagination={false}
              dataSource={orders}
              rowKey="id"
              columns={orderColumns}
            />
          </Card>
        </Col>
      </Row>

      <Divider />

      <Card bordered={false}>
        <div className="section-title">订单状态分布</div>
        <Text className="helper-text">
          {metrics?.orderStatusCounts
            ? Object.entries(metrics.orderStatusCounts)
                .map(([status, count]) => `${status}: ${count}`)
                .join(' · ')
            : '--'}
        </Text>
        <Divider />
        <Text className="helper-text">
          AI 助手已根据近期聊天记录生成采购建议。下一步可前往订单管理页面确认采购执行。
        </Text>
      </Card>
    </div>
  );
}

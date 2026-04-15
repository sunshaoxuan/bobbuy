import React, { useState } from 'react';
import { Card, Typography, Collapse, List, Checkbox, Tag, Space, Button, Progress, Row, Col, Statistic, Breadcrumb } from 'antd';
import { ShoppingOutlined, UserOutlined, CheckCircleOutlined, CameraOutlined, MessageOutlined, EllipsisOutlined } from '@ant-design/icons';
import { useI18n } from '../i18n';

const { Title, Text } = Typography;
const { Panel } = Collapse;

interface Item {
  id: string;
  name: string;
  sku: string;
  price: number;
  quantity: number;
  status: 'pending' | 'picked' | 'out_of_stock';
}

interface CustomerOrder {
  id: string;
  name: string;
  avatarLetter: string;
  items: Item[];
}

export default function ProcurementHUD() {
  const { t } = useI18n();
  const [orders, setOrders] = useState<CustomerOrder[]>([
    {
      id: 'C001',
      name: 'Alice Wang',
      avatarLetter: 'AW',
      items: [
        { id: 'I001', name: 'Organic Milk', sku: 'SKU-20934', price: 12.99, quantity: 2, status: 'picked' },
        { id: 'I002', name: 'Fresh Spinach', sku: 'SKU-88210', price: 4.49, quantity: 1, status: 'pending' }
      ]
    },
    {
      id: 'C002',
      name: 'Benjamin Tsui',
      avatarLetter: 'BT',
      items: [
        { id: 'I003', name: 'Fresh Strawberries', sku: 'SKU-77123', price: 5.99, quantity: 2, status: 'out_of_stock' }
      ]
    }
  ]);

  const totalItems = orders.reduce((sum, order) => sum + order.items.length, 0);
  const pickedItems = orders.reduce((sum, order) => sum + order.items.filter(i => i.status === 'picked').length, 0);
  const progressPercent = Math.round((pickedItems / totalItems) * 100);

  const togglePick = (orderId: string, itemId: string) => {
    setOrders(prev => prev.map(order => 
      order.id === orderId ? {
        ...order,
        items: order.items.map(item => 
          item.id === itemId ? { ...item, status: item.status === 'picked' ? 'pending' : 'picked' } : item
        )
      } : order
    ));
  };

  return (
    <div style={{ maxWidth: 600, margin: '0 auto', padding: '0 0 100px 0' }}>
      <Breadcrumb style={{ marginBottom: 16 }}>
        <Breadcrumb.Item>{t('nav.dashboard')}</Breadcrumb.Item>
        <Breadcrumb.Item>{t('nav.procurement')}</Breadcrumb.Item>
      </Breadcrumb>
      
      {/* HUD Header Sticky */}
      <Card style={{ 
        position: 'sticky', 
        top: 0, 
        zIndex: 10, 
        marginBottom: 16, 
        borderRadius: '0 0 16px 16px',
        boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        background: '#1890ff'
      }} bodyStyle={{ padding: 16 }}>
        <Row gutter={[16, 16]}>
          <Col span={12}>
            <div style={{ background: 'rgba(255,255,255,0.1)', padding: 12, borderRadius: 12, color: 'white' }}>
              <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.7em', textTransform: 'uppercase' }}>Items</Text>
              <div style={{ fontSize: '1.5em', fontWeight: 'black', lineHeight: 1 }}>{totalItems}</div>
            </div>
          </Col>
          <Col span={12}>
            <div style={{ background: 'rgba(255,255,255,0.1)', padding: 12, borderRadius: 12, color: 'white' }}>
              <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.7em', textTransform: 'uppercase' }}>Done</Text>
              <div style={{ fontSize: '1.5em', fontWeight: 'black', lineHeight: 1 }}>{pickedItems}</div>
            </div>
          </Col>
          <Col span={12}>
            <div style={{ background: 'rgba(255,255,255,0.1)', padding: 12, borderRadius: 12, color: 'white' }}>
              <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.7em', textTransform: 'uppercase' }}>Customers</Text>
              <div style={{ fontSize: '1.5em', fontWeight: 'black', lineHeight: 1 }}>{orders.length}</div>
            </div>
          </Col>
          <Col span={12}>
            <div style={{ background: 'rgba(255,255,255,0.1)', padding: 12, borderRadius: 12, color: 'white' }}>
              <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.7em', textTransform: 'uppercase' }}>Progress</Text>
              <div style={{ fontSize: '1.5em', fontWeight: 'black', lineHeight: 1 }}>{progressPercent}%</div>
            </div>
          </Col>
        </Row>
      </Card>

      {/* Main List */}
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        {orders.map(order => (
          <Card key={order.id} size="small" style={{ borderRadius: 16, overflow: 'hidden' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: 12 }}>
              <div style={{ 
                width: 48, height: 48, borderRadius: 12, 
                backgroundColor: '#f0f5ff', color: '#1890ff',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontWeight: 'black', fontSize: '1em'
              }}>
                {order.avatarLetter}
              </div>
              <div style={{ flex: 1 }}>
                <Title level={5} style={{ margin: 0 }}>{order.name}</Title>
                <Text type="secondary" style={{ fontSize: '0.8em' }}>
                  {order.items.filter(i => i.status === 'picked').length} / {order.items.length} Picked
                </Text>
              </div>
              <Button type="text" icon={<MessageOutlined />} onClick={() => window.location.href='/order-desk'} />
            </div>
            
            <div style={{ padding: '0 12px 12px 12px' }}>
              <List
                dataSource={order.items}
                renderItem={(item) => (
                  <div style={{ 
                    display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0', 
                    borderTop: '1px solid #f0f0f0',
                    opacity: item.status === 'out_of_stock' ? 0.5 : 1
                  }}>
                    <div style={{ width: 50, height: 50, borderRadius: 8, background: '#f5f5f5', flexShrink: 0 }} />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 'bold', fontSize: '0.9em' }}>{item.name}</div>
                      <div style={{ fontSize: '0.7em', color: '#8c8c8c' }}>{item.sku} | ${item.price}</div>
                    </div>
                    <div style={{ textAlign: 'center', minWidth: 40 }}>
                      <div style={{ fontSize: '0.7em', fontWeight: 'bold', color: '#8c8c8c' }}>QTY</div>
                      <div style={{ fontSize: '1.2em', fontWeight: 'bold' }}>{item.quantity}</div>
                    </div>
                    {item.status === 'out_of_stock' ? (
                      <Tag color="error">OOS</Tag>
                    ) : (
                      <Button 
                        shape="circle" 
                        size="large"
                        type={item.status === 'picked' ? 'primary' : 'default'}
                        style={{ backgroundColor: item.status === 'picked' ? '#52c41a' : undefined }}
                        icon={<CheckCircleOutlined />}
                        onClick={() => togglePick(order.id, item.id)}
                      />
                    )}
                  </div>
                )}
              />
            </div>
          </Card>
        ))}
      </Space>

      {/* Floating Action Button */}
      <div style={{ 
        position: 'fixed', bottom: 24, left: '50%', transform: 'translateX(-50%)', 
        display: 'flex', gap: 16, zIndex: 100 
      }}>
        <Button size="large" type="primary" shape="round" icon={<CameraOutlined />} style={{ height: 60, padding: '0 32px', fontSize: '1.2em', fontWeight: 'bold', boxShadow: '0 8px 24px rgba(24, 144, 255, 0.4)' }}>
          AI PHOTO IDENTIFY
        </Button>
      </div>
    </div>
  );
}

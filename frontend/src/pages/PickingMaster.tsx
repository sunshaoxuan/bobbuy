import React, { useState } from 'react';
import { Card, Typography, List, Row, Col, Button, Image, Tag, Space, Badge, Radio, Breadcrumb } from 'antd';
import { CheckCircleFilled, AppstoreOutlined, ShoppingCartOutlined, ContainerOutlined } from '@ant-design/icons';
import { useI18n } from '../i18n';

const { Title, Text } = Typography;

interface PickItem {
  id: string;
  name: string;
  customerName: string;
  sku: string;
  picked: boolean;
  imageUrl: string;
}

export default function PickingMaster() {
  const { t } = useI18n();
  const [filter, setFilter] = useState('all');
  const [items, setItems] = useState<PickItem[]>([
    { id: 'I001', name: 'Organic Milk', customerName: 'Alice Wang', sku: 'SKU-20934', picked: true, imageUrl: '/assets/products/milk.png' },
    { id: 'I002', name: 'Fresh Spinach', customerName: 'Alice Wang', sku: 'SKU-88210', picked: false, imageUrl: '/assets/products/spinach.png' },
    { id: 'I004', name: 'Rotisserie Chicken', customerName: 'Alex Cos', sku: 'SKU-111', picked: true, imageUrl: '/assets/products/chicken.png' },
    { id: 'I005', name: 'Paper Towels', customerName: 'Alex Cos', sku: 'SKU-222', picked: false, imageUrl: '/assets/products/paper_towels.png' }
  ]);

  const togglePick = (id: string) => {
    setItems(prev => prev.map(item => 
      item.id === id ? { ...item, picked: !item.picked } : item
    ));
  };

  const filteredItems = items.filter(i => {
    if (filter === 'todo') return !i.picked;
    if (filter === 'done') return i.picked;
    return true;
  });

  return (
    <div style={{ padding: '0 0 40px 0' }}>
      <Breadcrumb style={{ marginBottom: 16 }}>
        <Breadcrumb.Item>{t('nav.dashboard')}</Breadcrumb.Item>
        <Breadcrumb.Item>{t('nav.picking')}</Breadcrumb.Item>
      </Breadcrumb>

      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>{t('picking.title')}</Title>
          <Text type="secondary">{t('picking.subtitle')}</Text>
        </div>
        <Radio.Group value={filter} onChange={e => setFilter(e.target.value)} buttonStyle="solid">
          <Radio.Button value="all">{t('picking.filter.all')}</Radio.Button>
          <Radio.Button value="todo">{t('picking.filter.todo')} ({items.filter(i => !i.picked).length})</Radio.Button>
          <Radio.Button value="done">{t('picking.filter.done')} ({items.filter(i => i.picked).length})</Radio.Button>
        </Radio.Group>
      </div>

      <Row gutter={[16, 16]}>
        {filteredItems.map(item => (
          <Col key={item.id} xs={12} sm={8} md={6} lg={4}>
            <Card 
              hoverable
              bodyStyle={{ padding: 12 }}
              cover={
                <div 
                  style={{ position: 'relative', overflow: 'hidden', cursor: 'pointer' }}
                  onClick={() => togglePick(item.id)}
                >
                  <img alt={item.name} src={item.imageUrl} style={{ width: '100%', height: 120, objectFit: 'cover' }} />
                  {item.picked && (
                    <div style={{ 
                      position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, 
                      backgroundColor: 'rgba(76, 175, 80, 0.4)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      transition: 'all 0.3s'
                    }}>
                      <CheckCircleFilled style={{ color: 'white', fontSize: '3em' }} />
                    </div>
                  )}
                  <div style={{ position: 'absolute', top: 8, right: 8 }}>
                    <Tag color={item.picked ? 'success' : 'warning'}>{item.picked ? t('picking.filter.done') : t('picking.filter.todo')}</Tag>
                  </div>
                </div>
              }
            >
              <Card.Meta 
                title={<div style={{ fontSize: '0.9em', fontWeight: 'bold' }}>{item.name}</div>}
                description={
                  <Space direction="vertical" size={0} style={{ width: '100%', marginTop: 4 }}>
                    <Text type="secondary" style={{ fontSize: '0.8em' }}>{item.sku}</Text>
                    <Badge color="blue" text={<Text style={{ fontSize: '0.8em' }}>{item.customerName}</Text>} />
                  </Space>
                }
              />
            </Card>
          </Col>
        ))}
      </Row>

      {items.length === 0 && (
        <Card style={{ textAlign: 'center', padding: '40px 0' }}>
          <ShoppingCartOutlined style={{ fontSize: '3em', color: '#f0f0f0', marginBottom: 16 }} />
          <div>{t('picking.empty')}</div>
        </Card>
      )}
    </div>
  );
}

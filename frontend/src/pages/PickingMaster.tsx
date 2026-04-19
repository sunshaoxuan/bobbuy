import React, { useState, useEffect } from 'react';
import { Card, Typography, List, Row, Col, Image, Tag, Space, Badge, Radio, Breadcrumb, Progress, Skeleton, Modal, Avatar } from 'antd';
import { CheckCircleFilled, ShoppingCartOutlined, UnorderedListOutlined, UserOutlined } from '@ant-design/icons';
import { useI18n } from '../i18n';
import { api, ProcurementItemResponse } from '../api';

const { Title, Text } = Typography;

export default function PickingMaster() {
  const { t } = useI18n();
  const [filter, setFilter] = useState('all');
  const [detailsItem, setDetailsItem] = useState<ProcurementItemResponse | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    const data = await api.procurementList(activeTripId);
    setItems(data);
    setLoading(false);
  };

  const filteredItems = items.filter(i => {
    const isDone = i.purchasedQuantity >= i.totalQuantity;
    if (filter === 'todo') return !isDone;
    if (filter === 'done') return isDone;
    return true;
  });

  if (loading) {
    return (
      <div style={{ padding: '24px' }}>
        <Skeleton active />
        <Skeleton active style={{ marginTop: 24 }} />
      </div>
    );
  }

  return (
    <div style={{ padding: '0 0 40px 0' }}>
      <Breadcrumb style={{ marginBottom: 16 }}>
        <Breadcrumb.Item>{t('nav.dashboard')}</Breadcrumb.Item>
        <Breadcrumb.Item>{t('nav.picking')}</Breadcrumb.Item>
      </Breadcrumb>

      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <Title level={4} style={{ margin: 0, fontFamily: "'Noto Serif JP', serif" }}>{t('picking.title')}</Title>
          <Text type="secondary">{t('picking.subtitle')} (Trip #{activeTripId})</Text>
        </div>
        <Radio.Group value={filter} onChange={e => setFilter(e.target.value)} buttonStyle="solid">
          <Radio.Button value="all">{t('picking.filter.all')}</Radio.Button>
          <Radio.Button value="todo">{t('picking.filter.todo')} ({items.filter(i => i.purchasedQuantity < i.totalQuantity).length})</Radio.Button>
          <Radio.Button value="done">{t('picking.filter.done')} ({items.filter(i => i.purchasedQuantity >= i.totalQuantity).length})</Radio.Button>
        </Radio.Group>
      </div>

      <Row gutter={[16, 16]}>
        {filteredItems.map(item => {
          const isDone = item.purchasedQuantity >= item.totalQuantity;
          const percent = Math.min(Math.round((item.purchasedQuantity / item.totalQuantity) * 100), 100);
          
          return (
            <Col key={item.skuId} xs={24} sm={12} md={8} lg={6}>
              <Card 
                hoverable
                bodyStyle={{ padding: 16 }}
                className="procurement-glass-card"
              >
                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <div style={{ position: 'relative', flexShrink: 0 }}>
                    <Badge count={item.customerIdList.length} overflowCount={9} offset={[-2, 2]}>
                      <div style={{ 
                        width: 64, height: 64, 
                        background: '#f5f5f5', 
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        borderRadius: '8px'
                      }}>
                        <UnorderedListOutlined style={{ fontSize: '24px', color: '#bfbfbf' }} />
                      </div>
                    </Badge>
                  </div>
                  
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Text strong style={{ display: 'block', fontSize: '1.1rem', marginBottom: 4 }}>
                      {item.itemName}
                    </Text>
                    <Text type="secondary" style={{ fontSize: '0.85rem' }}>{item.skuId}</Text>
                    
                    <div style={{ marginTop: 12 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                        <Text style={{ fontSize: '0.85rem' }}>
                          {item.purchasedQuantity} / {item.totalQuantity}
                        </Text>
                        {isDone && <CheckCircleFilled style={{ color: '#52c41a' }} />}
                      </div>
                      <Progress 
                        percent={percent} 
                        size="small" 
                        showInfo={false} 
                        strokeColor={isDone ? '#52c41a' : '#1890ff'}
                      />
                    </div>
                  </div>
                </div>
                
                <div style={{ marginTop: 16, paddingTop: 12, borderTop: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Space>
                    <Tag color="cyan">￥{item.unitPrice}</Tag>
                    <Text type="secondary" style={{ fontSize: '0.75rem' }}>
                      {item.customerIdList.length} {t('picking.customers')}
                    </Text>
                  </Space>
                  <Button size="small" type="link" onClick={() => setDetailsItem(item)}>
                    {t('picking.details')}
                  </Button>
                </div>
              </Card>
            </Col>
          );
        })}
      </Row>

      <Modal
        title={detailsItem?.itemName}
        open={!!detailsItem}
        onCancel={() => setDetailsItem(null)}
        footer={null}
        className="zen-modal"
      >
        <List
          header={<Text strong>{t('picking.customer_breakdown') || 'Customer Breakdown'}</Text>}
          dataSource={detailsItem?.customerIdList || []}
          renderItem={(cid) => (
            <List.Item>
              <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                <Space>
                  <Avatar size="small" icon={<UserOutlined />} />
                  <Text>{t('picking.customer_id') || 'Customer ID'}: {cid}</Text>
                </Space>
                <Tag color="blue">1 {t('common.unit') || 'unit'}</Tag>
              </Space>
            </List.Item>
          )}
        />
      </Modal>

      {items.length === 0 && (
        <Card style={{ textAlign: 'center', padding: '60px 0', border: 'var(--zen-line)' }}>
          <ShoppingCartOutlined style={{ fontSize: '3em', color: '#f0f0f0', marginBottom: 16 }} />
          <div style={{ color: 'var(--zen-muted)' }}>{t('picking.empty')}</div>
        </Card>
      )}
    </div>
  );
}

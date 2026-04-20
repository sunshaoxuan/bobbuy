import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Avatar,
  Badge,
  Breadcrumb,
  Button,
  Card,
  Col,
  List,
  Modal,
  Progress,
  Radio,
  Row,
  Select,
  Skeleton,
  Space,
  Tag,
  Typography
} from 'antd';
import { CheckCircleFilled, ShoppingCartOutlined, UnorderedListOutlined, UserOutlined } from '@ant-design/icons';
import { api, type Order, type ProcurementItemResponse, type Trip } from '../api';
import { useI18n } from '../i18n';

const { Title, Text } = Typography;

type ProcurementBreakdownRow = {
  businessId: string;
  quantity: number;
  purchasedQuantity: number;
};

export default function PickingMaster() {
  const { t } = useI18n();
  const [filter, setFilter] = useState('all');
  const [loading, setLoading] = useState(true);
  const [trips, setTrips] = useState<Trip[]>([]);
  const [activeTripId, setActiveTripId] = useState<number>();
  const [items, setItems] = useState<ProcurementItemResponse[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [detailsItem, setDetailsItem] = useState<ProcurementItemResponse | null>(null);

  const loadTripData = useCallback(async (tripId: number) => {
    setLoading(true);
    try {
      const [procurementItems, orderList] = await Promise.all([
        api.procurementList(tripId),
        api.orders(tripId)
      ]);
      setItems(procurementItems);
      setOrders(orderList);
    } catch {
      setItems([]);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      setLoading(true);
      try {
        const tripList = await api.trips();
        if (cancelled) {
          return;
        }
        setTrips(tripList);
        const firstTripId = tripList[0]?.id;
        setActiveTripId(firstTripId);
        if (firstTripId) {
          await loadTripData(firstTripId);
        } else {
          setItems([]);
          setOrders([]);
          setLoading(false);
        }
      } catch {
        if (!cancelled) {
          setTrips([]);
          setItems([]);
          setOrders([]);
          setLoading(false);
        }
      }
    };

    void bootstrap();
    return () => {
      cancelled = true;
    };
  }, [loadTripData]);

  const filteredItems = useMemo(
    () =>
      items.filter((item) => {
        const isDone = item.purchasedQuantity >= item.totalQuantity;
        if (filter === 'todo') return !isDone;
        if (filter === 'done') return isDone;
        return true;
      }),
    [filter, items]
  );

  const detailRows = useMemo<ProcurementBreakdownRow[]>(() => {
    if (!detailsItem) {
      return [];
    }
    const breakdown = new Map<string, ProcurementBreakdownRow>();
    for (const order of orders) {
      const matchedLine = (order.lines ?? []).find((line) => line.skuId === detailsItem.skuId);
      if (!matchedLine) {
        continue;
      }
      const current = breakdown.get(order.businessId) ?? {
        businessId: order.businessId,
        quantity: 0,
        purchasedQuantity: 0
      };
      current.quantity += matchedLine.quantity ?? 0;
      current.purchasedQuantity += matchedLine.purchasedQuantity ?? 0;
      breakdown.set(order.businessId, current);
    }
    return Array.from(breakdown.values()).sort((left, right) => left.businessId.localeCompare(right.businessId));
  }, [detailsItem, orders]);

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
      <Breadcrumb style={{ marginBottom: 16 }} items={[{ title: t('nav.dashboard') }, { title: t('nav.picking') }]} />

      <div
        style={{
          marginBottom: 24,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '16px'
        }}
      >
        <div>
          <Title level={4} style={{ margin: 0, fontFamily: "'Noto Serif JP', serif" }}>
            {t('picking.title')}
          </Title>
          <Text type="secondary">
            {t('picking.subtitle')}
            {activeTripId ? ` (Trip #${activeTripId})` : ''}
          </Text>
        </div>
        <Space wrap>
          <Select
            value={activeTripId}
            placeholder={t('procurement.trip_selector')}
            options={trips.map((trip) => ({
              value: trip.id,
              label: `${trip.id} · ${trip.origin} → ${trip.destination}`
            }))}
            style={{ minWidth: 220 }}
            onChange={(tripId) => {
              setActiveTripId(tripId);
              void loadTripData(tripId);
            }}
          />
          <Radio.Group value={filter} onChange={(event) => setFilter(event.target.value)} buttonStyle="solid">
            <Radio.Button value="all">{t('picking.filter.all')}</Radio.Button>
            <Radio.Button value="todo">
              {t('picking.filter.todo')} ({items.filter((item) => item.purchasedQuantity < item.totalQuantity).length})
            </Radio.Button>
            <Radio.Button value="done">
              {t('picking.filter.done')} ({items.filter((item) => item.purchasedQuantity >= item.totalQuantity).length})
            </Radio.Button>
          </Radio.Group>
        </Space>
      </div>

      <Row gutter={[16, 16]}>
        {filteredItems.map((item) => {
          const isDone = item.purchasedQuantity >= item.totalQuantity;
          const percent = item.totalQuantity <= 0
            ? 0
            : Math.min(Math.round((item.purchasedQuantity / item.totalQuantity) * 100), 100);

          return (
            <Col key={item.skuId} xs={24} sm={12} md={8} lg={6}>
              <Card hoverable styles={{ body: { padding: 16 } }} className="procurement-glass-card">
                <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <div style={{ position: 'relative', flexShrink: 0 }}>
                    <Badge count={item.businessIds.length} overflowCount={9} offset={[-2, 2]}>
                      <div
                        style={{
                          width: 64,
                          height: 64,
                          background: '#f5f5f5',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          borderRadius: '8px'
                        }}
                      >
                        <UnorderedListOutlined style={{ fontSize: '24px', color: '#bfbfbf' }} />
                      </div>
                    </Badge>
                  </div>

                  <div style={{ flex: 1, minWidth: 0 }}>
                    <Text strong style={{ display: 'block', fontSize: '1.1rem', marginBottom: 4 }}>
                      {item.itemName}
                    </Text>
                    <Text type="secondary" style={{ fontSize: '0.85rem' }}>
                      {item.skuId}
                    </Text>

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

                <div
                  style={{
                    marginTop: 16,
                    paddingTop: 12,
                    borderTop: '1px solid #f0f0f0',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center'
                  }}
                >
                  <Space>
                    <Tag color="cyan">￥{item.unitPrice}</Tag>
                    <Text type="secondary" style={{ fontSize: '0.75rem' }}>
                      {item.businessIds.length} {t('procurement.customers')}
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
          header={<Text strong>{t('procurement.reconcile_detail')}</Text>}
          dataSource={detailRows}
          renderItem={(row) => (
            <List.Item>
              <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                <Space>
                  <Avatar size="small" icon={<UserOutlined />} />
                  <Text>
                    {t('procurement.business_id')}: {row.businessId}
                  </Text>
                </Space>
                <Tag color="blue">
                  {row.purchasedQuantity}/{row.quantity}
                </Tag>
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

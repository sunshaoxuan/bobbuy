import { useCallback, useEffect, useMemo, useState } from 'react';
import { Breadcrumb, Card, Col, Empty, Progress, Row, Select, Space, Statistic, Table, Typography } from 'antd';
import { api, type Order, type ProcurementHudStats, type Trip } from '../api';
import { useI18n } from '../i18n';

const { Text } = Typography;

type ReconcileDetailRow = {
  key: string;
  skuId: string;
  itemName: string;
  businessId: string;
  purchasedQuantity: number;
};

export default function ProcurementDashboard() {
  const { t } = useI18n();
  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<number>();
  const [hudStats, setHudStats] = useState<ProcurementHudStats>();
  const [reconcileRows, setReconcileRows] = useState<ReconcileDetailRow[]>([]);
  const [loading, setLoading] = useState(false);

  const refreshTripData = useCallback(async (tripId: number) => {
    setLoading(true);
    try {
      const [hud, orders] = await Promise.all([api.procurementHud(tripId), api.orders(tripId)]);
      setHudStats(hud);
      setReconcileRows(buildReconcileRows(orders));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    api.trips().then((result) => {
      if (cancelled) {
        return;
      }
      setTrips(result);
      if (result.length > 0) {
        const nextTripId = result[0].id;
        setSelectedTripId(nextTripId);
        refreshTripData(nextTripId);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [refreshTripData]);

  useEffect(() => {
    const handler = (event: Event) => {
      const detail = (event as CustomEvent<{ tripId?: number }>).detail;
      if (!selectedTripId || (detail?.tripId && detail.tripId !== selectedTripId)) {
        return;
      }
      refreshTripData(selectedTripId);
    };
    window.addEventListener('procurement:reconciled', handler);
    return () => window.removeEventListener('procurement:reconciled', handler);
  }, [refreshTripData, selectedTripId]);

  const selectedTrip = useMemo(
    () => trips.find((trip) => trip.id === selectedTripId),
    [selectedTripId, trips]
  );

  const grossMarginRate = useMemo(() => {
    if (!hudStats) {
      return 0;
    }
    const expectedRevenue = hudStats.totalEstimatedProfit + hudStats.currentPurchasedAmount;
    if (expectedRevenue <= 0) {
      return 0;
    }
    return Number(((hudStats.totalEstimatedProfit * 100) / expectedRevenue).toFixed(2));
  }, [hudStats]);

  const maxWeight = selectedTrip?.capacity ?? 0;
  const currentWeight = hudStats?.currentWeight ?? 0;
  const loadPercent = maxWeight <= 0 ? 0 : Math.min((currentWeight / maxWeight) * 100, 100);

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Breadcrumb items={[{ title: t('nav.dashboard') }, { title: t('nav.procurement') }]} />
      <Card>
        <Space direction="vertical" style={{ width: '100%' }} size={12}>
          <Text strong>{t('procurement.trip_selector')}</Text>
          <Select
            value={selectedTripId}
            placeholder={t('orders.trip.select.placeholder')}
            options={trips.map((trip) => ({
              value: trip.id,
              label: `${trip.id} · ${trip.origin} → ${trip.destination}`
            }))}
            onChange={(tripId) => {
              setSelectedTripId(tripId);
              refreshTripData(tripId);
            }}
          />
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card title={t('procurement.profit_insight')} loading={loading}>
            <Row gutter={16}>
              <Col span={12}>
                <Statistic
                  title={t('procurement.estimated_profit')}
                  value={hudStats?.totalEstimatedProfit ?? 0}
                  precision={2}
                />
              </Col>
              <Col span={12}>
                <Statistic title={t('procurement.gross_margin_rate')} value={grossMarginRate} suffix="%" precision={2} />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card title={t('procurement.capacity_redline')} loading={loading}>
            <Progress percent={Number(loadPercent.toFixed(2))} status={loadPercent >= 100 ? 'exception' : 'active'} />
            <Text type="secondary">
              {t('procurement.current_weight_label')} {currentWeight.toFixed(2)} / {maxWeight.toFixed(2)}
            </Text>
          </Card>
        </Col>
      </Row>

      <Card title={t('procurement.reconcile_detail')} loading={loading}>
        {reconcileRows.length === 0 ? (
          <Empty description={t('procurement.no_reconcile_data')} />
        ) : (
          <Table<ReconcileDetailRow>
            rowKey="key"
            size="small"
            pagination={false}
            dataSource={reconcileRows}
            columns={[
              { title: t('orders.lines.sku_id'), dataIndex: 'skuId', key: 'skuId' },
              { title: t('orders.lines.item_name'), dataIndex: 'itemName', key: 'itemName' },
              { title: t('procurement.reconcile_quantity'), dataIndex: 'purchasedQuantity', key: 'purchasedQuantity' },
              { title: t('procurement.business_id'), dataIndex: 'businessId', key: 'businessId' }
            ]}
          />
        )}
      </Card>
    </Space>
  );
}

function buildReconcileRows(orders: Order[]): ReconcileDetailRow[] {
  return orders.flatMap((order) =>
    (order.lines ?? [])
      .filter((line) => (line.purchasedQuantity ?? 0) > 0)
      .map((line) => ({
        key: `${order.id}-${line.skuId}-${line.itemName}`,
        skuId: line.skuId,
        itemName: line.itemName,
        businessId: order.businessId,
        purchasedQuantity: line.purchasedQuantity ?? 0
      }))
  );
}

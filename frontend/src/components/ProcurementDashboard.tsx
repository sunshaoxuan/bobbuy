import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Breadcrumb,
  Button,
  Card,
  Col,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Timeline,
  Typography,
  message
} from 'antd';
import {
  api,
  type CustomerBalanceLedgerEntry,
  type FinancialAuditLog,
  type Order,
  type ProcurementHudStats,
  type Trip,
  type TripExpense
} from '../api';
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
  const [orders, setOrders] = useState<Order[]>([]);
  const [expenses, setExpenses] = useState<TripExpense[]>([]);
  const [auditLogs, setAuditLogs] = useState<FinancialAuditLog[]>([]);
  const [ledgerEntries, setLedgerEntries] = useState<CustomerBalanceLedgerEntry[]>([]);
  const [reconcileRows, setReconcileRows] = useState<ReconcileDetailRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRow, setEditingRow] = useState<ReconcileDetailRow>();
  const [targetBusinessId, setTargetBusinessId] = useState<string>();
  const [transferQuantity, setTransferQuantity] = useState<number>(1);
  const [expenseForm] = Form.useForm<{ category: string; cost: number }>();
  const refreshRequestRef = useRef(0);

  const refreshTripData = useCallback(async (tripId: number) => {
    const requestId = refreshRequestRef.current + 1;
    refreshRequestRef.current = requestId;
    setLoading(true);
    try {
      const [hud, orderList, expenseList, tripAuditLogs, customerLedger] = await Promise.all([
        api.procurementHud(tripId),
        api.orders(tripId),
        api.procurementExpenses(tripId),
        api.procurementAuditLogs(tripId),
        api.customerBalanceLedger(tripId)
      ]);
      if (refreshRequestRef.current !== requestId) {
        return;
      }
      setHudStats(hud);
      setOrders(orderList);
      setExpenses(expenseList);
      setAuditLogs(tripAuditLogs);
      setLedgerEntries(customerLedger);
      setReconcileRows(buildReconcileRows(orderList));
    } finally {
      if (refreshRequestRef.current === requestId) {
        setLoading(false);
      }
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

  useEffect(() => {
    if (!selectedTripId) {
      return;
    }
    const timer = window.setInterval(() => refreshTripData(selectedTripId), 15000);
    return () => window.clearInterval(timer);
  }, [refreshTripData, selectedTripId]);

  const selectedTrip = useMemo(() => trips.find((trip) => trip.id === selectedTripId), [selectedTripId, trips]);

  const grossMarginRate = useMemo(() => {
    if (!hudStats) {
      return 0;
    }
    const expectedRevenue = hudStats.totalEstimatedProfit + hudStats.currentPurchasedAmount + hudStats.totalTripExpenses;
    if (expectedRevenue <= 0) {
      return 0;
    }
    return Number(((hudStats.totalEstimatedProfit * 100) / expectedRevenue).toFixed(2));
  }, [hudStats]);

  const trendArrow = grossMarginRate >= 0 ? '▲' : '▼';
  const trendColor = grossMarginRate >= 0 ? 'green' : 'red';
  const maxWeight = selectedTrip?.capacity ?? 0;
  const currentWeight = hudStats?.currentWeight ?? 0;
  const loadPercent = maxWeight <= 0 ? 0 : Math.min((currentWeight / maxWeight) * 100, 100);

  const targetBusinessOptions = useMemo(() => {
    if (!editingRow) {
      return [];
    }
    return orders
      .filter((order) => order.businessId !== editingRow.businessId)
      .filter((order) => (order.lines ?? []).some((line) => line.skuId === editingRow.skuId))
      .map((order) => ({ label: order.businessId, value: order.businessId }));
  }, [editingRow, orders]);

  const openReconcileModal = (row: ReconcileDetailRow) => {
    setEditingRow(row);
    setTransferQuantity(1);
    setTargetBusinessId(undefined);
    setIsModalOpen(true);
  };

  const submitManualReconcile = async () => {
    if (!selectedTripId || !editingRow || !targetBusinessId || transferQuantity <= 0) {
      return;
    }
    const result = await api.manualReconcile(selectedTripId, {
      skuId: editingRow.skuId,
      fromBusinessId: editingRow.businessId,
      toBusinessId: targetBusinessId,
      quantity: transferQuantity
    });
    if (result.transferredQuantity <= 0) {
      message.warning(t('procurement.reconcile_no_change'));
    } else {
      message.success(`${t('procurement.reconcile_success_prefix')} ${result.transferredQuantity}`);
    }
    await refreshTripData(selectedTripId);
    setIsModalOpen(false);
  };

  const submitExpense = async () => {
    if (!selectedTripId) {
      return;
    }
    const values = await expenseForm.validateFields();
    await api.createProcurementExpense(selectedTripId, { category: values.category, cost: values.cost });
    expenseForm.resetFields();
    await refreshTripData(selectedTripId);
  };

  const exportSettlement = async (format: 'csv' | 'pdf') => {
    if (!selectedTripId) {
      return;
    }
    const blob = await api.exportProcurementSettlement(selectedTripId, format);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `trip-${selectedTripId}-settlement.${format}`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  };

  const exportCustomerStatement = async (businessId: string) => {
    if (!selectedTripId) {
      return;
    }
    const blob = await api.exportCustomerStatement(selectedTripId, businessId);
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `trip-${selectedTripId}-customer-${businessId}-statement.pdf`;
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Breadcrumb items={[{ title: t('nav.dashboard') }, { title: t('nav.procurement') }]} />
      <Card className="procurement-glass-card">
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
          <Space wrap>
            <Button onClick={() => exportSettlement('csv')}>{t('procurement.export_csv')}</Button>
            <Button onClick={() => exportSettlement('pdf')}>{t('procurement.export_pdf')}</Button>
          </Space>
        </Space>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Card title={t('procurement.profit_insight')} loading={loading} className="procurement-glass-card">
            <Row gutter={16}>
              <Col span={12}>
                <Statistic title={t('procurement.estimated_profit')} value={hudStats?.totalEstimatedProfit ?? 0} precision={2} />
              </Col>
              <Col span={12}>
                <Statistic title={t('procurement.gross_margin_rate')} value={grossMarginRate} suffix="%" precision={2} />
                <Tag color={trendColor}>{trendArrow}</Tag>
              </Col>
            </Row>
            <Text type="secondary">
              {t('procurement.fx_rate_label')} {hudStats?.currentFxRate ?? 1} / {hudStats?.referenceFxRate ?? 1}
            </Text>
          </Card>
        </Col>
        <Col xs={24} md={12}>
          <Card title={t('procurement.capacity_redline')} loading={loading} className="procurement-glass-card">
            <Progress percent={Number(loadPercent.toFixed(2))} status={loadPercent >= 100 ? 'exception' : 'active'} />
            <Text type="secondary">
              {t('procurement.current_weight_label')} {currentWeight.toFixed(2)} / {maxWeight.toFixed(2)}
            </Text>
          </Card>
        </Col>
      </Row>

      <Card title={t('procurement.extra_expenses')} loading={loading} className="procurement-glass-card">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Form form={expenseForm} layout="inline">
            <Form.Item
              name="category"
              rules={[{ required: true, message: t('procurement.expense_category_required') }]}
              style={{ minWidth: 180 }}
            >
              <Input placeholder={t('procurement.expense_category_placeholder')} />
            </Form.Item>
            <Form.Item
              name="cost"
              rules={[{ required: true, message: t('procurement.expense_cost_required') }]}
              style={{ minWidth: 140 }}
            >
              <InputNumber min={0.01} precision={2} placeholder={t('procurement.expense_cost_placeholder')} />
            </Form.Item>
            <Button type="primary" onClick={submitExpense}>
              {t('procurement.add_expense')}
            </Button>
          </Form>
          <Text strong>
            {t('procurement.total_expenses')}: {(hudStats?.totalTripExpenses ?? 0).toFixed(2)}
          </Text>
          {expenses.length === 0 ? (
            <Empty description={t('procurement.no_expenses')} />
          ) : (
            <Table<TripExpense>
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={expenses}
              columns={[
                { title: t('procurement.expense_category'), dataIndex: 'category', key: 'category' },
                { title: t('procurement.expense_cost'), dataIndex: 'cost', key: 'cost' },
                { title: t('procurement.expense_created_at'), dataIndex: 'createdAt', key: 'createdAt' }
              ]}
            />
          )}
        </Space>
      </Card>

      <Card title={t('procurement.customer_ledger')} loading={loading} className="procurement-glass-card">
        {ledgerEntries.length === 0 ? (
          <Empty description={t('procurement.no_ledger_data')} />
        ) : (
          <Table<CustomerBalanceLedgerEntry>
            rowKey="businessId"
            size="small"
            pagination={false}
            dataSource={ledgerEntries}
            columns={[
              { title: t('procurement.business_id'), dataIndex: 'businessId', key: 'businessId' },
              { title: t('orders.header.customer_id'), dataIndex: 'customerId', key: 'customerId' },
              { title: t('procurement.total_receivable'), dataIndex: 'totalReceivable', key: 'totalReceivable' },
              { title: t('procurement.paid_deposit'), dataIndex: 'paidDeposit', key: 'paidDeposit' },
              { title: t('procurement.outstanding_balance'), dataIndex: 'outstandingBalance', key: 'outstandingBalance' },
              {
                title: t('procurement.customer_statement'),
                key: 'statement',
                render: (_, row) => (
                  <Button size="small" onClick={() => exportCustomerStatement(row.businessId)}>
                    {t('procurement.export_pdf')}
                  </Button>
                )
              }
            ]}
          />
        )}
      </Card>

      <Card title={t('procurement.operation_history')} loading={loading} className="procurement-glass-card">
        {auditLogs.length === 0 ? (
          <Empty description={t('procurement.no_audit_logs')} />
        ) : (
          <Timeline
            items={auditLogs.map((log) => ({
              children: `${log.createdAt} · ${log.actionType} · ${log.operatorName} · ${log.originalValue} -> ${log.modifiedValue}`
            }))}
          />
        )}
      </Card>

      <Card title={t('procurement.reconcile_detail')} loading={loading} className="procurement-glass-card">
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
              { title: t('procurement.business_id'), dataIndex: 'businessId', key: 'businessId' },
              {
                title: t('procurement.reconcile_action'),
                key: 'action',
                render: (_, row) => (
                  <Button size="small" onClick={() => openReconcileModal(row)}>
                    {t('procurement.correct')}
                  </Button>
                )
              }
            ]}
          />
        )}
      </Card>

      <Modal
        open={isModalOpen}
        title={t('procurement.manual_reconcile')}
        okText={t('procurement.apply_fix')}
        cancelText={t('common.cancel')}
        onCancel={() => setIsModalOpen(false)}
        onOk={submitManualReconcile}
      >
        <Space direction="vertical" style={{ width: '100%' }}>
          <Text>{editingRow ? `${editingRow.itemName} (${editingRow.skuId})` : ''}</Text>
          <Select
            placeholder={t('procurement.target_order_placeholder')}
            options={targetBusinessOptions}
            value={targetBusinessId}
            onChange={setTargetBusinessId}
          />
          <InputNumber
            min={1}
            max={editingRow?.purchasedQuantity ?? 1}
            value={transferQuantity}
            onChange={(value) => setTransferQuantity(value ?? 1)}
          />
        </Space>
      </Modal>
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

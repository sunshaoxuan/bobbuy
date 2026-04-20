import { useCallback, useEffect, useMemo, useRef, useState, type ChangeEvent } from 'react';
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
  type LogisticsTracking,
  type Order,
  type ProfitSharingConfig,
  type ProcurementDeficitItemResponse,
  type ProcurementHudStats,
  type Trip,
  type TripExpense,
  type WalletSummary,
  type WalletTransaction
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
  const [expenseReceiptBase64, setExpenseReceiptBase64] = useState<string>();
  const [auditLogs, setAuditLogs] = useState<FinancialAuditLog[]>([]);
  const [ledgerEntries, setLedgerEntries] = useState<CustomerBalanceLedgerEntry[]>([]);
  const [profitSharing, setProfitSharing] = useState<ProfitSharingConfig>();
  const [purchaserRatio, setPurchaserRatio] = useState<number>(70);
  const [promoterRatio, setPromoterRatio] = useState<number>(30);
  const [logisticsTrackings, setLogisticsTrackings] = useState<LogisticsTracking[]>([]);
  const [logisticsTrackingNumber, setLogisticsTrackingNumber] = useState<string>('');
  const [logisticsChannel, setLogisticsChannel] = useState<string>('DOMESTIC');
  const [logisticsProvider, setLogisticsProvider] = useState<string>('MOCK');
  const [purchaserWallet, setPurchaserWallet] = useState<WalletSummary>();
  const [promoterWallet, setPromoterWallet] = useState<WalletSummary>();
  const [walletTransactions, setWalletTransactions] = useState<WalletTransaction[]>([]);
  const [reconcileRows, setReconcileRows] = useState<ReconcileDetailRow[]>([]);
  const [deficitItems, setDeficitItems] = useState<ProcurementDeficitItemResponse[]>([]);
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
      const [hud, orderList, expenseList, tripAuditLogs, customerLedger, profitShareConfig, logistics, deficits] = await Promise.all([
        api.procurementHud(tripId),
        api.orders(tripId),
        api.procurementExpenses(tripId),
        api.procurementAuditLogs(tripId),
        api.customerBalanceLedger(tripId),
        api.procurementProfitSharing(tripId),
        api.procurementLogistics(tripId),
        api.procurementDeficitItems(tripId)
      ]);
      if (refreshRequestRef.current !== requestId) {
        return;
      }
      setHudStats(hud);
      setOrders(orderList);
      setExpenses(expenseList);
      setAuditLogs(tripAuditLogs);
      setLedgerEntries(customerLedger);
      setProfitSharing(profitShareConfig);
      setPurchaserRatio(profitShareConfig.purchaserRatioPercent);
      setPromoterRatio(profitShareConfig.promoterRatioPercent);
      setLogisticsTrackings(logistics);
      setDeficitItems(deficits);
      setReconcileRows(buildReconcileRows(orderList));
      const [wPurchaser, wPromoter, txList] = await Promise.all([
        api.getWallet('PURCHASER'),
        api.getWallet('PROMOTER'),
        api.getWalletTransactions('PURCHASER')
      ]);
      setPurchaserWallet(wPurchaser);
      setPromoterWallet(wPromoter);
      setWalletTransactions(txList);
    } catch {
      setHudStats(undefined);
      setOrders([]);
      setExpenses([]);
      setAuditLogs([]);
      setLedgerEntries([]);
      setProfitSharing(undefined);
      setLogisticsTrackings([]);
      setDeficitItems([]);
      setReconcileRows([]);
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
    await api.createProcurementExpense(selectedTripId, {
      category: values.category,
      cost: values.cost,
      receiptImageBase64: expenseReceiptBase64
    });
    expenseForm.resetFields();
    setExpenseReceiptBase64(undefined);
    await refreshTripData(selectedTripId);
  };

  const updateProfitSharing = async () => {
    if (!selectedTripId) {
      return;
    }
    const config = await api.updateProcurementProfitSharing(selectedTripId, {
      purchaserRatioPercent: purchaserRatio,
      promoterRatioPercent: promoterRatio
    });
    setProfitSharing(config);
    message.success(t('procurement.profit_share_updated'));
    await refreshTripData(selectedTripId);
  };

  const createLogisticsTracking = async () => {
    if (!selectedTripId || !logisticsTrackingNumber.trim()) {
      return;
    }
    await api.createProcurementLogistics(selectedTripId, {
      trackingNumber: logisticsTrackingNumber.trim(),
      channel: logisticsChannel,
      provider: logisticsProvider
    });
    setLogisticsTrackingNumber('');
    await refreshTripData(selectedTripId);
  };

  const refreshLogisticsTracking = async (trackingId: number) => {
    if (!selectedTripId) {
      return;
    }
    await api.refreshProcurementLogistics(selectedTripId, trackingId);
    await refreshTripData(selectedTripId);
  };

  const finalizeSettlement = async () => {
    if (!selectedTripId) {
      return;
    }
    Modal.confirm({
      title: t('procurement.finalize_confirm_title'),
      content: t('procurement.finalize_confirm_content'),
      onOk: async () => {
        try {
          await api.finalizeProcurementSettlement(selectedTripId);
          message.success(t('procurement.finalize_success'));
          await refreshTripData(selectedTripId);
        } catch {
          message.error(t('errors.request_failed'));
        }
      }
    });
  };

  const previewReceipt = async (expense: TripExpense) => {
    if (!selectedTripId || !expense.id) {
      return;
    }
    const preview = await api.expenseReceiptPreview(selectedTripId, expense.id);
    if (preview.previewUrl) {
      window.open(preview.previewUrl, '_blank', 'noopener,noreferrer');
    }
  };

  const onSelectExpenseReceipt = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      setExpenseReceiptBase64(undefined);
      return;
    }
    const base64 = await toBase64(file);
    setExpenseReceiptBase64(base64);
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

  const publishToMall = async (skuId: string) => {
    try {
      await api.patchProduct(skuId, { visibilityStatus: 'PUBLIC' as any });
      message.success(t('procurement.publish_success') || 'Published to Mall');
      if (selectedTripId) {
        await refreshTripData(selectedTripId);
      }
    } catch {
      message.error(t('errors.request_failed'));
    }
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
            {selectedTrip?.status !== 'SETTLED' ? (
              <Button type="primary" danger onClick={finalizeSettlement}>{t('procurement.finalize_settlement')}</Button>
            ) : (
              <Tag color="gold" style={{ padding: '4px 12px', fontSize: '14px' }}>{t('procurement.status_settled')}</Tag>
            )}
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

      <Card title={t('procurement.profit_share')} loading={loading} className="procurement-glass-card">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Space wrap>
            <InputNumber
              min={0}
              max={100}
              precision={2}
              value={purchaserRatio}
              onChange={(value) => setPurchaserRatio(value ?? 0)}
              placeholder={t('procurement.purchaser_ratio')}
            />
            <InputNumber
              min={0}
              max={100}
              precision={2}
              value={promoterRatio}
              onChange={(value) => setPromoterRatio(value ?? 0)}
              placeholder={t('procurement.promoter_ratio')}
            />
            <Button type="primary" onClick={updateProfitSharing}>{t('procurement.update_ratio')}</Button>
          </Space>
          <Table
            rowKey="partnerRole"
            size="small"
            pagination={false}
            dataSource={profitSharing?.shares ?? hudStats?.partnerShares ?? []}
            columns={[
              { title: t('procurement.partner_role'), dataIndex: 'partnerRole', key: 'partnerRole' },
              { title: t('procurement.ratio_percent'), dataIndex: 'ratioPercent', key: 'ratioPercent' },
              { title: t('procurement.share_amount'), dataIndex: 'amount', key: 'amount' }
            ]}
          />
        </Space>
      </Card>

      <Card title={t('procurement.wallet_center')} loading={loading} className="procurement-glass-card">
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12}>
            <Card size="small" title={t('procurement.purchaser_wallet')}>
              <Statistic 
                title={t('procurement.current_balance')} 
                value={purchaserWallet?.balance ?? 0} 
                suffix="CNY" 
                precision={2} 
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                {t('procurement.last_updated')}: {purchaserWallet?.updatedAt}
              </Text>
            </Card>
          </Col>
          <Col xs={24} sm={12}>
            <Card size="small" title={t('procurement.promoter_wallet')}>
              <Statistic 
                title={t('procurement.current_balance')} 
                value={promoterWallet?.balance ?? 0} 
                suffix="CNY" 
                precision={2} 
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                {t('procurement.last_updated')}: {promoterWallet?.updatedAt}
              </Text>
            </Card>
          </Col>
        </Row>
        <div style={{ marginTop: '16px' }}>
          <Text strong>{t('procurement.transaction_history')}</Text>
          <Table<WalletTransaction>
            dataSource={walletTransactions}
            rowKey="id"
            size="small"
            pagination={{ pageSize: 5 }}
            columns={[
              { title: t('common.date'), dataIndex: 'createdAt', key: 'createdAt' },
              { title: t('procurement.trip_id'), dataIndex: 'tripId', key: 'tripId' },
              { title: t('procurement.amount'), dataIndex: 'amount', key: 'amount' },
              { title: t('procurement.type'), dataIndex: 'type', key: 'type' }
            ]}
          />
        </div>
      </Card>

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
          <Text>{t('procurement.upload_receipt')}</Text>
          <Input
            type="file"
            accept="image/*"
            aria-label={t('procurement.upload_receipt')}
            onChange={onSelectExpenseReceipt}
          />
          {expenseReceiptBase64 ? <Text type="secondary">{t('procurement.receipt_selected')}</Text> : null}
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
                { title: t('procurement.expense_created_at'), dataIndex: 'createdAt', key: 'createdAt' },
                {
                  title: t('procurement.receipt'),
                  key: 'receipt',
                  render: (_, row) => row.receiptThumbnailUrl ? (
                    <Button type="link" onClick={() => previewReceipt(row)}>{t('procurement.preview_receipt')}</Button>
                  ) : '-'
                },
                { title: t('procurement.ocr_status'), dataIndex: 'ocrStatus', key: 'ocrStatus' }
              ]}
            />
          )}
        </Space>
      </Card>

      <Card title={t('procurement.logistics_tracking')} loading={loading} className="procurement-glass-card">
        <Space direction="vertical" style={{ width: '100%' }}>
          <Space wrap>
            <Input
              value={logisticsTrackingNumber}
              onChange={(event) => setLogisticsTrackingNumber(event.target.value)}
              placeholder={t('procurement.logistics_number_placeholder')}
            />
            <Select
              value={logisticsChannel}
              options={[
                { label: t('procurement.channel_domestic'), value: 'DOMESTIC' },
                { label: t('procurement.channel_international'), value: 'INTERNATIONAL' }
              ]}
              onChange={setLogisticsChannel}
              style={{ minWidth: 140 }}
            />
            <Select
              value={logisticsProvider}
              options={[
                { label: 'MOCK', value: 'MOCK' },
                { label: '17TRACK', value: 'TRACK17' }
              ]}
              onChange={setLogisticsProvider}
              style={{ minWidth: 120 }}
            />
            <Button type="primary" onClick={createLogisticsTracking}>{t('procurement.add_logistics')}</Button>
          </Space>
          <Table<LogisticsTracking>
            rowKey="id"
            size="small"
            pagination={false}
            dataSource={logisticsTrackings}
            columns={[
              { title: t('procurement.logistics_number'), dataIndex: 'trackingNumber', key: 'trackingNumber' },
              { title: t('procurement.logistics_channel'), dataIndex: 'channel', key: 'channel' },
              { title: t('procurement.logistics_provider'), dataIndex: 'provider', key: 'provider' },
              { title: t('procurement.logistics_status'), dataIndex: 'status', key: 'status' },
              { title: t('procurement.logistics_message'), dataIndex: 'lastMessage', key: 'lastMessage' },
              {
                title: t('procurement.settlement_reminder'),
                key: 'settlementReminderTriggered',
                render: (_, row) => row.settlementReminderTriggered ? <Tag color="success">{t('procurement.triggered')}</Tag> : '-'
              },
              {
                title: t('procurement.reconcile_action'),
                key: 'action',
                render: (_, row) => (
                  <Button size="small" onClick={() => refreshLogisticsTracking(row.id)}>
                    {t('procurement.refresh_logistics')}
                  </Button>
                )
              }
            ]}
          />
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

      <Card title={t('procurement.procurement_deficit') || 'Procurement Deficit'} loading={loading} className="procurement-glass-card">
        <Table<ProcurementDeficitItemResponse>
          rowKey="skuId"
          size="small"
          pagination={false}
          dataSource={deficitItems}
          columns={[
            { title: t('orders.lines.sku_id'), dataIndex: 'skuId', key: 'skuId' },
            { title: t('orders.lines.item_name'), dataIndex: 'itemName', key: 'itemName' },
            { title: t('procurement.deficit_quantity') || 'Deficit Qty', dataIndex: 'deficitQuantity', key: 'deficitQuantity' },
             {
               title: t('procurement.priority') || 'Priority',
               dataIndex: 'priority',
               key: 'priority',
               render: (value: string) => <Tag color={value === 'CRITICAL' ? 'red' : 'orange'}>{value}</Tag>
             },
             { 
               title: t('common.actions') || 'Actions', 
               key: 'actions', 
               render: (_, row) => (
                 <Space>
                   {row.isTemporary && row.visibilityStatus === 'DRAFTER_ONLY' && (
                     <Button size="small" type="primary" ghost onClick={() => void publishToMall(row.skuId)}>
                       {t('procurement.publish_to_mall') || 'Publish to Mall'}
                     </Button>
                   )}
                </Space>
              )
            }
          ]}
        />
      </Card>

      <Card title={t('procurement.operation_history')} loading={loading} className="procurement-glass-card">
        {auditLogs.length === 0 ? (
          <Empty description={t('procurement.no_audit_logs')} />
        ) : (
          <Timeline
            items={auditLogs.map((log) => ({
              children: `${log.createdAt} · ${log.actionType} · ${log.operatorName} · ${formatAuditPayload(log.originalValue)} -> ${formatAuditPayload(log.modifiedValue)}`
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

function formatAuditPayload(raw: string): string {
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>;
    return Object.entries(parsed)
      .map(([key, value]) => `${key}:${String(value)}`)
      .join(', ');
  } catch {
    return raw;
  }
}

function toBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ''));
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.readAsDataURL(file);
  });
}

import { Card, Empty, Grid, Select, Space, Spin, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { api, type CustomerBalanceLedgerEntry, type Trip } from '../api';
import { useI18n } from '../i18n';

const { Title, Text } = Typography;

export default function ClientBilling() {
  const { t } = useI18n();
  const screens = Grid.useBreakpoint();
  const isMobile = screens.md === false;
  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<number>();
  const [entries, setEntries] = useState<CustomerBalanceLedgerEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const formatAmount = (amount: number) =>
    new Intl.NumberFormat(undefined, { style: 'currency', currency: 'JPY' }).format(amount);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const tripList = await api.trips();
        if (cancelled) return;
        setTrips(tripList);
        const firstTripId = tripList[0]?.id;
        if (firstTripId) {
          setSelectedTripId(firstTripId);
          setEntries(await api.customerBalanceLedger(firstTripId));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const onTripChange = async (tripId: number) => {
    setSelectedTripId(tripId);
    setLoading(true);
    try {
      setEntries(await api.customerBalanceLedger(tripId));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-card client-page">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space direction={isMobile ? 'vertical' : 'horizontal'} style={{ width: '100%', justifyContent: 'space-between' }}>
          <Title level={4} style={{ margin: 0 }}>
            {t('nav.client_billing')}
          </Title>
          <Select
            value={selectedTripId}
            placeholder={t('orders.trip.select.placeholder')}
            onChange={onTripChange}
            style={{ minWidth: isMobile ? '100%' : 280 }}
            options={trips.map((trip) => ({
              value: trip.id,
              label: `${trip.origin} → ${trip.destination}`
            }))}
          />
        </Space>
        {loading ? (
          <Spin />
        ) : entries.length === 0 ? (
          <Empty description={t('procurement.no_ledger_data')} />
        ) : (
          <div className="client-card-list">
            {entries.map((entry) => (
              <Card key={entry.businessId} className="client-list-card">
                <Space direction="vertical" size={6} style={{ width: '100%' }}>
                  <Text strong>{entry.businessId}</Text>
                  <Text>
                    {t('zen.statement_total_receivable')}: {formatAmount(entry.totalReceivable)}
                  </Text>
                  <Text>
                    {t('zen.statement_paid_deposit')}: {formatAmount(entry.paidDeposit)}
                  </Text>
                  <Text strong>
                    {t('zen.statement_outstanding')}: {formatAmount(entry.outstandingBalance)}
                  </Text>
                </Space>
              </Card>
            ))}
          </div>
        )}
      </Space>
    </div>
  );
}

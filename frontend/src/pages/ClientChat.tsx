import { Grid, Select, Space, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { api, type Trip } from '../api';
import ChatWidget from '../components/ChatWidget';
import { useI18n } from '../i18n';

const { Title } = Typography;

export default function ClientChat() {
  const { t } = useI18n();
  const screens = Grid.useBreakpoint();
  const isMobile = screens.md === false;
  const [trips, setTrips] = useState<Trip[]>([]);
  const [selectedTripId, setSelectedTripId] = useState<number>();
  const senderId =
    typeof window !== 'undefined' ? window.localStorage.getItem('bobbuy_chat_sender_id') ?? 'DEMO-CUST' : 'DEMO-CUST';

  useEffect(() => {
    let cancelled = false;
    api.trips().then((tripList) => {
      if (cancelled) {
        return;
      }
      setTrips(tripList);
      setSelectedTripId(tripList[0]?.id);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="page-card client-page">
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Space direction={isMobile ? 'vertical' : 'horizontal'} style={{ width: '100%', justifyContent: 'space-between' }}>
          <Title level={4} style={{ margin: 0 }}>
            {t('nav.client_chat')}
          </Title>
          <Select
            value={selectedTripId}
            placeholder={t('orders.trip.select.placeholder')}
            onChange={(value) => setSelectedTripId(value)}
            style={{ minWidth: isMobile ? '100%' : 280 }}
            options={trips.map((trip) => ({
              value: trip.id,
              label: `${trip.origin} → ${trip.destination}`
            }))}
          />
        </Space>
        <ChatWidget tripId={selectedTripId} senderId={senderId} recipientId="PURCHASER" />
      </Space>
    </div>
  );
}

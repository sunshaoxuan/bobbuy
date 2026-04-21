import React, { useState } from 'react';
import { Card, Breadcrumb, Typography, List, Avatar, Input, Button, Badge, Tag, Drawer, Space, Divider, message, Grid } from 'antd';
import { RobotOutlined, UserOutlined, SendOutlined, CheckCircleOutlined, ShoppingCartOutlined, PlusOutlined } from '@ant-design/icons';
import { useI18n } from '../i18n';
import { api } from '../api';

const { Title, Text } = Typography;

interface ChatMessage {
  id: number;
  sender: 'merchant' | 'ai' | 'customer';
  text: string;
  time: string;
}

interface ExtractedItem {
  id: string;
  originalName: string;
  name: string;
  quantity: number;
  note?: string;
  price: number;
  confidence: number;
  status: 'detected' | 'added';
  imageUrl?: string;
}

export default function OrderDesk() {
  const { t } = useI18n();
  const screens = Grid.useBreakpoint();
  const isMobile = screens.md !== true;
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [extractedItems, setExtractedItems] = useState<ExtractedItem[]>([]);
  const [isParsing, setIsParsing] = useState(false);

  const handleSend = async () => {
    const text = inputValue.trim();
    if (!text || isParsing) return;

    setIsParsing(true);
    const newMessage: ChatMessage = {
      id: Date.now(),
      sender: 'merchant',
      text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };
    setMessages(prev => [...prev, newMessage]);
    setInputValue('');

    try {
      const parsed = await api.parseOrderText(text);
      const aiSummary = parsed.items.length
        ? `${t('order_desk.detected_prefix')}: ${parsed.items.map((item) => `${item.matchedName} (x${item.quantity})`).join(', ')}`
        : t('order_desk.no_item_detected');
      setMessages(prev => [...prev, {
        id: Date.now() + 1,
        sender: 'ai',
        text: aiSummary,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }]);
      setExtractedItems(parsed.items.map((item) => ({
        id: item.id,
        originalName: item.originalName,
        name: item.matchedName,
        quantity: item.quantity,
        note: item.note,
        price: item.price,
        confidence: item.confidence,
        status: 'detected' as const
      })));
    } catch {
      setMessages(prev => [...prev, {
        id: Date.now() + 1,
        sender: 'ai',
        text: t('order_desk.parse_failed'),
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      }]);
    } finally {
      setIsParsing(false);
    }
  };

  const handleAddToOrder = async (item: ExtractedItem) => {
    setExtractedItems(prev => prev.map(current => 
      current.id === item.id ? { ...current, status: 'added' } : current
    ));
    try {
      await api.confirmAiMapping(item.originalName, item.name);
    } catch {
      message.error(t('errors.request_failed'));
      setExtractedItems(prev => prev.map(current =>
        current.id === item.id ? { ...current, status: 'detected' } : current
      ));
      return;
    }
    message.success(t('order_desk.item_added'));
  };

  return (
    <div style={{ minHeight: 'calc(100vh - 150px)', display: 'flex', flexDirection: 'column' }} data-testid="orderdesk-root">
      <Breadcrumb style={{ marginBottom: 16 }} items={[{ title: t('nav.dashboard') }, { title: t('nav.order_desk') }]} />
      
      <div style={{ display: 'flex', flex: 1, flexDirection: isMobile ? 'column' : 'row', gap: 24, overflow: 'hidden' }}>
        {/* Chat Area */}
        <Card title={
          <Space>
            <Avatar style={{ backgroundColor: '#1890ff' }} icon={<UserOutlined />} />
            <span>{t('order_desk.customer_label')}: Alice Wang</span>
            <Badge status="processing" text={t('common.online')} />
          </Space>
        } style={{ flex: 1, display: 'flex', flexDirection: 'column', height: isMobile ? 420 : '100%' }} styles={{ body: { flex: 1, overflowY: 'auto', padding: '16px 24px' } }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {messages.map((msg) => (
              <div key={msg.id} style={{ alignSelf: msg.sender === 'merchant' ? 'flex-end' : 'flex-start', maxWidth: '70%', textAlign: msg.sender === 'merchant' ? 'right' : 'left' }}>
                <Space align="start" direction="horizontal" style={{ flexDirection: msg.sender === 'merchant' ? 'row-reverse' : 'row' }}>
                  <Avatar size="small" icon={msg.sender === 'ai' ? <RobotOutlined /> : <UserOutlined />} />
                  <div style={{ 
                    background: msg.sender === 'merchant' ? '#1890ff' : msg.sender === 'ai' ? '#f0f5ff' : '#f5f5f5',
                    color: msg.sender === 'merchant' ? 'white' : '#000',
                    padding: '8px 16px',
                    borderRadius: 12,
                    boxShadow: '0 2px 4px rgba(0,0,0,0.05)'
                  }}>
                    <div style={{ fontWeight: 600, fontSize: '0.8em', marginBottom: 4, display: 'flex', justifyContent: 'space-between', color: msg.sender === 'merchant' ? '#e6f7ff' : '#8c8c8c' }}>
                      <span>{t(`order_desk.sender.${msg.sender}`)}</span>
                      <span style={{ marginLeft: 8 }}>{msg.time}</span>
                    </div>
                    <div>{msg.text}</div>
                    {msg.sender === 'ai' && (
                      <Tag color="processing" style={{ marginTop: 8, border: 'none' }}>{t('order_desk.matched_item')}</Tag>
                    )}
                  </div>
                </Space>
              </div>
            ))}
          </div>
          
          <div style={{ position: 'absolute', bottom: 16, left: 16, right: 16, display: 'flex', gap: 8 }}>
            <Input 
              data-testid="orderdesk-input"
              placeholder={t('order_desk.input_placeholder')}
              value={inputValue} 
              onChange={e => setInputValue(e.target.value)}
              onPressEnter={handleSend}
              disabled={isParsing}
            />
            <Button type="primary" icon={<SendOutlined />} onClick={handleSend} loading={isParsing} data-testid="orderdesk-send" />
          </div>
        </Card>

        {/* AI Extraction Panel */}
        <Card 
          title={<Space><RobotOutlined /> <Title level={5} style={{ margin: 0 }}>{t('order_desk.extraction_title')}</Title></Space>}
          style={{ width: isMobile ? '100%' : 320, height: '100%' }}
          styles={{ body: { padding: 16 } }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <Text type="secondary">{extractedItems.length} {t('order_desk.items_detected')}</Text>
            <Button size="small" type="link">{t('order_desk.clear_all')}</Button>
          </div>
          
          <List
            dataSource={extractedItems}
            renderItem={(item) => (
              <Card size="small" style={{ marginBottom: 12, border: item.status === 'added' ? '1px solid #52c41a' : '1px solid #f0f0f0' }}>
                <div style={{ display: 'flex', gap: 12, marginBottom: 8 }}>
                  <img src={(item as any).imageUrl} alt={item.name} style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'cover', background: '#f5f5f5' }} />
                  <div style={{ flex: 1 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <Text strong style={{ fontSize: '0.9em' }}>{item.name}</Text>
                      {item.status === 'added' && <CheckCircleOutlined style={{ color: '#52c41a' }} />}
                    </div>
                     <Text type="secondary" style={{ fontSize: '0.8em' }}>{t('order_desk.qty_label')}: {item.quantity} | ${item.price}</Text>
                     {item.note ? <div><Text type="secondary" style={{ fontSize: '0.75em' }}>{t('order_desk.note_label')}: {item.note}</Text></div> : null}
                  </div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                   <Text type="secondary" style={{ fontSize: '0.75em' }}>{t('order_desk.confidence_label')}: {(item.confidence * 100).toFixed(0)}%</Text>
                  <Button 
                    size="small" 
                    type={item.status === 'added' ? 'text' : 'primary'} 
                     disabled={item.status === 'added'}
                     icon={item.status === 'added' ? <CheckCircleOutlined /> : <PlusOutlined />}
                     onClick={() => void handleAddToOrder(item)}
                   >
                     {item.status === 'added' ? t('order_desk.added') : t('order_desk.add')}
                   </Button>
                </div>
              </Card>
            )}
          />
          
          <Divider />
          
          <Button type="primary" block icon={<ShoppingCartOutlined />} size="large">
            {t('order_desk.confirm_full_order')}
          </Button>
        </Card>
      </div>
    </div>
  );
}

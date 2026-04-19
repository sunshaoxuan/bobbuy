import React, { useState, useEffect, useRef } from 'react';
import { Button, Input, List, Modal, Avatar, Space, Typography, Badge, message } from 'antd';
import { MessageOutlined, SendOutlined, UserOutlined, PictureOutlined } from '@ant-design/icons';
import { api, ChatMessage } from '../api';
import { useI18n } from '../i18n';

const { Text } = Typography;

interface ChatWidgetProps {
  orderId?: number;
  senderId: string;
  recipientId: string;
}

export default function ChatWidget({ orderId, senderId, recipientId }: ChatWidgetProps) {
  const { t } = useI18n();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;

    const fetchMessages = async () => {
      let data: ChatMessage[];
      if (orderId) {
        data = await api.getOrderChat(orderId);
      } else {
        data = await api.getPrivateChat(senderId, recipientId);
      }
      setMessages(data);
    };

    fetchMessages();
    const timer = setInterval(fetchMessages, 3000);
    return () => clearInterval(timer);
  }, [open, orderId, senderId, recipientId]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    if (!inputValue.trim()) return;
    setLoading(true);
    try {
      await api.sendChatMessage({
        orderId,
        senderId,
        recipientId,
        content: inputValue.trim(),
        type: 'TEXT'
      });
      setInputValue('');
    } catch {
      message.error(t('errors.request_failed'));
    } finally {
      setLoading(false);
    }
  };

  const handleSendImage = () => {
    // Simulated image upload
    api.sendChatMessage({
        orderId,
        senderId,
        recipientId,
        content: '[Image Simulation]',
        type: 'IMAGE',
        metadata: { url: 'https://via.placeholder.com/300' }
    });
    message.info('Image sent (mock)');
  };

  return (
    <>
      <div 
        style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 1000 }}
        onClick={() => setOpen(true)}
      >
        <Badge count={0}>
          <Button 
            type="primary" 
            shape="circle" 
            icon={<MessageOutlined />} 
            size="large"
            style={{ 
              width: 56, height: 56, 
              boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
              background: 'var(--zen-brand)'
            }} 
          />
        </Badge>
      </div>

      <Modal
        title={
          <Space>
            <Avatar size="small" icon={<UserOutlined />} />
            <Text strong>{t('chat.title') || 'Procurement Chat'}</Text>
          </Space>
        }
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={400}
        bodyStyle={{ padding: 0 }}
        className="zen-chat-modal"
      >
        <div style={{ height: 400, display: 'flex', flexDirection: 'column' }}>
          <div 
            ref={scrollRef}
            style={{ flex: 1, overflowY: 'auto', padding: '16px', background: '#fafafa' }}
          >
            <List
              dataSource={messages}
              split={false}
              renderItem={(msg) => (
                <div style={{ 
                  display: 'flex', 
                  justifyContent: msg.senderId === senderId ? 'flex-end' : 'flex-start',
                  marginBottom: 12
                }}>
                  <div style={{ 
                    maxWidth: '80%', 
                    padding: '8px 12px', 
                    borderRadius: '12px',
                    background: msg.senderId === senderId ? 'var(--zen-brand)' : 'white',
                    color: msg.senderId === senderId ? 'white' : 'black',
                    boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                    border: msg.senderId === senderId ? 'none' : 'var(--zen-line)'
                  }}>
                    {msg.type === 'IMAGE' ? (
                        <div style={{ borderRadius: 8, overflow: 'hidden' }}>
                            <img src={msg.metadata?.url} alt="chat" style={{ width: '100%', display: 'block' }} />
                        </div>
                    ) : (
                        <Text style={{ color: 'inherit' }}>{msg.content}</Text>
                    )}
                    <div style={{ fontSize: '10px', opacity: 0.6, textAlign: 'right', marginTop: 4 }}>
                      {msg.createdAt?.split('T')[1].substring(0, 5)}
                    </div>
                  </div>
                </div>
              )}
            />
          </div>
          <div style={{ padding: '12px', borderTop: 'var(--zen-line)', background: 'white' }}>
            <Space.Compact style={{ width: '100%' }}>
              <Button icon={<PictureOutlined />} onClick={handleSendImage} />
              <Input 
                placeholder={t('chat.placeholder') || 'Type a message...'} 
                value={inputValue}
                onChange={e => setInputValue(e.target.value)}
                onPressEnter={handleSend}
              />
              <Button type="primary" icon={<SendOutlined />} onClick={handleSend} loading={loading} />
            </Space.Compact>
          </div>
        </div>
      </Modal>
    </>
  );
}

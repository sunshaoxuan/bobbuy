import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Checkbox,
  Divider,
  Input,
  List,
  Modal,
  Radio,
  Space,
  Tag,
  Typography,
  message
} from 'antd';
import {
  CameraOutlined,
  MessageOutlined,
  ReloadOutlined,
  SendOutlined,
  ShopOutlined,
  UserOutlined
} from '@ant-design/icons';
import { api, type AiOnboardingSuggestion, type ChatMessage } from '../api';
import { useI18n } from '../i18n';

const { Text, Paragraph } = Typography;

interface ChatWidgetProps {
  orderId?: number;
  tripId?: number;
  senderId: string;
  recipientId: string;
}

type ConversationType = 'PRIVATE' | 'ORDER' | 'TRIP';

const messageKey = (chatMessage: ChatMessage) =>
  String(
    chatMessage.id ??
      `${chatMessage.tripId ?? ''}-${chatMessage.orderId ?? ''}-${chatMessage.createdAt ?? ''}-${chatMessage.senderId}-${chatMessage.recipientId}-${chatMessage.type}-${String(chatMessage.metadata?.url ?? '').slice(-32)}`
  );

export default function ChatWidget({ orderId, tripId, senderId, recipientId }: ChatWidgetProps) {
  const { t } = useI18n();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [loadError, setLoadError] = useState<string>();
  const [unreadCount, setUnreadCount] = useState(0);
  const [pendingSuggestion, setPendingSuggestion] = useState<AiOnboardingSuggestion | null>(null);
  const [pendingImagePreview, setPendingImagePreview] = useState<string>();
  const [confirmingImage, setConfirmingImage] = useState(false);
  const [publishOnConfirm, setPublishOnConfirm] = useState(false);
  const [selectedCandidateId, setSelectedCandidateId] = useState<string>();
  const scrollRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const knownMessageKeysRef = useRef<Set<string>>(new Set());

  const conversationType: ConversationType = tripId ? 'TRIP' : orderId ? 'ORDER' : 'PRIVATE';
  const conversationTypeLabel = useMemo(() => {
    if (conversationType === 'TRIP') {
      return t('chat.scope.trip');
    }
    if (conversationType === 'ORDER') {
      return t('chat.scope.order');
    }
    return t('chat.scope.private');
  }, [conversationType, t]);

  const conversationHint = useMemo(() => {
    if (conversationType === 'TRIP') {
      return t('chat.scope.trip_hint');
    }
    if (conversationType === 'ORDER') {
      return t('chat.scope.order_hint');
    }
    return t('chat.scope.private_hint');
  }, [conversationType, t]);

  const fetchConversation = useCallback(async () => {
    if (tripId) {
      return api.getTripChat(tripId);
    }
    if (orderId) {
      return api.getOrderChat(orderId);
    }
    return api.getPrivateChat(senderId, recipientId);
  }, [orderId, recipientId, senderId, tripId]);

  const loadMessages = useCallback(
    async ({ silent = false, showFeedback = false }: { silent?: boolean; showFeedback?: boolean } = {}) => {
      if (!silent) {
        setRefreshing(true);
      }
      try {
        const nextMessages = await fetchConversation();
        const nextKeys = nextMessages.map(messageKey);
        if (!open) {
          const unseen = nextKeys.filter((key) => !knownMessageKeysRef.current.has(key)).length;
          if (unseen > 0) {
            setUnreadCount((current) => current + unseen);
          }
        } else {
          setUnreadCount(0);
        }
        knownMessageKeysRef.current = new Set(nextKeys);
        setMessages(nextMessages);
        setLoadError(undefined);
        if (showFeedback) {
          message.success(t('chat.refresh_success'));
        }
      } catch {
        if (!silent || open) {
          setLoadError(t('chat.load_failed'));
        }
        if (showFeedback) {
          message.error(t('chat.load_failed'));
        }
      } finally {
        if (!silent) {
          setRefreshing(false);
        }
      }
    },
    [fetchConversation, open, t]
  );

  useEffect(() => {
    void loadMessages({ silent: !open });
    const timer = window.setInterval(() => {
      void loadMessages({ silent: true });
    }, 15000);
    return () => window.clearInterval(timer);
  }, [loadMessages, open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setUnreadCount(0);
    knownMessageKeysRef.current = new Set(messages.map(messageKey));
  }, [messages, open]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  const handleSend = async () => {
    if (!inputValue.trim()) {
      return;
    }
    setLoading(true);
    try {
      await api.sendChatMessage({
        orderId,
        tripId,
        senderId,
        recipientId,
        content: inputValue.trim(),
        type: 'TEXT',
        metadata: { conversationType }
      });
      setInputValue('');
      await loadMessages();
    } catch {
      message.error(t('errors.request_failed'));
    } finally {
      setLoading(false);
    }
  };

  const handleSelectImage = () => {
    fileInputRef.current?.click();
  };

  const handleImagePicked = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setConfirmingImage(true);
    try {
      const base64 = await readFileAsBase64(file);
      const suggestion = await api.onboardScan(base64);
      setPendingSuggestion(suggestion);
      setPendingImagePreview(base64);
      setPublishOnConfirm(false);
      setSelectedCandidateId(undefined);
    } catch {
      message.error(t('chat.image_scan_failed'));
    } finally {
      setConfirmingImage(false);
      event.target.value = '';
    }
  };

  const confirmImageFlow = async () => {
    if (!pendingSuggestion) {
      return;
    }
    setConfirmingImage(true);
    try {
      const isManualCandidate = !pendingSuggestion.existingProductFound && Boolean(selectedCandidateId);
      const payload: AiOnboardingSuggestion = {
        ...pendingSuggestion,
        existingProductFound: pendingSuggestion.existingProductFound || isManualCandidate,
        existingProductId: pendingSuggestion.existingProductFound ? pendingSuggestion.existingProductId : selectedCandidateId,
        visibilityStatus: pendingSuggestion.existingProductFound || isManualCandidate
          ? undefined
          : publishOnConfirm
            ? 'PUBLIC'
            : 'DRAFTER_ONLY'
      };
      const response = await api.onboardConfirm(payload);
      const previewUrl = response.product.mediaGallery?.[0]?.url ?? pendingImagePreview ?? pendingSuggestion.mediaGallery?.[0]?.url ?? '';
      const productName = response.displayName || pendingSuggestion.name;
      const draftOnly = response.product.visibilityStatus === 'DRAFTER_ONLY' || (!payload.existingProductFound && !publishOnConfirm);
      const visibilityStatus = draftOnly ? 'DRAFTER_ONLY' : response.product.visibilityStatus ?? 'PUBLIC';

      await api.sendChatMessage({
        orderId,
        tripId,
        senderId,
        recipientId,
        content: productName,
        type: 'IMAGE',
        metadata: {
          conversationType,
          url: previewUrl,
          productId: response.product.id,
          productName,
          itemNumber: response.product.itemNumber,
          visibilityStatus,
          isTemporary: response.product.isTemporary ?? true,
          existingProductFound: payload.existingProductFound ?? false,
          matchedBy: payload.existingProductFound
            ? pendingSuggestion.existingProductFound
              ? 'ITEM_NUMBER'
              : 'NAME_CANDIDATE'
            : 'NEW_TEMP_PRODUCT'
        }
      });

      setPendingSuggestion(null);
      setPendingImagePreview(undefined);
      setSelectedCandidateId(undefined);
      setPublishOnConfirm(false);
      await loadMessages();
      message.success(t('chat.image_sent_success'));
    } catch {
      message.error(t('errors.request_failed'));
    } finally {
      setConfirmingImage(false);
    }
  };

  const handlePublishInChat = async (target: ChatMessage) => {
    const productId = String(target.metadata?.productId ?? '');
    if (!productId) {
      return;
    }
    try {
      await api.patchProduct(productId, { visibilityStatus: 'PUBLIC' });
      setMessages((current) =>
        current.map((entry) =>
          messageKey(entry) === messageKey(target)
            ? { ...entry, metadata: { ...entry.metadata, visibilityStatus: 'PUBLIC' } }
            : entry
        )
      );
      message.success(t('chat.publish_success'));
    } catch {
      message.error(t('errors.request_failed'));
    }
  };

  return (
    <>
      <div style={{ position: 'fixed', bottom: 24, right: 24, zIndex: 1000 }} onClick={() => setOpen(true)}>
        <Badge count={unreadCount}>
          <Button
            type="primary"
            shape="circle"
            icon={<MessageOutlined />}
            size="large"
            style={{
              width: 56,
              height: 56,
              boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
              background: 'var(--zen-brand)'
            }}
          />
        </Badge>
      </div>

      <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleImagePicked} />

      <Modal
        title={
          <Space>
            <Avatar size="small" icon={<UserOutlined />} />
            <Text strong>{t('chat.title')}</Text>
            <Tag color="blue">{conversationTypeLabel}</Tag>
          </Space>
        }
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        width={420}
        styles={{ body: { padding: 0 } }}
        className="zen-chat-modal"
      >
        <div style={{ height: 460, display: 'flex', flexDirection: 'column' }}>
          <div style={{ padding: '12px 16px', borderBottom: 'var(--zen-line)', background: '#fff' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {conversationHint}
            </Text>
          </div>
          {loadError ? (
            <Alert
              type="warning"
              showIcon
              message={loadError}
              action={
                <Button size="small" onClick={() => void loadMessages({ showFeedback: true })}>
                  {t('chat.retry')}
                </Button>
              }
              style={{ margin: 12 }}
            />
          ) : null}
          <div ref={scrollRef} style={{ flex: 1, overflowY: 'auto', padding: '16px', background: '#fafafa' }}>
            <List
              locale={{ emptyText: refreshing ? t('chat.loading') : t('chat.empty') }}
              dataSource={messages}
              split={false}
              renderItem={(msg) => {
                const canPublish = msg.metadata?.isTemporary && msg.metadata?.visibilityStatus === 'DRAFTER_ONLY';
                return (
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: msg.senderId === senderId ? 'flex-end' : 'flex-start',
                      marginBottom: 12
                    }}
                  >
                    <div
                      style={{
                        maxWidth: '82%',
                        padding: '8px 12px',
                        borderRadius: '12px',
                        background: msg.senderId === senderId ? 'var(--zen-brand)' : 'white',
                        color: msg.senderId === senderId ? 'white' : 'black',
                        boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                        border: msg.senderId === senderId ? 'none' : 'var(--zen-line)'
                      }}
                    >
                      {msg.type === 'IMAGE' ? (
                        <Space direction="vertical" size={8} style={{ width: '100%' }}>
                          {msg.metadata?.url ? (
                            <div style={{ borderRadius: 8, overflow: 'hidden' }}>
                              <img src={msg.metadata.url} alt={msg.metadata?.productName ?? 'chat'} style={{ width: '100%', display: 'block' }} />
                            </div>
                          ) : null}
                          <div>
                            <Text strong style={{ color: 'inherit' }}>
                              {msg.metadata?.productName ?? msg.content}
                            </Text>
                            {msg.metadata?.itemNumber ? (
                              <div>
                                <Text style={{ color: 'inherit', opacity: 0.8 }}>SKU: {msg.metadata.itemNumber}</Text>
                              </div>
                            ) : null}
                            <Space wrap size={4} style={{ marginTop: 6 }}>
                              {msg.metadata?.matchedBy ? <Tag>{String(msg.metadata.matchedBy)}</Tag> : null}
                              {msg.metadata?.visibilityStatus ? <Tag color={canPublish ? 'orange' : 'green'}>{String(msg.metadata.visibilityStatus)}</Tag> : null}
                            </Space>
                          </div>
                          {canPublish ? (
                            <Button size="small" icon={<ShopOutlined />} onClick={() => void handlePublishInChat(msg)}>
                              {t('chat.publish_action')}
                            </Button>
                          ) : null}
                        </Space>
                      ) : (
                        <Text style={{ color: 'inherit' }}>{msg.content}</Text>
                      )}
                      <div style={{ fontSize: '10px', opacity: 0.6, textAlign: 'right', marginTop: 4 }}>
                        {msg.createdAt?.split('T')[1]?.substring(0, 5) ?? '--:--'}
                      </div>
                    </div>
                  </div>
                );
              }}
            />
          </div>
          <div style={{ padding: '12px', borderTop: 'var(--zen-line)', background: 'white' }}>
            <Space.Compact style={{ width: '100%' }}>
              <Button icon={<ReloadOutlined />} loading={refreshing} onClick={() => void loadMessages({ showFeedback: true })} />
              <Button icon={<CameraOutlined />} loading={confirmingImage} onClick={handleSelectImage} />
              <Input
                placeholder={t('chat.placeholder')}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onPressEnter={handleSend}
              />
              <Button type="primary" icon={<SendOutlined />} onClick={handleSend} loading={loading} />
            </Space.Compact>
          </div>
        </div>
      </Modal>

      <Modal
        open={Boolean(pendingSuggestion)}
        title={t('chat.image_confirm_title')}
        onCancel={() => {
          if (!confirmingImage) {
            setPendingSuggestion(null);
            setPendingImagePreview(undefined);
            setSelectedCandidateId(undefined);
            setPublishOnConfirm(false);
          }
        }}
        onOk={() => void confirmImageFlow()}
        okButtonProps={{ loading: confirmingImage }}
      >
        {pendingImagePreview ? (
          <div style={{ borderRadius: 12, overflow: 'hidden', marginBottom: 12 }}>
            <img src={pendingImagePreview} alt={pendingSuggestion?.name ?? 'preview'} style={{ width: '100%', display: 'block' }} />
          </div>
        ) : null}
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <div>
            <Text strong>{pendingSuggestion?.name}</Text>
            {pendingSuggestion?.itemNumber ? (
              <div>
                <Text type="secondary">SKU: {pendingSuggestion.itemNumber}</Text>
              </div>
            ) : null}
          </div>

          {pendingSuggestion?.existingProductFound ? (
            <Alert
              type="info"
              showIcon
              message={t('chat.match_exact')}
              description={t('chat.match_exact_hint')}
            />
          ) : null}

          {pendingSuggestion?.similarProductCandidates?.length ? (
            <>
              <Alert
                type="warning"
                showIcon
                message={t('chat.match_candidate')}
                description={t('chat.match_candidate_hint')}
              />
              <Radio.Group
                value={selectedCandidateId}
                onChange={(event) => setSelectedCandidateId(event.target.value)}
                style={{ width: '100%' }}
              >
                <Space direction="vertical" style={{ width: '100%' }}>
                  {pendingSuggestion.similarProductCandidates.map((candidate) => (
                    <Radio key={candidate.productId} value={candidate.productId}>
                      {candidate.displayName}
                      {candidate.itemNumber ? ` · ${candidate.itemNumber}` : ''}
                    </Radio>
                  ))}
                </Space>
              </Radio.Group>
              <Text type="secondary">{t('chat.match_candidate_manual')}</Text>
            </>
          ) : null}

          {!pendingSuggestion?.existingProductFound ? (
            <>
              <Divider style={{ margin: '8px 0' }} />
              <Checkbox checked={publishOnConfirm} onChange={(event) => setPublishOnConfirm(event.target.checked)}>
                {t('chat.publish_checkbox')}
              </Checkbox>
              <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                {publishOnConfirm ? t('chat.publish_public_hint') : t('chat.publish_draft_hint')}
              </Paragraph>
            </>
          ) : null}
        </Space>
      </Modal>
    </>
  );
}

function readFileAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result ?? ''));
    reader.onerror = () => reject(new Error('Failed to read file'));
    reader.readAsDataURL(file);
  });
}

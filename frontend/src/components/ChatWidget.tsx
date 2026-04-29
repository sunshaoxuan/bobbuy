import { type ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
import { api, type AiOnboardingSuggestion, type ChatConversationSlice, type ChatMessage } from '../api';
import { useChatPersistence } from '../hooks/useChatPersistence';
import { useChatWebSocket } from '../hooks/useChatWebSocket';
import { useNetworkStatus } from '../hooks/useNetworkStatus';
import { useI18n } from '../i18n';

const { Text, Paragraph } = Typography;
const CHAT_PAGE_SIZE = 50;

interface ChatWidgetProps {
  orderId?: number;
  tripId?: number;
  senderId: string;
  recipientId: string;
}

type ConversationType = 'PRIVATE' | 'ORDER' | 'TRIP';
type RetryableImageDraft = {
  base64: string;
  fileName: string;
};

const messageKey = (chatMessage: ChatMessage) =>
  String(
    chatMessage.metadata?.clientMessageId ??
      chatMessage.id ??
      `${chatMessage.tripId ?? ''}-${chatMessage.orderId ?? ''}-${chatMessage.createdAt ?? ''}-${chatMessage.senderId}-${chatMessage.recipientId}-${chatMessage.type}-${String(chatMessage.metadata?.url ?? '').slice(-32)}`
  );

export default function ChatWidget({ orderId, tripId, senderId, recipientId }: ChatWidgetProps) {
  const { t } = useI18n();
  const { isOnline } = useNetworkStatus();
  const [open, setOpen] = useState(false);
  const [sendingMessage, setSendingMessage] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [loadError, setLoadError] = useState<string>();
  const [pendingSuggestion, setPendingSuggestion] = useState<AiOnboardingSuggestion | null>(null);
  const [pendingImagePreview, setPendingImagePreview] = useState<string>();
  const [pendingAttachmentName, setPendingAttachmentName] = useState<string>();
  const [confirmingImage, setConfirmingImage] = useState(false);
  const [publishOnConfirm, setPublishOnConfirm] = useState(false);
  const [selectedCandidateId, setSelectedCandidateId] = useState<string>();
  const [retryableImageDraft, setRetryableImageDraft] = useState<RetryableImageDraft | null>(null);
  const [publishingMessageId, setPublishingMessageId] = useState<string>();
  const [confirmError, setConfirmError] = useState<string>();
  const [hasMoreHistory, setHasMoreHistory] = useState(false);
  const [nextCursor, setNextCursor] = useState<number | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const knownMessageKeysRef = useRef<Set<string>>(new Set());
  const loadingMessagesRef = useRef(false);
  const flushingPendingRef = useRef(false);
  // Latest-value refs used inside callbacks to avoid stale-closure re-creation cycles
  const messagesRef = useRef<ReturnType<typeof useChatPersistence>['persistedState']['messages']>([]);
  const pendingMessagesRef = useRef<ReturnType<typeof useChatPersistence>['persistedState']['pendingMessages']>([]);
  const nextCursorRef = useRef<number | null>(null);
  const hasScopedConversation = Boolean(tripId || orderId);

  const conversationType: ConversationType = tripId ? 'TRIP' : orderId ? 'ORDER' : 'PRIVATE';
  const conversationKey = useMemo(
    () => buildConversationStorageKey({ conversationType, orderId, tripId, senderId, recipientId }),
    [conversationType, orderId, tripId, senderId, recipientId]
  );
  const {
    persistedState,
    setMessages,
    setPendingMessages,
    setInputValue,
    setUnreadCount,
    setLastSuccessfulSyncAt
  } = useChatPersistence(conversationKey);
  const messages = persistedState.messages;
  const pendingMessages = persistedState.pendingMessages;
  const inputValue = persistedState.inputValue;
  const unreadCount = persistedState.unreadCount;
  const lastSuccessfulSyncAt = persistedState.lastSuccessfulSyncAt;

  // Keep latest-value refs in sync so callbacks can read them without adding
  // them to dependency arrays (which would recreate the callbacks on every
  // state update and cause an infinite render loop).
  messagesRef.current = messages;
  pendingMessagesRef.current = pendingMessages;
  nextCursorRef.current = nextCursor;

  useEffect(() => {
    knownMessageKeysRef.current = new Set(messages.map(messageKey));
  }, [messages]);

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

  const fetchConversationPage = useCallback(
    async (beforeId?: number): Promise<ChatConversationSlice> => {
      const params = { beforeId, limit: CHAT_PAGE_SIZE };
      if (tripId) {
        return api.getTripChatCursor(tripId, params);
      }
      if (orderId) {
        return api.getOrderChatCursor(orderId, params);
      }
      return api.getPrivateChatCursor(senderId, recipientId, params);
    },
    [orderId, recipientId, senderId, tripId]
  );

  const syncFromSlice = useCallback(
    (slice: ChatConversationSlice, options?: { appendHistory?: boolean; baseline?: boolean }) => {
      const currentMessages = messagesRef.current;
      const currentPendingMessages = pendingMessagesRef.current;
      const currentNextCursor = nextCursorRef.current;
      const baseMessages = options?.appendHistory
        ? [...currentMessages, ...slice.messages]
        : preserveOlderMessages(currentMessages, currentNextCursor, slice.messages);
      const mergedMessages = mergeChatMessages(baseMessages, currentPendingMessages);
      const nextKeys = mergedMessages.map(messageKey);
      if (options?.baseline) {
        knownMessageKeysRef.current = new Set(nextKeys);
        setUnreadCount(0);
      } else if (!open) {
        const unseen = nextKeys.filter((key) => !knownMessageKeysRef.current.has(key)).length;
        if (unseen > 0) {
          setUnreadCount((current) => current + unseen);
        }
      } else {
        setUnreadCount(0);
      }
      knownMessageKeysRef.current = new Set(nextKeys);
      setMessages(mergedMessages);
      setHasMoreHistory(slice.hasMore);
      setNextCursor(slice.nextCursor);
      setLoadError(undefined);
      setLastSuccessfulSyncAt(new Date().toISOString());
    },
    [open, setLastSuccessfulSyncAt, setMessages, setUnreadCount]
  );

  const loadMessages = useCallback(
    async ({
      silent = false,
      showFeedback = false,
      baseline = false,
      beforeId,
      appendHistory = false
    }: {
      silent?: boolean;
      showFeedback?: boolean;
      baseline?: boolean;
      beforeId?: number;
      appendHistory?: boolean;
    } = {}) => {
      if (loadingMessagesRef.current) {
        return;
      }
      loadingMessagesRef.current = true;
      if (!silent) {
        setRefreshing(true);
      }
      try {
        const slice = await fetchConversationPage(beforeId);
        syncFromSlice(slice, { appendHistory, baseline });
        if (showFeedback) {
          message.success(t('chat.refresh_success'));
        }
      } catch {
        if (!silent || open) {
          setLoadError(messagesRef.current.length > 0 ? t('chat.load_failed_keep_last') : t('chat.load_failed'));
        }
        if (showFeedback) {
          message.error(t('chat.load_failed'));
        }
      } finally {
        loadingMessagesRef.current = false;
        if (!silent) {
          setRefreshing(false);
        }
      }
    },
    [fetchConversationPage, open, syncFromSlice, t]
  );

  const websocketDestination = useMemo(
    () => buildConversationDestination({ conversationType, orderId, tripId, senderId, recipientId }),
    [conversationType, orderId, recipientId, senderId, tripId]
  );

  const sendQueuedMessage = useCallback(
    async (queuedMessage: ChatMessage, fromReconnect = false) => {
      const key = messageKey(queuedMessage);
      try {
        await api.sendChatMessage(stripLocalDeliveryState(queuedMessage));
        setPendingMessages((current) => current.filter((entry) => messageKey(entry) !== key));
        setMessages((current) =>
          current.map((entry) =>
            messageKey(entry) === key
              ? {
                  ...entry,
                  metadata: {
                    ...entry.metadata,
                    deliveryState: 'PENDING'
                  }
                }
              : entry
          )
        );
        if (!fromReconnect) {
          void loadMessages({ silent: true });
        }
        return true;
      } catch {
        setPendingMessages((current) => current.filter((entry) => messageKey(entry) !== key));
        setMessages((current) =>
          current.map((entry) =>
            messageKey(entry) === key
              ? {
                  ...entry,
                  metadata: {
                    ...entry.metadata,
                    deliveryState: isOnline ? 'FAILED' : 'QUEUED'
                  }
                }
              : entry
          )
        );
        if (isOnline) {
          message.error(t('errors.request_failed'));
        }
        return false;
      }
    },
    [isOnline, loadMessages, setMessages, setPendingMessages, t]
  );

  const flushPendingMessages = useCallback(async () => {
    if (!isOnline || flushingPendingRef.current) {
      return;
    }
    const queuedItems = pendingMessages.filter((entry) => entry.metadata?.deliveryState === 'QUEUED');
    if (queuedItems.length === 0) {
      return;
    }
    flushingPendingRef.current = true;
    try {
      for (const queuedItem of queuedItems) {
        const sent = await sendQueuedMessage(queuedItem, true);
        if (!sent) {
          break;
        }
      }
      await loadMessages({ silent: true });
    } finally {
      flushingPendingRef.current = false;
    }
  }, [isOnline, loadMessages, pendingMessages, sendQueuedMessage]);

  useChatWebSocket({
    enabled: Boolean(senderId && recipientId && (hasScopedConversation || open || pendingMessages.length > 0)),
    destination: websocketDestination,
    onConnect: ({ reconnected }) => {
      if (reconnected) {
        void loadMessages({ silent: true });
      }
      void flushPendingMessages();
    },
    onMessage: () => {
      void loadMessages({ silent: true });
    }
  });

  useEffect(() => {
    void loadMessages({ silent: !open, baseline: !open });
  }, [conversationKey, loadMessages, open]);

  useEffect(() => {
    if (open) {
      void loadMessages();
    }
  }, [loadMessages, open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setUnreadCount(0);
    knownMessageKeysRef.current = new Set(messages.map(messageKey));
  }, [messages, open, setUnreadCount]);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  useEffect(() => {
    void flushPendingMessages();
  }, [flushPendingMessages]);

  const handleSend = async () => {
    if (!inputValue.trim() || sendingMessage) {
      return;
    }
    const optimisticMessage = buildOptimisticTextMessage({
      orderId,
      tripId,
      senderId,
      recipientId,
      content: inputValue.trim(),
      conversationType,
      isOnline
    });
    setInputValue('');
    setMessages((current) => mergeChatMessages(current, [optimisticMessage]));
    if (!isOnline) {
      setPendingMessages((current) => mergeChatMessages(current, [optimisticMessage]));
      message.info(t('chat.offline_queue_notice'));
      return;
    }
    setSendingMessage(true);
    try {
      await sendQueuedMessage(optimisticMessage);
    } finally {
      setSendingMessage(false);
    }
  };

  const handleSelectImage = () => {
    if (!isOnline) {
      message.info(t('chat.offline_image_notice'));
      return;
    }
    fileInputRef.current?.click();
  };

  const scanImageDraft = useCallback(
    async (base64: string, fileName: string) => {
      setRetryableImageDraft({ base64, fileName });
      setConfirmingImage(true);
      setConfirmError(undefined);
      try {
        const suggestion = await api.onboardScan(base64);
        setPendingSuggestion(suggestion);
        setPendingImagePreview(base64);
        setPendingAttachmentName(fileName);
        setPublishOnConfirm(false);
        setSelectedCandidateId(undefined);
        setRetryableImageDraft(null);
      } catch {
        message.error(t('chat.image_scan_failed'));
      } finally {
        setConfirmingImage(false);
      }
    },
    [t]
  );

  const handleImagePicked = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    const fileName = file.name;
    setConfirmingImage(true);
    try {
      const base64 = await readFileAsBase64(file);
      await scanImageDraft(base64, fileName);
    } catch {
      message.error(t('chat.image_scan_failed'));
    } finally {
      event.target.value = '';
    }
  };

  const confirmImageFlow = async () => {
    if (!pendingSuggestion || confirmingImage) {
      return;
    }
    setConfirmingImage(true);
    setConfirmError(undefined);
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
      const confirmedAt = new Date().toISOString();
      const previewUrl: string | undefined =
        response.product.mediaGallery?.[0]?.url ?? pendingImagePreview ?? pendingSuggestion.mediaGallery?.[0]?.url;
      const productName = response.displayName || pendingSuggestion.name;
      const draftOnly = response.product.visibilityStatus === 'DRAFTER_ONLY' || (!payload.existingProductFound && !publishOnConfirm);
      const visibilityStatus = draftOnly ? 'DRAFTER_ONLY' : response.product.visibilityStatus ?? 'PUBLIC';
      const candidate = pendingSuggestion.similarProductCandidates?.find((entry) => entry.productId === selectedCandidateId);
      const recommendedProductIds = pendingSuggestion.similarProductCandidates?.map((entry) => entry.productId) ?? [];
      const rejectedProductIds = recommendedProductIds.filter((productId) => productId !== selectedCandidateId);
      const candidateDecision = payload.existingProductFound
        ? pendingSuggestion.existingProductFound
          ? 'EXACT_MATCH'
          : 'SELECTED_CANDIDATE'
        : 'CREATED_TEMP_PRODUCT';
      const imageFlowStatus = draftOnly
        ? payload.existingProductFound
          ? pendingSuggestion.existingProductFound
            ? 'MATCHED_EXISTING_PRODUCT'
            : 'CANDIDATE_SELECTED'
          : 'TEMP_PRODUCT_CREATED'
        : 'PUBLISHED_TO_MARKET';
      const matchedBy = payload.existingProductFound
        ? pendingSuggestion.existingProductFound
          ? 'ITEM_NUMBER'
          : 'NAME_CANDIDATE'
        : 'NEW_TEMP_PRODUCT';

      await api.sendChatMessage({
        orderId,
        tripId,
        senderId,
        recipientId,
        content: productName,
        type: 'IMAGE',
        metadata: {
          conversationType,
          source: 'CHAT_WIDGET',
          orderId,
          tripId,
          relatedOrderId: orderId,
          relatedTripId: tripId,
          url: previewUrl,
          attachmentUrl: previewUrl,
          attachmentName: pendingAttachmentName,
          auditVersion: 'V14',
          operatorId: senderId,
          decisionAt: confirmedAt,
          productId: response.product.id,
          productName,
          itemNumber: response.product.itemNumber,
          visibilityStatus,
          isTemporary: response.product.isTemporary ?? true,
          existingProductFound: payload.existingProductFound ?? false,
          matchedBy,
          imageFlowStatus,
          publishedAt: draftOnly ? undefined : confirmedAt,
          recoveryAction: previewUrl ? undefined : 'REQUEST_ATTACHMENT_REUPLOAD',
          candidateSelectionResult: candidateDecision,
          candidateReason: candidate?.matchReason,
          candidateReasons: candidate?.matchSignals ?? [],
          candidateSummary: {
            brand: candidate?.brand,
            categoryId: candidate?.categoryId,
            matchedFragments: candidate?.matchedFragments ?? [],
            aliasSources: candidate?.aliasSources ?? [],
            presentedCount: recommendedProductIds.length,
            rejectedCount: rejectedProductIds.length
          },
          candidateAudit: {
            decision: candidateDecision,
            selectedProductId: response.product.id,
            recommendedProductIds,
            rejectedProductIds,
            selectedReason: candidate?.matchReason ?? matchedBy,
            reviewedBy: senderId,
            triggerEntry: 'CHAT_IMAGE_CONFIRMATION',
            confirmedAt,
            publishedAt: draftOnly ? undefined : confirmedAt
          }
        }
      });

      setPendingSuggestion(null);
      setPendingImagePreview(undefined);
      setPendingAttachmentName(undefined);
      setSelectedCandidateId(undefined);
      setPublishOnConfirm(false);
      setRetryableImageDraft(null);
      await loadMessages();
      message.success(t('chat.image_sent_success'));
    } catch {
      setConfirmError(t('errors.request_failed'));
      message.error(t('errors.request_failed'));
    } finally {
      setConfirmingImage(false);
    }
  };

  const handlePublishInChat = async (target: ChatMessage) => {
    const productId = String(target.metadata?.productId ?? '');
    const targetMessageId = messageKey(target);
    if (!productId || publishingMessageId === targetMessageId) {
      return;
    }
    const alreadyPublished =
      target.metadata?.imageFlowStatus === 'PUBLISHED_TO_MARKET' || target.metadata?.visibilityStatus === 'PUBLIC';
    if (alreadyPublished) {
      message.info(t('chat.image_status.PUBLISHED_TO_MARKET'));
      return;
    }
    setPublishingMessageId(targetMessageId);
    try {
      await api.patchProduct(productId, { visibilityStatus: 'PUBLIC' });
      setMessages((current) =>
        current.map((entry) =>
          messageKey(entry) === targetMessageId
            ? {
                ...entry,
                metadata: {
                  ...entry.metadata,
                  visibilityStatus: 'PUBLIC',
                  imageFlowStatus: 'PUBLISHED_TO_MARKET',
                  publishedAt: new Date().toISOString(),
                  recoveryAction: undefined
                }
              }
            : entry
        )
      );
      message.success(t('chat.publish_success'));
    } catch {
      setMessages((current) =>
        current.map((entry) =>
          messageKey(entry) === targetMessageId
            ? {
                ...entry,
                metadata: {
                  ...entry.metadata,
                  imageFlowStatus: 'PUBLISH_FAILED',
                  recoveryAction: 'RETRY_PUBLISH'
                }
              }
            : entry
        )
      );
      message.error(t('errors.request_failed'));
    } finally {
      setPublishingMessageId(undefined);
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
            aria-label="Open chat"
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
          {!isOnline ? (
            <Alert
              type="info"
              showIcon
              message={t('chat.offline_banner')}
              style={{ margin: 12, marginBottom: 0 }}
            />
          ) : null}
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
          {retryableImageDraft ? (
            <Alert
              type="info"
              showIcon
              message={t('chat.pending_upload')}
              description={retryableImageDraft.fileName}
              action={
                <Button size="small" loading={confirmingImage} onClick={() => void scanImageDraft(retryableImageDraft.base64, retryableImageDraft.fileName)}>
                  {t('chat.retry_image_upload')}
                </Button>
              }
              style={{ margin: '0 12px 12px' }}
            />
          ) : null}
          <div ref={scrollRef} style={{ flex: 1, overflowY: 'auto', padding: '16px', background: '#fafafa' }}>
            {hasMoreHistory && nextCursor ? (
              <div style={{ marginBottom: 12, textAlign: 'center' }}>
                <Button size="small" onClick={() => void loadMessages({ beforeId: nextCursor, appendHistory: true })}>
                  {t('chat.load_older')}
                </Button>
              </div>
            ) : null}
            <List
              locale={{ emptyText: refreshing ? t('chat.loading') : t('chat.empty') }}
              dataSource={messages}
              split={false}
              renderItem={(msg) => {
                const canPublish = msg.metadata?.isTemporary && msg.metadata?.visibilityStatus === 'DRAFTER_ONLY';
                const statusLabel = getImageFlowStatusLabel(msg, t);
                const recoveryActionLabel = getRecoveryActionLabel(msg, t);
                const deliveryStateLabel = getDeliveryStateLabel(msg, t);
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
                        border: msg.senderId === senderId ? 'none' : 'var(--zen-line)',
                        opacity: msg.metadata?.deliveryState === 'FAILED' ? 0.7 : 1
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
                            {msg.metadata?.productSnapshot?.summary ? (
                              <div>
                                <Text style={{ color: 'inherit', opacity: 0.8 }}>{msg.metadata.productSnapshot.summary}</Text>
                              </div>
                            ) : null}
                            <Space wrap size={4} style={{ marginTop: 6 }}>
                              {msg.metadata?.matchedBy ? <Tag>{String(msg.metadata.matchedBy)}</Tag> : null}
                              {msg.metadata?.visibilityStatus ? <Tag color={canPublish ? 'orange' : 'green'}>{String(msg.metadata.visibilityStatus)}</Tag> : null}
                              {statusLabel ? <Tag color={getImageFlowStatusColor(msg.metadata?.imageFlowStatus)}>{statusLabel}</Tag> : null}
                            </Space>
                            {msg.metadata?.candidateReasons?.length ? (
                              <div style={{ marginTop: 6 }}>
                                <Text style={{ color: 'inherit', opacity: 0.85 }}>
                                  {t('chat.candidate_reason_title')}: {msg.metadata.candidateReasons.join(' · ')}
                                </Text>
                              </div>
                            ) : null}
                            {msg.metadata?.candidateSummary?.brand || msg.metadata?.candidateSummary?.categoryId ? (
                              <div style={{ marginTop: 6 }}>
                                <Text style={{ color: 'inherit', opacity: 0.85 }}>
                                  {[
                                    msg.metadata?.candidateSummary?.brand
                                      ? `${t('chat.candidate_brand')}: ${msg.metadata.candidateSummary.brand}`
                                      : null,
                                    msg.metadata?.candidateSummary?.categoryId
                                      ? `${t('chat.candidate_category')}: ${msg.metadata.candidateSummary.categoryId}`
                                      : null
                                  ]
                                    .filter(Boolean)
                                    .join(' · ')}
                                </Text>
                              </div>
                            ) : null}
                            {msg.metadata?.candidateSummary?.matchedFragments?.length ? (
                              <div style={{ marginTop: 6 }}>
                                <Text style={{ color: 'inherit', opacity: 0.85 }}>
                                  {t('chat.candidate_fragments')}: {msg.metadata.candidateSummary.matchedFragments.join(' · ')}
                                </Text>
                              </div>
                            ) : null}
                            {msg.metadata?.candidateSummary?.aliasSources?.length ? (
                              <div style={{ marginTop: 6 }}>
                                <Text style={{ color: 'inherit', opacity: 0.85 }}>
                                  {t('chat.candidate_alias_sources')}: {msg.metadata.candidateSummary.aliasSources.join(' · ')}
                                </Text>
                              </div>
                            ) : null}
                            {msg.metadata?.candidateAudit?.reviewedBy || msg.metadata?.candidateSummary?.rejectedCount ? (
                              <div style={{ marginTop: 6 }}>
                                <Text style={{ color: 'inherit', opacity: 0.85 }}>
                                  {t('chat.audit_label')}: {msg.metadata?.candidateAudit?.reviewedBy ?? senderId}
                                  {msg.metadata?.candidateSummary?.rejectedCount
                                    ? ` · ${t('chat.audit_rejected')} ${msg.metadata.candidateSummary.rejectedCount}`
                                    : ''}
                                </Text>
                              </div>
                            ) : null}
                            {recoveryActionLabel ? (
                              <div style={{ marginTop: 6 }}>
                                <Text style={{ color: 'inherit', opacity: 0.85 }}>
                                  {t('chat.recovery_action_title')}: {recoveryActionLabel}
                                </Text>
                              </div>
                            ) : null}
                          </div>
                          {canPublish ? (
                            <Button
                              size="small"
                              icon={<ShopOutlined />}
                              loading={publishingMessageId === messageKey(msg)}
                              onClick={() => void handlePublishInChat(msg)}
                            >
                              {msg.metadata?.imageFlowStatus === 'PUBLISH_FAILED' ? t('chat.publish_retry_action') : t('chat.publish_action')}
                            </Button>
                          ) : null}
                        </Space>
                      ) : (
                        <Text style={{ color: 'inherit' }}>{msg.content}</Text>
                      )}
                      {deliveryStateLabel ? (
                        <div style={{ marginTop: 6 }}>
                          <Text style={{ color: 'inherit', opacity: 0.75 }}>{deliveryStateLabel}</Text>
                        </div>
                      ) : null}
                      <div style={{ fontSize: '10px', opacity: 0.6, textAlign: 'right', marginTop: 4 }}>
                        {formatChatTime(msg.createdAt)}
                      </div>
                    </div>
                  </div>
                );
              }}
            />
          </div>
          <div style={{ padding: '12px', borderTop: 'var(--zen-line)', background: 'white' }}>
            {lastSuccessfulSyncAt ? (
              <div style={{ marginBottom: 8 }}>
                <Text type="secondary" style={{ fontSize: 12 }}>
                  {t('chat.last_synced')}: {formatChatTime(lastSuccessfulSyncAt, { withSeconds: true })}
                </Text>
              </div>
            ) : null}
            <Space.Compact style={{ width: '100%' }}>
              <Button aria-label="Refresh chat" icon={<ReloadOutlined />} loading={refreshing} onClick={() => void loadMessages({ showFeedback: true })} />
              <Button aria-label="Upload image" icon={<CameraOutlined />} loading={confirmingImage} onClick={handleSelectImage} />
              <Input
                placeholder={t('chat.placeholder')}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onPressEnter={() => void handleSend()}
              />
              <Button aria-label="Send message" type="primary" icon={<SendOutlined />} onClick={() => void handleSend()} loading={sendingMessage} />
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
            setPendingAttachmentName(undefined);
            setSelectedCandidateId(undefined);
            setPublishOnConfirm(false);
            setConfirmError(undefined);
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
          <Tag color="processing">{t('chat.image_status.PENDING_CONFIRMATION')}</Tag>
          {confirmError ? <Alert type="error" showIcon message={confirmError} /> : null}
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
                      <Space direction="vertical" size={2}>
                        <Text strong>
                          {candidate.displayName}
                          {candidate.itemNumber ? ` · ${candidate.itemNumber}` : ''}
                          {candidate.matchSignals?.length ? ` · ${candidate.matchSignals.join(' / ')}` : ''}
                        </Text>
                        {candidate.brand || candidate.categoryId ? (
                          <Text type="secondary">
                            {[
                              candidate.brand ? `${t('chat.candidate_brand')}: ${candidate.brand}` : null,
                              candidate.categoryId ? `${t('chat.candidate_category')}: ${candidate.categoryId}` : null
                            ]
                              .filter(Boolean)
                              .join(' · ')}
                          </Text>
                        ) : null}
                        {candidate.matchedFragments?.length ? (
                          <Text type="secondary">
                            {t('chat.candidate_fragments')}: {candidate.matchedFragments.join(' · ')}
                          </Text>
                        ) : null}
                        {candidate.aliasSources?.length ? (
                          <Text type="secondary">
                            {t('chat.candidate_alias_sources')}: {candidate.aliasSources.join(' · ')}
                          </Text>
                        ) : null}
                      </Space>
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

function getImageFlowStatusLabel(msg: ChatMessage, t: (key: string) => string) {
  if (msg.type !== 'IMAGE') {
    return undefined;
  }
  const status = msg.metadata?.imageFlowStatus;
  if (!status) {
    return undefined;
  }
  return t(`chat.image_status.${status}`);
}

function getImageFlowStatusColor(status?: string) {
  if (status === 'PUBLISHED_TO_MARKET') {
    return 'green';
  }
  if (status === 'PUBLISH_FAILED') {
    return 'red';
  }
  return 'blue';
}

function getRecoveryActionLabel(msg: ChatMessage, t: (key: string) => string) {
  const recoveryAction = msg.metadata?.recoveryAction;
  if (!recoveryAction) {
    return undefined;
  }
  return t(`chat.recovery.${recoveryAction}`);
}

function getDeliveryStateLabel(msg: ChatMessage, t: (key: string) => string) {
  const deliveryState = msg.metadata?.deliveryState;
  if (!deliveryState) {
    return undefined;
  }
  return t(`chat.delivery.${deliveryState}`);
}

function formatChatTime(value?: string, options?: { withSeconds?: boolean }) {
  if (!value) {
    return '--:--';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: options?.withSeconds ? '2-digit' : undefined,
    hour12: false
  });
}

function buildConversationDestination({
  conversationType,
  orderId,
  tripId,
  senderId,
  recipientId
}: {
  conversationType: ConversationType;
  orderId?: number;
  tripId?: number;
  senderId: string;
  recipientId: string;
}) {
  if (conversationType === 'TRIP' && tripId) {
    return `/topic/trip/${tripId}`;
  }
  if (conversationType === 'ORDER' && orderId) {
    return `/topic/order/${orderId}`;
  }
  const [firstParticipant, secondParticipant] = [senderId, recipientId]
    .sort((left, right) => (left < right ? -1 : left > right ? 1 : 0));
  return `/topic/private/${encodeURIComponent(firstParticipant)}/${encodeURIComponent(secondParticipant)}`;
}

function buildConversationStorageKey({
  conversationType,
  orderId,
  tripId,
  senderId,
  recipientId
}: {
  conversationType: ConversationType;
  orderId?: number;
  tripId?: number;
  senderId: string;
  recipientId: string;
}) {
  if (conversationType === 'TRIP' && tripId) {
    return `trip-${tripId}`;
  }
  if (conversationType === 'ORDER' && orderId) {
    return `order-${orderId}`;
  }
  const [firstParticipant, secondParticipant] = [senderId, recipientId]
    .sort((left, right) => (left < right ? -1 : left > right ? 1 : 0));
  return `private-${firstParticipant}-${secondParticipant}`;
}

function buildOptimisticTextMessage({
  orderId,
  tripId,
  senderId,
  recipientId,
  content,
  conversationType,
  isOnline
}: {
  orderId?: number;
  tripId?: number;
  senderId: string;
  recipientId: string;
  content: string;
  conversationType: ConversationType;
  isOnline: boolean;
}): ChatMessage {
  return {
    orderId,
    tripId,
    senderId,
    recipientId,
    content,
    type: 'TEXT',
    createdAt: new Date().toISOString(),
    metadata: {
      conversationType,
      source: 'CHAT_WIDGET',
      clientMessageId: `client-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
      deliveryState: isOnline ? 'PENDING' : 'QUEUED'
    }
  };
}

function stripLocalDeliveryState(message: ChatMessage): ChatMessage {
  if (!message.metadata) {
    return message;
  }
  const { deliveryState, ...metadata } = message.metadata;
  return {
    ...message,
    metadata
  };
}

function mergeChatMessages(baseMessages: ChatMessage[], pendingMessages: ChatMessage[]) {
  const ordered = [...baseMessages, ...pendingMessages];
  const byKey = new Map<string, ChatMessage>();
  for (const entry of ordered) {
    const key = messageKey(entry);
    const existing = byKey.get(key);
    if (!existing) {
      byKey.set(key, entry);
      continue;
    }
    byKey.set(key, preferSyncedMessage(existing, entry));
  }
  return [...byKey.values()].sort(compareChatMessages);
}

function preserveOlderMessages(currentMessages: ChatMessage[], previousCursor: number | null, latestMessages: ChatMessage[]) {
  if (!previousCursor) {
    return latestMessages;
  }
  const olderMessages = currentMessages.filter((entry) => typeof entry.id === 'number' && entry.id < previousCursor);
  return [...olderMessages, ...latestMessages];
}

function preferSyncedMessage(left: ChatMessage, right: ChatMessage) {
  const preferred = left.id && !right.id ? left : right.id && !left.id ? right : right;
  const fallback = preferred === left ? right : left;
  const metadata = { ...fallback.metadata, ...preferred.metadata };
  if (preferred.id && !fallback.id && (preferred.metadata?.deliveryState === null || preferred.metadata?.deliveryState === undefined)) {
    delete metadata.deliveryState;
  }
  return { ...fallback, ...preferred, metadata };
}

function compareChatMessages(left: ChatMessage, right: ChatMessage) {
  const leftTime = left.createdAt ? new Date(left.createdAt).getTime() : 0;
  const rightTime = right.createdAt ? new Date(right.createdAt).getTime() : 0;
  if (leftTime !== rightTime) {
    return leftTime - rightTime;
  }
  return Number(left.id ?? 0) - Number(right.id ?? 0);
}

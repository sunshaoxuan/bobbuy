import React, { useState } from 'react';
import { Modal, Upload, Button, Steps, Result, Spin, message, Space, Typography, Alert, Card, Divider, Progress } from 'antd';
import type { RcFile } from 'antd/es/upload';
import { CameraOutlined, UploadOutlined, FileSearchOutlined, GlobalOutlined, CheckCircleOutlined, PictureOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { api, type AiOnboardingSuggestion } from '../api';
import { useI18n } from '../i18n';
import AttributeDiffTable from './AttributeDiffTable';

const { Text, Title, Paragraph } = Typography;

interface AiQuickAddModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: (suggestion: AiOnboardingSuggestion) => void;
}

const AiQuickAddModal: React.FC<AiQuickAddModalProps> = ({ visible, onCancel, onSuccess }) => {
  const { t } = useI18n();
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [fileList, setFileList] = useState<any[]>([]);

  const [suggestion, setSuggestion] = useState<AiOnboardingSuggestion | null>(null);
  const lowConfidence = (suggestion?.matchScore ?? 100) < 70;
  const historicalImage = suggestion?.verificationTarget?.mediaGallery?.find((item) => item.type === 'IMAGE' || item.type === 'image')?.url;

  React.useEffect(() => {
    if (visible) {
      setCurrentStep(0);
      setLoading(false);
      setFileList([]);
      setSuggestion(null);
    }
  }, [visible]);

  const handleUpload = (file: RcFile) => {
    setLoading(true);
    setCurrentStep(1);

    const reader = new FileReader();
    reader.onload = async () => {
      try {
        const base64 = reader.result as string;
        // Inject the original photo into the suggestion
        const result = await api.onboardScan(base64, file.name);
        setSuggestion({ ...result, originalPhotoBase64: base64 });
        
        setCurrentStep(2);
        await new Promise(resolve => setTimeout(resolve, 800));
        
        setCurrentStep(3);
        await new Promise(resolve => setTimeout(resolve, 500));

        setCurrentStep(4);
        setLoading(false);
      } catch (error) {
        logError(error);
        setLoading(false);
        setCurrentStep(0);
        message.error(t('stock.ai_quick_add.failed'));
      }
    };
    reader.onerror = () => {
      setLoading(false);
      setCurrentStep(0);
      message.error(t('stock.ai_quick_add.failed'));
    };
    reader.readAsDataURL(file);
    return false; // Prevent automatic upload
  };

  const logError = (err: any) => {
    console.error('AI Onboarding Error:', err);
  };

  const handleFinish = () => {
    if (suggestion) {
      onSuccess(suggestion);
    }
  };

  const handleSaveAsNew = () => {
    if (!suggestion) {
      return;
    }
    onSuccess({
      ...suggestion,
      existingProductFound: false,
      existingProductId: undefined,
    });
  };

  const steps = [
    { title: t('stock.ai_quick_add.step_upload'), icon: <CameraOutlined /> },
    { title: t('stock.ai_quick_add.step_scan'), icon: <FileSearchOutlined /> },
    { title: t('stock.ai_quick_add.step_research'), icon: <GlobalOutlined /> },
    { title: t('stock.ai_quick_add.step_enrich'), icon: <PictureOutlined /> },
    { title: t('stock.ai_quick_add.step_done'), icon: <CheckCircleOutlined /> },
  ];
  const stageByStep = ['UPLOAD', 'SCANNING', 'RESEARCHING', 'ENRICHING', 'SUCCESS'] as const;

  return (
    <Modal
      title={t('stock.ai_quick_add.title')}
      open={visible}
      onCancel={onCancel}
      footer={currentStep === 4 ? [
        suggestion?.verificationTarget ? (
          <Button key="save-as-new" onClick={handleSaveAsNew}>
            {t('stock.ai_quick_add.save_as_new')}
          </Button>
        ) : null,
        <Button
          key="ok"
          type="primary"
          onClick={handleFinish}
          disabled={Boolean(suggestion?.verificationTarget) && lowConfidence}
        >
          {t('stock.master.edit_detail')}
        </Button>
      ].filter(Boolean) : null}
      width={880}
      centered
      destroyOnClose
    >
      <div style={{ padding: '24px 0' }}>
        <Steps
          current={currentStep}
          items={steps.map(s => ({ title: s.title, icon: s.icon }))}
          size="small"
          style={{ marginBottom: 32 }}
          data-testid="ai-onboarding-steps"
          data-stage={stageByStep[currentStep]}
        />

        {currentStep === 0 && (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Upload.Dragger
              accept="image/*"
              beforeUpload={handleUpload}
              showUploadList={false}
              multiple={false}
            >
              <p className="ant-upload-drag-icon">
                <CameraOutlined style={{ fontSize: 48, color: '#1890ff' }} />
              </p>
              <p className="ant-upload-text">{t('stock.ai_quick_add.drag_tip')}</p>
              <p className="ant-upload-hint">{t('stock.ai_quick_add.hint')}</p>
              <Button type="primary" icon={<UploadOutlined />} style={{ marginTop: 16 }}>
                {t('stock.ai_quick_add.select_btn')}
              </Button>
            </Upload.Dragger>
          </div>
        )}

        {(currentStep > 0 && currentStep < 4) && (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <Spin size="large" />
            <div style={{ marginTop: 24 }}>
              <Text strong style={{ fontSize: 18 }}>
                {currentStep === 1 && (
                  <span>
                    <FileSearchOutlined style={{ marginRight: 8 }} />
                    OCR提取文本並由AI進行整理中...
                  </span>
                )}
                {currentStep === 2 && t('stock.ai_quick_add.researching_msg')}
                {currentStep === 3 && t('stock.ai_quick_add.enriching_msg')}
              </Text>
            </div>
            <div style={{ marginTop: 8, color: '#8c8c8c' }}>
              {t('stock.ai_quick_add.patience_tip')}
            </div>
          </div>
        )}

        {currentStep === 4 && (
          <>
            {suggestion?.verificationTarget && (
              <Alert
                message={`${t('stock.ai_quick_add.compare_mode')}${suggestion.verificationTarget.displayName ? `：${suggestion.verificationTarget.displayName}` : ''}`}
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}
            {suggestion?.existingProductFound && (
              <Alert
                data-testid="ai-existing-product-alert"
                message={`${t('stock.ai_quick_add.existing_found')}${suggestion.existingProductId ? `：${suggestion.existingProductId}` : ''}`}
                description={t('stock.ai_quick_add.existing_update_hint')}
                type="info"
                showIcon
                icon={<InfoCircleOutlined />}
                style={{ marginBottom: 24 }}
              />
            )}
            {lowConfidence && suggestion?.verificationTarget ? (
              <Alert
                data-testid="ai-low-match-warning"
                message={t('stock.ai_quick_add.low_match_warning')}
                description={t('stock.ai_quick_add.low_match_hint')}
                type="error"
                showIcon
                style={{ marginBottom: 24 }}
              />
            ) : null}
            {!suggestion?.existingProductFound && suggestion?.similarProductCandidates?.length ? (
              <Alert
                message={t('stock.ai_quick_add.candidate_found')}
                description={t('stock.ai_quick_add.candidate_hint')}
                type="warning"
                showIcon
                style={{ marginBottom: 24 }}
              />
            ) : null}
            {typeof suggestion?.matchScore === 'number' ? (
              <Card size="small" style={{ marginBottom: 16 }} data-testid="ai-match-score-card">
                <Space direction="vertical" style={{ width: '100%' }} size={12}>
                  <div>
                    <Text strong>{t('stock.ai_quick_add.match_score')}</Text>
                    <Title level={4} style={{ margin: '4px 0 0' }}>{`${Math.round(suggestion.matchScore)}%`}</Title>
                  </div>
                  <Progress
                    percent={Math.round(suggestion.matchScore)}
                    status={lowConfidence ? 'exception' : 'success'}
                    strokeColor={lowConfidence ? '#ff4d4f' : '#52c41a'}
                  />
                </Space>
              </Card>
            ) : null}
            {(historicalImage || suggestion?.originalPhotoBase64) && (
              <Card size="small" title={t('stock.ai_quick_add.visual_evidence')} style={{ marginBottom: 16 }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 16 }}>
                  <div>
                    <Text strong>{t('stock.ai_quick_add.history_image')}</Text>
                    <div style={{ marginTop: 8, border: '1px solid #f0f0f0', borderRadius: 8, minHeight: 180, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', background: '#fafafa' }}>
                      {historicalImage ? (
                        <img src={historicalImage} alt={t('stock.ai_quick_add.history_image')} style={{ width: '100%', objectFit: 'cover' }} />
                      ) : (
                        <Text type="secondary">{t('stock.ai_quick_add.no_history_image')}</Text>
                      )}
                    </div>
                  </div>
                  <div>
                    <Text strong>{t('stock.ai_quick_add.current_image')}</Text>
                    <div style={{ marginTop: 8, border: '1px solid #f0f0f0', borderRadius: 8, minHeight: 180, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden', background: '#fafafa' }}>
                      {suggestion?.originalPhotoBase64 ? (
                        <img src={suggestion.originalPhotoBase64} alt={t('stock.ai_quick_add.current_image')} style={{ width: '100%', objectFit: 'cover' }} />
                      ) : (
                        <Text type="secondary">{t('stock.ai_quick_add.no_current_image')}</Text>
                      )}
                    </div>
                  </div>
                </div>
              </Card>
            )}
            {suggestion?.fieldDiffs?.length ? (
              <Card size="small" title={t('stock.ai_quick_add.diff_table')} style={{ marginBottom: 16 }}>
                <AttributeDiffTable diffs={suggestion.fieldDiffs} />
              </Card>
            ) : null}
            {suggestion?.semanticReasoning ? (
              <Card size="small" title={t('stock.ai_quick_add.reasoning_title')} style={{ marginBottom: 16 }}>
                <Paragraph style={{ marginBottom: 0 }}>{suggestion.semanticReasoning}</Paragraph>
              </Card>
            ) : null}
            <Divider />
            <Result
              data-testid="ai-onboarding-result"
              subTitle={
                <span data-testid="ai-onboarding-result-subtitle" data-ai-status="SUCCESS">
                  {t('stock.ai_quick_add.success_subtitle')}
                </span>
              }
              status="success"
              title={t('stock.ai_quick_add.success_title')}
            />
          </>
        )}
      </div>
    </Modal>
  );
};

export default AiQuickAddModal;

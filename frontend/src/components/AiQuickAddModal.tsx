import React, { useState } from 'react';
import { Modal, Upload, Button, Steps, Result, Spin, message, Space, Typography } from 'antd';
import type { RcFile } from 'antd/es/upload';
import { CameraOutlined, UploadOutlined, FileSearchOutlined, GlobalOutlined, CheckCircleOutlined, PictureOutlined } from '@ant-design/icons';
import { api, type AiOnboardingSuggestion } from '../api';
import { useI18n } from '../i18n';

const { Text } = Typography;

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

  const handleUpload = async (file: RcFile) => {
    setLoading(true);
    setCurrentStep(1); // Scanning

    try {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = async () => {
        const base64 = reader.result as string;
        
        // Step 1: Scanning & Extracting
        setCurrentStep(1);
        const suggestion = await api.onboardScan(base64);
        
        // Step 2: Researching (Simulated delay or handled in backend)
        setCurrentStep(2);
        await new Promise(resolve => setTimeout(resolve, 1500));
        
        // Step 3: Enriching (Simulated delay)
        setCurrentStep(3);
        await new Promise(resolve => setTimeout(resolve, 1000));

        setCurrentStep(4); // Success
        setLoading(false);
        onSuccess(suggestion);
      };
    } catch (error) {
      setLoading(false);
      setCurrentStep(0);
      message.error(t('stock.ai_quick_add.failed'));
    }
    return false; // Prevent default upload
  };

  const steps = [
    { title: t('stock.ai_quick_add.step_upload'), icon: <CameraOutlined /> },
    { title: t('stock.ai_quick_add.step_scan'), icon: <FileSearchOutlined /> },
    { title: t('stock.ai_quick_add.step_research'), icon: <GlobalOutlined /> },
    { title: t('stock.ai_quick_add.step_enrich'), icon: <PictureOutlined /> },
    { title: t('stock.ai_quick_add.step_done'), icon: <CheckCircleOutlined /> },
  ];

  return (
    <Modal
      title={t('stock.ai_quick_add.title')}
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
      centered
      destroyOnClose
    >
      <div style={{ padding: '24px 0' }}>
        <Steps
          current={currentStep}
          items={steps.map(s => ({ title: s.title, icon: s.icon }))}
          size="small"
          style={{ marginBottom: 32 }}
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
                {currentStep === 1 && t('stock.ai_quick_add.scanning_msg')}
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
          <Result
            status="success"
            title={t('stock.ai_quick_add.success_title')}
            subTitle={t('stock.ai_quick_add.success_subtitle')}
          />
        )}
      </div>
    </Modal>
  );
};

export default AiQuickAddModal;

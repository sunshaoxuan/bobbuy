import React, { useEffect, useState } from 'react';
import { Card, Result, Space, Spin, Tag, Typography, Button } from 'antd';
import { AuditOutlined, CheckCircleOutlined, WarningOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import { api, FinancialAuditLog } from '../api';
import { useI18n } from '../i18n';

const { Title, Text } = Typography;

export default function ZenAuditView() {
    const { tripId } = useParams<{ tripId: string }>();
    const navigate = useNavigate();
    const { t } = useI18n();
    const [loading, setLoading] = useState(true);
    const [logs, setLogs] = useState<FinancialAuditLog[]>([]);
    const [integrity, setIntegrity] = useState<{ isValid: boolean } | null>(null);

    useEffect(() => {
        if (tripId) {
            const id = parseInt(tripId);
            Promise.all([
                api.getFinancialAuditLogs(id),
                api.checkFinancialAuditIntegrity(id)
            ]).then(([l, i]) => {
                setLogs(l);
                setIntegrity(i);
                setLoading(false);
            }).catch(() => setLoading(false));
        }
    }, [tripId]);

    if (loading) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '100px' }}>
                <Spin size="large" />
            </div>
        );
    }

    return (
        <div className="zen-audit-container">
            <header className="zen-audit-header">
                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                    <Button 
                        type="text" 
                        icon={<ArrowLeftOutlined />} 
                        onClick={() => navigate(-1)} 
                        className="zen-back-button"
                    />
                    <Title level={2} className="zen-audit-title serif-font">
                        {t('zen_audit.scroll_title')}
                    </Title>
                    {integrity?.isValid ? (
                        <Tag color="success" icon={<CheckCircleOutlined />} className="zen-status-tag">
                            {t('zen_audit.integrity_verified')}
                        </Tag>
                    ) : (
                        <Tag color="error" icon={<WarningOutlined />} className="zen-status-tag">
                            {t('zen_audit.broken')}
                        </Tag>
                    )}
                </Space>
            </header>

            <main className="zen-audit-scroll">
                {logs.length === 0 ? (
                    <Result title="No history found" />
                ) : (
                    logs.map((log) => (
                        <Card key={log.id} className="zen-audit-pill-card">
                            <div className="zen-audit-card-inner">
                                <header>
                                    <Space direction="vertical" size={2}>
                                        <Text type="secondary" style={{ fontSize: '10px' }}>
                                            {new Date(log.createdAt).toLocaleString()}
                                        </Text>
                                        <Text strong className="serif-font" style={{ fontSize: '16px' }}>
                                            {log.actionType.replace(/_/g, ' ')}
                                        </Text>
                                    </Space>
                                    <Tag className="zen-hash-tag">
                                        <AuditOutlined style={{ marginRight: 4 }} />
                                        {log.currentHash.substring(0, 8)}...
                                    </Tag>
                                </header>
                                <div className="zen-audit-values">
                                    {log.originalValue && (
                                        <div className="zen-val-block">
                                            <Text type="secondary" delete>{log.originalValue}</Text>
                                        </div>
                                    )}
                                    <div className="zen-val-block">
                                        <Text>{log.modifiedValue}</Text>
                                    </div>
                                </div>
                                <footer>
                                    <Text type="secondary" italic style={{ fontSize: '11px' }}>
                                        by {log.operatorName}
                                    </Text>
                                </footer>
                            </div>
                        </Card>
                    ))
                )}
            </main>

            <style>{`
                .zen-audit-container {
                    max-width: 600px;
                    margin: 0 auto;
                    background: var(--zen-bg);
                    min-height: 100vh;
                    padding: 2rem 1rem;
                }
                .zen-audit-header {
                    margin-bottom: 2.5rem;
                    text-align: center;
                }
                .zen-audit-title {
                    font-weight: 300 !important;
                    letter-spacing: 0.15em;
                    margin-bottom: 0.5rem !important;
                }
                .serif-font {
                    font-family: 'Playfair Display', "Noto Serif SC", serif;
                }
                .zen-status-tag {
                    border: none;
                    background: transparent;
                    font-size: 13px;
                }
                .zen-audit-scroll {
                    display: flex;
                    flex-direction: column;
                    gap: 1.5rem;
                }
                .zen-audit-pill-card {
                    background: #fffafa !important; /* Washi white */
                    border: none !important;
                    box-shadow: 0 4px 20px rgba(0,0,0,0.03);
                    border-left: 2px solid #2d2926 !important;
                    border-radius: 2px !important;
                }
                .zen-audit-card-inner {
                    display: flex;
                    flex-direction: column;
                    gap: 0.8rem;
                }
                .zen-audit-card-inner header {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                }
                .zen-hash-tag {
                    font-family: monospace;
                    font-size: 10px;
                    background: #f1efe8;
                    border: none;
                }
                .zen-audit-values {
                    padding: 0.5rem 0;
                    border-top: 1px dotted #e0e0e0;
                    border-bottom: 1px dotted #e0e0e0;
                }
                .zen-back-button {
                    position: absolute;
                    left: 0;
                    top: 0;
                }
            `}</style>
        </div>
    );
}

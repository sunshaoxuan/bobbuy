import React from 'react';
import { Table, Tag, Typography } from 'antd';
import { useI18n } from '../i18n';

const { Text } = Typography;

type AttributeDiff = {
  field: string;
  label: string;
  oldValue?: string;
  newValue?: string;
  different: boolean;
  identityField: boolean;
};

interface AttributeDiffTableProps {
  diffs?: AttributeDiff[];
}

const renderEmpty = (value?: string) => value?.trim() || '—';

const AttributeDiffTable: React.FC<AttributeDiffTableProps> = ({ diffs = [] }) => {
  const { t } = useI18n();

  if (!diffs.length) {
    return null;
  }

  return (
    <Table<AttributeDiff>
      rowKey="field"
      dataSource={diffs}
      pagination={false}
      size="small"
      data-testid="attribute-diff-table"
      columns={[
        {
          title: t('stock.ai_quick_add.field_column'),
          dataIndex: 'label',
          render: (label: string, record) => (
            <>
              <Text strong={record.identityField}>{label}</Text>
              {record.identityField ? <Tag color="gold" style={{ marginInlineStart: 8 }}>{t('stock.ai_quick_add.identity_field')}</Tag> : null}
            </>
          )
        },
        {
          title: t('stock.ai_quick_add.comparison_result'),
          key: 'diff',
          render: (_, record) => {
            if (!record.different) {
              return <Text type="secondary">{renderEmpty(record.newValue)}</Text>;
            }
            return (
              <>
                <Text style={{ background: '#fffbe6', fontWeight: 700, paddingInline: 6 }}>
                  {renderEmpty(record.newValue)}
                </Text>
                <Text type="secondary" style={{ marginInlineStart: 8 }}>
                  ({t('stock.ai_quick_add.original_value')}: {renderEmpty(record.oldValue)})
                </Text>
              </>
            );
          }
        }
      ]}
    />
  );
};

export default AttributeDiffTable;

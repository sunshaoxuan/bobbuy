import React from 'react';
import { Table, Tag, Typography } from 'antd';

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
          title: '字段',
          dataIndex: 'label',
          render: (label: string, record) => (
            <>
              <Text strong={record.identityField}>{label}</Text>
              {record.identityField ? <Tag color="gold" style={{ marginInlineStart: 8 }}>身份标识</Tag> : null}
            </>
          )
        },
        {
          title: '对比结果',
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
                  (原值: {renderEmpty(record.oldValue)})
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

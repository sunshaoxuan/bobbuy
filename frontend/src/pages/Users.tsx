import { Button, Card, Form, Input, Rate, Select, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, User } from '../api';
import { useI18n } from '../i18n';

const { Text } = Typography;

const roleOptions = ['CUSTOMER', 'AGENT', 'MERCHANT'];

export default function Users() {
  const { t } = useI18n();
  const [users, setUsers] = useState<User[]>([]);

  const columns: ColumnsType<User> = [
    { title: t('users.table.name'), dataIndex: 'name' },
    {
      title: t('users.table.role'),
      dataIndex: 'role',
      render: (role: User['role']) => <Tag color="purple">{role}</Tag>
    },
    {
      title: t('users.table.rating'),
      dataIndex: 'rating',
      render: (rating: User['rating']) => rating.toFixed(1)
    }
  ];

  useEffect(() => {
    api.users().then(setUsers);
  }, []);

  return (
    <div className="page-card">
      <div className="section-title">{t('users.title')}</div>
      <Text className="helper-text">{t('users.helper')}</Text>

      <Card style={{ marginTop: 16, marginBottom: 24 }} bordered={false}>
        <Form layout="vertical">
          <Form.Item label={t('users.form.name.label')} required>
            <Input placeholder={t('users.form.name.placeholder')} />
          </Form.Item>
          <Form.Item label={t('users.form.role.label')}>
            <Select
              options={roleOptions.map((role) => ({ value: role }))}
              placeholder={t('users.form.role.placeholder')}
            />
          </Form.Item>
          <Form.Item label={t('users.form.rating.label')}>
            <Rate allowHalf defaultValue={4.5} />
          </Form.Item>
          <Button type="primary">{t('users.form.submit')}</Button>
        </Form>
      </Card>

      <Card bordered={false} title={t('users.list.title')}>
        <Table<User>
          dataSource={users}
          rowKey="id"
          columns={columns}
        />
      </Card>
    </div>
  );
}

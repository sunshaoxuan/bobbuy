import { Button, Card, Form, Input, InputNumber, Select, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, type User } from '../api';
import { useI18n } from '../i18n';

const { Text } = Typography;
const roleOptions = ['CUSTOMER', 'AGENT', 'MERCHANT'] as const;

type UserFormValues = Omit<User, 'id'>;

const emptyProfile: UserFormValues = {
  name: '',
  role: 'CUSTOMER',
  rating: 0,
  phone: '',
  email: '',
  note: '',
  defaultAddress: {
    contactName: '',
    phone: '',
    countryRegion: '',
    city: '',
    addressLine: '',
    postalCode: '',
    latitude: undefined,
    longitude: undefined
  },
  socialAccounts: []
};

export default function Users() {
  const { t } = useI18n();
  const [form] = Form.useForm<UserFormValues>();
  const [users, setUsers] = useState<User[]>([]);
  const [editingUserId, setEditingUserId] = useState<number>();

  const loadUsers = async () => {
    setUsers(await api.users());
  };

  useEffect(() => {
    void loadUsers();
    form.setFieldsValue(emptyProfile);
  }, [form]);

  const onEdit = (user: User) => {
    setEditingUserId(user.id);
    form.setFieldsValue({
      ...emptyProfile,
      ...user,
      socialAccounts: user.socialAccounts ?? [],
      defaultAddress: {
        ...emptyProfile.defaultAddress,
        ...user.defaultAddress
      }
    });
  };

  const resetForm = () => {
    setEditingUserId(undefined);
    form.resetFields();
    form.setFieldsValue(emptyProfile);
  };

  const onFinish = async (values: UserFormValues) => {
    const payload: UserFormValues = {
      ...values,
      rating: Number(values.rating ?? 0),
      socialAccounts: values.socialAccounts ?? [],
      defaultAddress: values.defaultAddress ?? emptyProfile.defaultAddress
    };
    if (editingUserId) {
      await api.updateUser(editingUserId, payload);
    } else {
      await api.createUser(payload);
    }
    message.success(t('users.form.saved'));
    resetForm();
    await loadUsers();
  };

  const columns: ColumnsType<User> = [
    { title: t('users.table.name'), dataIndex: 'name' },
    {
      title: t('users.table.role'),
      dataIndex: 'role',
      render: (role: User['role']) => <Tag color="purple">{t(`enum.role.${role}`)}</Tag>
    },
    { title: t('users.table.phone'), dataIndex: 'phone', render: (value?: string) => value || '-' },
    { title: t('users.table.email'), dataIndex: 'email', render: (value?: string) => value || '-' },
    {
      title: t('users.table.social_accounts'),
      key: 'socialAccounts',
      render: (_, user) => (user.socialAccounts ?? []).map((account) => account.platform).filter(Boolean).join(', ') || '-'
    },
    {
      title: t('common.actions') || 'Actions',
      key: 'actions',
      render: (_, user) => (
        <Button size="small" onClick={() => onEdit(user)}>
          {t('common.edit') || 'Edit'}
        </Button>
      )
    }
  ];

  return (
    <div className="page-card">
      <div className="section-title" data-testid="users-title">{t('users.title')}</div>
      <Text className="helper-text">{t('users.helper')}</Text>

      <Card style={{ marginTop: 16, marginBottom: 24 }} bordered={false}>
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text type="secondary">{t('users.social.notice')}</Text>
          <Form form={form} layout="vertical" onFinish={onFinish}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space wrap style={{ width: '100%' }}>
                <Form.Item label={t('users.form.name.label')} name="name" rules={[{ required: true }]}>
                  <Input placeholder={t('users.form.name.placeholder')} style={{ minWidth: 220 }} />
                </Form.Item>
                <Form.Item label={t('users.form.role.label')} name="role">
                  <Select
                    style={{ minWidth: 160 }}
                    options={roleOptions.map((role) => ({ value: role, label: t(`enum.role.${role}`) }))}
                    placeholder={t('users.form.role.placeholder')}
                  />
                </Form.Item>
                <Form.Item label={t('users.form.rating.label')} name="rating">
                  <InputNumber min={0} max={5} step={0.1} style={{ minWidth: 140 }} />
                </Form.Item>
              </Space>
              <Space wrap style={{ width: '100%' }}>
                <Form.Item label={t('users.form.phone.label')} name="phone">
                  <Input placeholder={t('users.form.phone.placeholder')} style={{ minWidth: 220 }} />
                </Form.Item>
                <Form.Item label={t('users.form.email.label')} name="email">
                  <Input placeholder={t('users.form.email.placeholder')} style={{ minWidth: 220 }} />
                </Form.Item>
              </Space>
              <Form.Item label={t('users.form.note.label')} name="note">
                <Input.TextArea rows={3} placeholder={t('users.form.note.placeholder')} />
              </Form.Item>

              <Card size="small" title={t('users.address.title')}>
                <Space wrap style={{ width: '100%' }}>
                  <Form.Item label={t('users.address.contact_name')} name={['defaultAddress', 'contactName']}>
                    <Input style={{ minWidth: 180 }} />
                  </Form.Item>
                  <Form.Item label={t('users.address.phone')} name={['defaultAddress', 'phone']}>
                    <Input style={{ minWidth: 180 }} />
                  </Form.Item>
                  <Form.Item label={t('users.address.country_region')} name={['defaultAddress', 'countryRegion']}>
                    <Input style={{ minWidth: 180 }} />
                  </Form.Item>
                  <Form.Item label={t('users.address.city')} name={['defaultAddress', 'city']}>
                    <Input style={{ minWidth: 160 }} />
                  </Form.Item>
                  <Form.Item label={t('users.address.postal_code')} name={['defaultAddress', 'postalCode']}>
                    <Input style={{ minWidth: 140 }} />
                  </Form.Item>
                  <Form.Item label={t('users.address.latitude')} name={['defaultAddress', 'latitude']}>
                    <InputNumber style={{ minWidth: 140 }} />
                  </Form.Item>
                  <Form.Item label={t('users.address.longitude')} name={['defaultAddress', 'longitude']}>
                    <InputNumber style={{ minWidth: 140 }} />
                  </Form.Item>
                </Space>
                <Form.Item label={t('users.address.address_line')} name={['defaultAddress', 'addressLine']}>
                  <Input />
                </Form.Item>
              </Card>

              <Card size="small" title={t('users.social.title')}>
                <Form.List name="socialAccounts">
                  {(fields, { add, remove }) => (
                    <Space direction="vertical" style={{ width: '100%' }}>
                      {fields.map((field) => (
                        <Card
                          key={field.key}
                          size="small"
                          extra={
                            <Button type="link" danger onClick={() => remove(field.name)}>
                              {t('users.social.remove')}
                            </Button>
                          }
                        >
                          <Space wrap style={{ width: '100%' }}>
                            <Form.Item label={t('users.social.platform')} name={[field.name, 'platform']}>
                              <Input style={{ minWidth: 140 }} />
                            </Form.Item>
                            <Form.Item label={t('users.social.handle')} name={[field.name, 'handle']}>
                              <Input style={{ minWidth: 160 }} />
                            </Form.Item>
                            <Form.Item label={t('users.social.display_name')} name={[field.name, 'displayName']}>
                              <Input style={{ minWidth: 160 }} />
                            </Form.Item>
                            <Form.Item label={t('users.social.note')} name={[field.name, 'note']}>
                              <Input style={{ minWidth: 180 }} />
                            </Form.Item>
                          </Space>
                        </Card>
                      ))}
                      <Button onClick={() => add({ verified: false })}>{t('users.social.add')}</Button>
                    </Space>
                  )}
                </Form.List>
              </Card>

              <Space wrap>
                <Button type="primary" htmlType="submit" data-testid="users-submit">
                  {editingUserId ? t('users.form.update') : t('users.form.submit')}
                </Button>
                <Button onClick={resetForm}>{t('common.reset') || 'Reset'}</Button>
              </Space>
            </Space>
          </Form>
        </Space>
      </Card>

      <Card bordered={false} title={t('users.list.title')}>
        <Table<User>
          dataSource={users}
          rowKey="id"
          columns={columns}
          scroll={{ x: 'max-content' }}
        />
      </Card>
    </div>
  );
}

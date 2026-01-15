import { Button, Card, Form, Input, InputNumber, Select, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, Trip } from '../api';
import { useI18n } from '../i18n';

const { Text } = Typography;

const statusOptions = ['DRAFT', 'PUBLISHED', 'IN_PROGRESS', 'COMPLETED'];

export default function Trips() {
  const { t } = useI18n();
  const [trips, setTrips] = useState<Trip[]>([]);
  const [form] = Form.useForm<Omit<Trip, 'id' | 'statusUpdatedAt' | 'remainingCapacity'>>();

  const columns: ColumnsType<Trip> = [
    { title: t('trips.table.origin'), dataIndex: 'origin' },
    { title: t('trips.table.destination'), dataIndex: 'destination' },
    { title: t('trips.table.depart_date'), dataIndex: 'departDate' },
    { title: t('trips.table.capacity'), dataIndex: 'capacity' },
    { title: t('trips.table.remaining_capacity'), dataIndex: 'remainingCapacity' },
    {
      title: t('trips.table.status'),
      dataIndex: 'status',
      render: (status: Trip['status']) => <Tag color="blue">{status}</Tag>
    }
  ];

  useEffect(() => {
    api.trips().then(setTrips);
  }, []);

  const handleSubmit = async (values: Omit<Trip, 'id' | 'statusUpdatedAt' | 'remainingCapacity'>) => {
    try {
      await api.createTrip(values);
      message.success(t('trips.form.success'));
      form.resetFields();
      const refreshed = await api.trips();
      setTrips(refreshed);
    } catch {
      // Errors are surfaced in the API layer.
    }
  };

  return (
    <div className="page-card">
      <div className="section-title">{t('trips.title')}</div>
      <Text className="helper-text">{t('trips.helper')}</Text>

      <Card style={{ marginTop: 16, marginBottom: 24 }} bordered={false}>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          initialValues={{ status: 'DRAFT', capacity: 1, reservedCapacity: 0 }}
        >
          <Form.Item
            label={t('trips.form.agent_id.label')}
            name="agentId"
            rules={[{ required: true, message: t('trips.form.agent_id.required') }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('trips.form.agent_id.placeholder')} />
          </Form.Item>
          <Form.Item
            label={t('trips.form.origin.label')}
            name="origin"
            rules={[{ required: true, message: t('trips.form.origin.required') }]}
          >
            <Input placeholder={t('trips.form.origin.placeholder')} />
          </Form.Item>
          <Form.Item
            label={t('trips.form.destination.label')}
            name="destination"
            rules={[{ required: true, message: t('trips.form.destination.required') }]}
          >
            <Input placeholder={t('trips.form.destination.placeholder')} />
          </Form.Item>
          <Form.Item
            label={t('trips.form.depart_date.label')}
            name="departDate"
            rules={[{ required: true, message: t('trips.form.depart_date.required') }]}
          >
            <Input type="date" />
          </Form.Item>
          <Form.Item
            label={t('trips.form.capacity.label')}
            name="capacity"
            rules={[{ required: true, message: t('trips.form.capacity.required') }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder={t('trips.form.capacity.placeholder')} />
          </Form.Item>
          <Form.Item
            label={t('trips.form.status.label')}
            name="status"
            rules={[{ required: true, message: t('trips.form.status.required') }]}
          >
            <Select options={statusOptions.map((status) => ({ value: status }))} placeholder={t('trips.form.status.placeholder')} />
          </Form.Item>
          <Button type="primary" htmlType="submit">{t('trips.form.submit')}</Button>
        </Form>
      </Card>

      <Card bordered={false} title={t('trips.list.title')}>
        <Table<Trip>
          dataSource={trips}
          rowKey="id"
          columns={columns}
        />
      </Card>
    </div>
  );
}

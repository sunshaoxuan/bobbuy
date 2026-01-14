import { Button, Card, Form, Input, Rate, Select, Table, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useEffect, useState } from 'react';
import { api, User } from '../api';

const { Text } = Typography;

const roleOptions = ['CUSTOMER', 'AGENT', 'MERCHANT'];

export default function Users() {
  const [users, setUsers] = useState<User[]>([]);

  const columns: ColumnsType<User> = [
    { title: '姓名', dataIndex: 'name' },
    {
      title: '角色',
      dataIndex: 'role',
      render: (role: User['role']) => <Tag color="purple">{role}</Tag>
    },
    {
      title: '评分',
      dataIndex: 'rating',
      render: (rating: User['rating']) => rating.toFixed(1)
    }
  ];

  useEffect(() => {
    api.users().then(setUsers);
  }, []);

  return (
    <div className="page-card">
      <div className="section-title">参与者管理与身份认证</div>
      <Text className="helper-text">维护客户、代理人与商户的基础信息与信用评分。</Text>

      <Card style={{ marginTop: 16, marginBottom: 24 }} bordered={false}>
        <Form layout="vertical">
          <Form.Item label="姓名" required>
            <Input placeholder="填写姓名" />
          </Form.Item>
          <Form.Item label="角色">
            <Select options={roleOptions.map((role) => ({ value: role }))} placeholder="选择角色" />
          </Form.Item>
          <Form.Item label="评分">
            <Rate allowHalf defaultValue={4.5} />
          </Form.Item>
          <Button type="primary">保存参与者</Button>
        </Form>
      </Card>

      <Card bordered={false} title="参与者列表">
        <Table<User>
          dataSource={users}
          rowKey="id"
          columns={columns}
        />
      </Card>
    </div>
  );
}

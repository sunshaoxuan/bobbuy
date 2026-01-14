import { Layout, Menu, Typography } from 'antd';
import { NavLink, Route, Routes, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Trips from './pages/Trips';
import Orders from './pages/Orders';
import Users from './pages/Users';

const { Header, Content, Sider } = Layout;
const { Title, Text } = Typography;

const menuItems = [
  { key: '/', label: <NavLink to="/">概览</NavLink> },
  { key: '/trips', label: <NavLink to="/trips">行程管理</NavLink> },
  { key: '/orders', label: <NavLink to="/orders">订单管理</NavLink> },
  { key: '/users', label: <NavLink to="/users">参与者</NavLink> }
];

export default function App() {
  const location = useLocation();
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="light">
        <div style={{ padding: 24 }}>
          <Title level={4} style={{ margin: 0 }}>
            BOBBuy
          </Title>
          <Text type="secondary">全球代购指挥台</Text>
        </div>
        <Menu mode="inline" selectedKeys={[location.pathname]} items={menuItems} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px' }}>
          <Title level={5} style={{ margin: 0 }}>
            产品第一版 · MVP Console
          </Title>
        </Header>
        <Content style={{ padding: 24 }}>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/trips" element={<Trips />} />
            <Route path="/orders" element={<Orders />} />
            <Route path="/users" element={<Users />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}

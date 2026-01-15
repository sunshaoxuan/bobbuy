import { Layout, Menu, Select, Space, Typography } from 'antd';
import { NavLink, Route, Routes, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Trips from './pages/Trips';
import Orders from './pages/Orders';
import Users from './pages/Users';
import type { Locale } from './i18n';
import { supportedLocales, useI18n } from './i18n';

const { Header, Content, Sider } = Layout;
const { Title, Text } = Typography;

export default function App() {
  const location = useLocation();
  const { locale, setLocale, t } = useI18n();
  const menuItems = [
    { key: '/', label: <NavLink to="/">{t('nav.dashboard')}</NavLink> },
    { key: '/trips', label: <NavLink to="/trips">{t('nav.trips')}</NavLink> },
    { key: '/orders', label: <NavLink to="/orders">{t('nav.orders')}</NavLink> },
    { key: '/users', label: <NavLink to="/users">{t('nav.users')}</NavLink> }
  ];
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} theme="light">
        <div style={{ padding: 24 }}>
          <Title level={4} style={{ margin: 0 }}>
            BOBBuy
          </Title>
          <Text type="secondary">{t('app.brand_subtitle')}</Text>
        </div>
        <Menu mode="inline" selectedKeys={[location.pathname]} items={menuItems} />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Title level={5} style={{ margin: 0 }}>
              {t('app.header_title')}
            </Title>
            <Space>
              <Text type="secondary">{t('language.label')}</Text>
              <Select
                popupMatchSelectWidth={false}
                size="small"
                value={locale}
                onChange={(value) => setLocale(value as Locale)}
                options={supportedLocales.map((value) => ({
                  value,
                  label: value === 'zh-CN' ? t('language.zh') : t('language.en')
                }))}
              />
            </Space>
          </div>
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

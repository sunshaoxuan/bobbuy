import { Layout, Menu, Select, Space, Typography } from 'antd';
import { NavLink, Route, Routes, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Trips from './pages/Trips';
import Orders from './pages/Orders';
import Users from './pages/Users';
import OrderDesk from './pages/OrderDesk';
import ProcurementHUD from './pages/ProcurementHUD';
import PickingMaster from './pages/PickingMaster';
import StockMaster from './pages/StockMaster';
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
    { key: '/order-desk', label: <NavLink to="/order-desk">{t('nav.order_desk')}</NavLink> },
    { key: '/procurement', label: <NavLink to="/procurement">{t('nav.procurement')}</NavLink> },
    { key: '/picking', label: <NavLink to="/picking">{t('nav.picking')}</NavLink> },
    { key: '/stock-master', label: <NavLink to="/stock-master">{t('nav.stock_master')}</NavLink> },
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
            <Route path="/order-desk" element={<OrderDesk />} />
            <Route path="/procurement" element={<ProcurementHUD />} />
            <Route path="/picking" element={<PickingMaster />} />
            <Route path="/stock-master" element={<StockMaster />} />
            <Route path="/users" element={<Users />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}

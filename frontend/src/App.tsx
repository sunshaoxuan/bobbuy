import { useMemo, useState } from 'react';
import { Button, Drawer, Grid, Layout, Menu, Select, Space, Typography } from 'antd';
import { AppstoreOutlined, BarsOutlined, OrderedListOutlined, QrcodeOutlined, ShoppingOutlined } from '@ant-design/icons';
import { NavLink, Route, Routes, useLocation } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Trips from './pages/Trips';
import Orders from './pages/Orders';
import Users from './pages/Users';
import OrderDesk from './pages/OrderDesk';
import ProcurementHUD from './pages/ProcurementHUD';
import PickingMaster from './pages/PickingMaster';
import StockMaster from './pages/StockMaster';
import ClientHomeV2 from './pages/ClientHomeV2';
import ZenAuditView from './pages/ZenAuditView';
import type { Locale } from './i18n';
import { supportedLocales, useI18n } from './i18n';

const { Header, Content, Sider } = Layout;
const { Title, Text } = Typography;
const DESKTOP_SIDER_EXPANDED_WIDTH = '14.5rem';

export default function App() {
  const location = useLocation();
  const { locale, setLocale, t } = useI18n();
  const screens = Grid.useBreakpoint();
  const isMobile = screens.md === false;
  const isTablet = screens.md && !screens.lg;
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [desktopCollapsed, setDesktopCollapsed] = useState(false);
  const contentPadding = isMobile ? '0.5rem' : isTablet ? '1rem' : '1.5rem';

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

  const mobileQuickNavItems = useMemo(
    () => [
      { key: '/trips', icon: <ShoppingOutlined />, label: t('nav.trips') },
      { key: '/orders', icon: <OrderedListOutlined />, label: t('nav.orders') },
      { key: '/procurement', icon: <QrcodeOutlined />, label: t('nav.scan') },
      { key: '/stock-master', icon: <AppstoreOutlined />, label: t('nav.stock_master') }
    ],
    [t]
  );

  const pageTitles: Record<string, string> = useMemo(
    () => ({
      '/': t('nav.dashboard'),
      '/dashboard': t('nav.dashboard'),
      '/trips': t('nav.trips'),
      '/orders': t('nav.orders'),
      '/order-desk': t('nav.order_desk'),
      '/procurement': t('nav.procurement'),
      '/picking': t('nav.picking'),
      '/stock-master': t('nav.stock_master'),
      '/users': t('nav.users')
    }),
    [t]
  );

  const pageLabel = useMemo(() => {
    return pageTitles[location.pathname] ?? t('app.header_title');
  }, [location.pathname, pageTitles, t]);

  return (
    <Layout className="app-shell">
      {!isMobile ? (
        <Sider
          collapsible
          collapsed={desktopCollapsed}
          onCollapse={setDesktopCollapsed}
          theme="light"
          className="app-sider app-shadow-high"
          width={desktopCollapsed ? undefined : DESKTOP_SIDER_EXPANDED_WIDTH}
        >
          <div className="app-brand-block">
            <Title level={4} style={{ margin: 0 }}>
              BOBBuy
            </Title>
            {!desktopCollapsed ? <Text type="secondary">{t('app.brand_subtitle')}</Text> : null}
          </div>
          <Menu mode="inline" selectedKeys={[location.pathname]} items={menuItems} />
        </Sider>
      ) : null}
      <Layout>
        <Header className={isMobile ? 'app-header-mobile app-shadow-medium' : 'app-header-desktop'}>
          <div className="app-header-inner">
            <Space>
              {isMobile ? (
                <Button
                  type="text"
                  icon={<BarsOutlined />}
                  aria-label={t('app.open_menu')}
                  onClick={() => setMobileMenuOpen(true)}
                />
              ) : null}
              <Title level={5} style={{ margin: 0 }} ellipsis>
                {isMobile ? pageLabel : t('app.header_title')}
              </Title>
            </Space>
            <Space size="small">
              {!isMobile ? <Text type="secondary">{t('language.label')}</Text> : null}
              <Select
                popupMatchSelectWidth={false}
                size="small"
                value={locale}
                onChange={(value) => setLocale(value as Locale)}
                options={supportedLocales.map((value) => ({
                  value,
                  label:
                    value === 'zh-CN'
                      ? t('language.zh')
                      : value === 'ja-JP'
                      ? t('language.ja')
                      : t('language.en')
                }))}
              />
            </Space>
          </div>
        </Header>
        <Content style={{ padding: contentPadding }} className="app-content">
          <div className="app-page-transition">
            <Routes>
              <Route path="/" element={<ClientHomeV2 />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/trips" element={<Trips />} />
              <Route path="/orders" element={<Orders />} />
              <Route path="/order-desk" element={<OrderDesk />} />
              <Route path="/procurement" element={<ProcurementHUD />} />
              <Route path="/picking" element={<PickingMaster />} />
              <Route path="/stock-master" element={<StockMaster />} />
              <Route path="/users" element={<Users />} />
              <Route path="/audit/:tripId" element={<ZenAuditView />} />
            </Routes>
          </div>
        </Content>
      </Layout>

      {isMobile ? (
        <>
          <Drawer
            placement="left"
            onClose={() => setMobileMenuOpen(false)}
            open={mobileMenuOpen}
            styles={{ body: { padding: '1rem 0' } }}
            className="app-mobile-drawer"
          >
            <div className="app-brand-block">
              <Title level={4} style={{ margin: 0 }}>
                BOBBuy
              </Title>
              <Text type="secondary">{t('app.brand_subtitle')}</Text>
            </div>
            <Menu
              mode="inline"
              selectedKeys={[location.pathname]}
              items={menuItems}
              onClick={() => setMobileMenuOpen(false)}
            />
          </Drawer>
          <nav className="mobile-bottom-nav app-shadow-high" aria-label={t('app.bottom_nav')}>
            {mobileQuickNavItems.map((item) => (
              <NavLink
                key={item.key}
                to={item.key}
                className={({ isActive }) => `mobile-bottom-nav-item${isActive ? ' active' : ''}`}
              >
                <Space direction="vertical" size={4} align="center">
                  {item.icon}
                  <Text>{item.label}</Text>
                </Space>
              </NavLink>
            ))}
          </nav>
        </>
      ) : null}
    </Layout>
  );
}

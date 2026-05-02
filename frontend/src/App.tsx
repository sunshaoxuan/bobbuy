import { Suspense, lazy, useMemo, useState } from 'react';
import { Button, Drawer, Grid, Layout, Menu, Select, Space, Typography } from 'antd';
import { AppstoreOutlined, BarsOutlined, OrderedListOutlined, QrcodeOutlined, ShoppingOutlined } from '@ant-design/icons';
import { NavLink, Route, Routes, useLocation, Navigate } from 'react-router-dom';
import type { Locale } from './i18n';
import { supportedLocales, useI18n } from './i18n';
import { useUserRole } from './context/UserRoleContext';
import ProtectedRoute from './components/ProtectedRoute';

const { Header, Content, Sider } = Layout;
const { Title, Text } = Typography;
const DESKTOP_SIDER_EXPANDED_WIDTH = '14.5rem';
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Trips = lazy(() => import('./pages/Trips'));
const Users = lazy(() => import('./pages/Users'));
const Suppliers = lazy(() => import('./pages/Suppliers'));
const Orders = lazy(() => import('./pages/Orders'));
const OrderDesk = lazy(() => import('./pages/OrderDesk'));
const ProcurementHUD = lazy(() => import('./pages/ProcurementHUD'));
const PickingMaster = lazy(() => import('./pages/PickingMaster'));
const StockMaster = lazy(() => import('./pages/StockMaster'));
const ClientHomeV2 = lazy(() => import('./pages/ClientHomeV2'));
const ZenAuditView = lazy(() => import('./pages/ZenAuditView'));
const ClientOrders = lazy(() => import('./pages/ClientOrders'));
const ClientBilling = lazy(() => import('./pages/ClientBilling'));
const ClientChat = lazy(() => import('./pages/ClientChat'));
const LoginPage = lazy(() => import('./pages/LoginPage'));

export default function App() {
  const location = useLocation();
  const { locale, setLocale, t } = useI18n();
  const { role, isPurchaser, isAuthenticated, loading, user, logout } = useUserRole();
  const isAgentRole = role === 'AGENT';
  const screens = Grid.useBreakpoint();
  const isMobile = screens.md !== true;
  const isTablet = screens.md && !screens.lg;
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [desktopCollapsed, setDesktopCollapsed] = useState(false);
  const contentPadding = isMobile ? '0.5rem' : isTablet ? '1rem' : '1.5rem';

  const menuItems = useMemo(() => {
    if (!isAuthenticated) {
      return [];
    }
    if (isPurchaser) {
      return [
        { key: '/dashboard', label: <NavLink to="/dashboard">{t('nav.dashboard')}</NavLink> },
        { key: '/trips', label: <NavLink to="/trips">{t('nav.trips')}</NavLink> },
        { key: '/orders', label: <NavLink to="/orders">{t('nav.orders')}</NavLink> },
        { key: '/procurement', label: <NavLink to="/procurement">{t('nav.procurement')}</NavLink> },
        { key: '/picking', label: <NavLink to="/picking">{t('nav.picking')}</NavLink> },
        { key: '/stock-master', label: <NavLink to="/stock-master">{t('nav.stock_master')}</NavLink> },
        { key: '/suppliers', label: <NavLink to="/suppliers">{t('nav.suppliers')}</NavLink> },
        { key: '/users', label: <NavLink to="/users">{t('nav.users')}</NavLink> }
      ];
    }
    // Customer Menu
    return [
      { key: '/', label: <NavLink to="/">{t('nav.discover')}</NavLink> },
      { key: '/client/orders', label: <NavLink to="/client/orders">{t('nav.client_orders')}</NavLink> },
      { key: '/client/billing', label: <NavLink to="/client/billing">{t('nav.client_billing')}</NavLink> },
      { key: '/client/chat', label: <NavLink to="/client/chat">{t('nav.client_chat')}</NavLink> }
    ];
  }, [isAuthenticated, isPurchaser, t]);

  const mobileQuickNavItems = useMemo(() => {
    if (!isAuthenticated) {
      return [];
    }
    if (isPurchaser) {
      return [
        { key: '/dashboard', icon: <AppstoreOutlined />, label: t('nav.dashboard') },
        { key: '/procurement', icon: <QrcodeOutlined />, label: t('nav.scan') },
        { key: '/picking', icon: <BarsOutlined />, label: t('nav.picking') }
      ];
    }
    return [
      { key: '/', icon: <ShoppingOutlined />, label: t('nav.discover') },
      { key: '/client/orders', icon: <OrderedListOutlined />, label: t('nav.client_orders') },
      { key: '/client/billing', icon: <AppstoreOutlined />, label: t('nav.client_billing') },
      { key: '/client/chat', icon: <QrcodeOutlined />, label: t('nav.client_chat') }
    ];
  }, [isAuthenticated, isPurchaser, t]);

  const pageTitles: Record<string, string> = useMemo(
    () => ({
      '/': t('nav.dashboard'),
      '/dashboard': t('nav.dashboard'),
      '/trips': t('nav.trips'),
      '/orders': t('nav.orders'),
      '/client/orders': t('nav.client_orders'),
      '/client/billing': t('nav.client_billing'),
      '/client/chat': t('nav.client_chat'),
      '/login': t('auth.login_title'),
      '/order-desk': t('nav.order_desk'),
      '/procurement': t('nav.procurement'),
      '/picking': t('nav.picking'),
      '/stock-master': t('nav.stock_master'),
      '/suppliers': t('nav.suppliers'),
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
              {isAuthenticated && !isMobile ? (
                <>
                  <Text type="secondary">{user?.name}</Text>
                  <Text type="secondary">{t(`enum.role.${role}`)}</Text>
                </>
              ) : (
                !isAuthenticated ? <Text type="secondary">{t('auth.login_title')}</Text> : null
              )}
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
              {isAuthenticated ? (
                <Button size="small" onClick={() => void logout()}>
                  {t('auth.logout')}
                </Button>
              ) : null}
            </Space>
          </div>
        </Header>
        <Content style={{ padding: contentPadding }} className="app-content">
          <div className="app-page-transition">
            <Suspense fallback={<div style={{ padding: '2rem 1rem' }}>{t('chat.loading')}</div>}>
              <Routes>
                <Route
                  path="/login"
                  element={
                    loading ? null : isAuthenticated ? (
                      <Navigate to={isAgentRole ? '/dashboard' : '/'} replace />
                    ) : (
                      <LoginPage />
                    )
                  }
                />
                <Route
                  path="/"
                  element={
                    !isAuthenticated ? (
                      <Navigate to="/login" replace />
                    ) : isPurchaser ? (
                      <Navigate to="/dashboard" replace />
                    ) : (
                      <ClientHomeV2 />
                    )
                  }
                />
                <Route
                  path="/orders"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <Orders />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/dashboard"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <Dashboard />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/trips"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <Trips />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/order-desk"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <OrderDesk />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/procurement"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <ProcurementHUD />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/picking"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <PickingMaster />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/stock-master"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <StockMaster />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/suppliers"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <Suppliers />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/users"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <Users />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/audit/:tripId"
                  element={
                    <ProtectedRoute allowedRoles={['AGENT']}>
                      <ZenAuditView />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/client/orders"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER']}>
                      <ClientOrders />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/client/billing"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER']}>
                      <ClientBilling />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/client/chat"
                  element={
                    <ProtectedRoute allowedRoles={['CUSTOMER']}>
                      <ClientChat />
                    </ProtectedRoute>
                  }
                />
                <Route path="*" element={<Navigate to={isAuthenticated ? (isAgentRole ? '/dashboard' : '/') : '/login'} replace />} />
              </Routes>
            </Suspense>
          </div>
        </Content>
      </Layout>

      {isMobile && isAuthenticated ? (
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

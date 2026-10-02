import { Suspense, useEffect } from 'react';
import { Layout, Button, Space, Spin } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAdminAuthStore } from '../stores/adminAuthStore';

const { Sider, Content } = Layout;

// 侧边栏导航项，path 同时用于 NavLink 与页面标题推导
const NAV_ITEMS: { path: string; label: string }[] = [
  { path: '/', label: '首页' },
  { path: '/users/audits', label: '用户审核' },
  { path: '/items/audits', label: '物品审核' },
  { path: '/bans', label: '封禁管理' },
  { path: '/disputes', label: '异议处理' },
  { path: '/logs', label: '操作日志' },
];

// 顶栏样式：白底 + 底部边框 + flex 两端对齐
const topbarStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: 16,
  padding: '12px 24px',
  backgroundColor: '#ffffff',
  borderBottom: '1px solid var(--admin-color-border)',
  flex: '0 0 auto',
};

// 管理员后台整体布局：固定侧边栏 + 顶栏 + 内容区（Outlet）
export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const accessToken = useAdminAuthStore((s) => s.accessToken);
  const username = useAdminAuthStore((s) => s.username);
  const logout = useAdminAuthStore((s) => s.logout);

  // 防御性 auth bootstrap：accessToken 存在但 username 丢失时强制登出。
  // 不调用 /api/auth/me，adminAuthStore 已从 localStorage 持久化 username。
  useEffect(() => {
    if (accessToken && !username) {
      logout();
      navigate('/login', { replace: true });
    }
  }, [accessToken, username, logout, navigate]);

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  // 当前页面标题：精确匹配路径，未匹配时回退到首页
  const currentNav =
    NAV_ITEMS.find((n) => n.path === location.pathname) ?? NAV_ITEMS[0];

  return (
    <Layout className="admin-shell">
      <Sider className="admin-sidebar" width={140} theme="light">
        <div className="admin-brand">
          <span className="admin-brand-mark">邻享</span>
          <span className="admin-brand-text">邻享后台</span>
        </div>
        <nav className="admin-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/'}
              className={({ isActive }) =>
                `admin-nav-item${isActive ? ' is-active' : ''}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </Sider>
      <Layout style={{ background: 'var(--admin-color-bg)' }}>
        <div style={topbarStyle}>
          <h1 className="admin-title">{currentNav.label}</h1>
          <Space>
            <span className="admin-text">{username ?? '管理员'}</span>
            <Button
              className="admin-btn-secondary"
              icon={<LogoutOutlined />}
              onClick={handleLogout}
            >
              退出登录
            </Button>
          </Space>
        </div>
        <Content className="admin-page">
          <Suspense
            fallback={
              <div className="admin-empty">
                <Spin />
              </div>
            }
          >
            <Outlet />
          </Suspense>
        </Content>
      </Layout>
    </Layout>
  );
}

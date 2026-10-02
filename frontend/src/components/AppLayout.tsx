import { useEffect } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  ClipboardList,
  Heart,
  LogOut,
  MessageCircle,
  Plus,
  Search,
  User,
  Wallet,
} from 'lucide-react';
import { authService } from '../services/auth';
import { useAuthStore } from '../stores/authStore';
import { useMessageStore } from '../stores/messageStore';
import { useWalletStore } from '../stores/walletStore';

const DESKTOP_NAV = [
  { to: '/items', label: '租赁' },
  { to: '/my-items', label: '我的物品' },
  { to: '/orders', label: '订单' },
  { to: '/messages', label: '消息' },
  { to: '/favorites', label: '收藏' },
  { to: '/profile', label: '我的' },
];

const BOTTOM_TABS = [
  { to: '/items', label: '租赁', Icon: Search },
  { to: '/orders', label: '订单', Icon: ClipboardList },
  { to: '/messages', label: '消息', Icon: MessageCircle },
  { to: '/favorites', label: '收藏', Icon: Heart },
  { to: '/profile', label: '我的', Icon: User },
];

export default function AppLayout() {
  const accessToken = useAuthStore((s) => s.accessToken);
  const currentUser = useAuthStore((s) => s.currentUser);
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);
  const fetchWallet = useWalletStore((s) => s.fetchWallet);
  const resetWallet = useWalletStore((s) => s.reset);
  const hasDebt = useWalletStore((s) => s.hasDebt());
  const unreadTotal = useMessageStore((s) => s.unreadTotal);
  const connectSocket = useMessageStore((s) => s.connectSocket);
  const disconnectSocket = useMessageStore((s) => s.disconnectSocket);
  const fetchUnreadCount = useMessageStore((s) => s.fetchUnreadCount);
  const resetMessages = useMessageStore((s) => s.reset);
  const navigate = useNavigate();

  const handleLogout = () => {
    useAuthStore.getState().logout();
    resetWallet();
    disconnectSocket();
    resetMessages();
    navigate('/items', { replace: true });
  };

  useEffect(() => {
    if (!accessToken || currentUser) return undefined;
    let cancelled = false;
    authService
      .me()
      .then((user) => {
        if (!cancelled) setCurrentUser(user);
      })
      .catch(() => {
        // 401 会由 http interceptor 清理登录态；这里避免布局层重复弹错。
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, currentUser, setCurrentUser]);

  useEffect(() => {
    if (accessToken) {
      fetchWallet();
      // WebSocket 在登录态常驻：在 /messages 与 /messages/:id 间切换不会断连；
      // 退出登录时由下面的 cleanup 断开。
      connectSocket(accessToken);
      fetchUnreadCount();
    } else {
      resetWallet();
      disconnectSocket();
      resetMessages();
    }
    return () => {
      // 组件卸载（路由切出 AppLayout）时断开 socket。
      disconnectSocket();
    };
  }, [accessToken, fetchWallet, resetWallet, connectSocket, disconnectSocket, fetchUnreadCount, resetMessages]);

  return (
    <>
      <header className="sr-topbar">
        <Link to="/items" className="sr-topbar-brand">邻享租借</Link>
        <nav className="sr-topbar-nav">
          {DESKTOP_NAV.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'active' : undefined)}
            >
              {item.label}
              {item.to === '/messages' && unreadTotal > 0 && (
                <span className="sr-nav-badge">{unreadTotal > 99 ? '99+' : unreadTotal}</span>
              )}
            </NavLink>
          ))}
        </nav>
        <div className="sr-topbar-actions">
          {accessToken ? (
            <>
              {hasDebt && (
                <>
                  <span className="sr-debt-badge">
                    <Wallet size={14} />
                    钱包欠费
                  </span>
                  <Link to="/wallet" className="sr-btn sr-btn-sm sr-btn-primary">
                    模拟充值
                  </Link>
                </>
              )}
              <Link to="/profile" className="sr-topbar-user" aria-label={`${currentUser?.username ?? '用户'} 的个人页`}>
                {currentUser?.avatarUrl ? (
                  <img src={currentUser.avatarUrl} alt="" className="sr-topbar-avatar" />
                ) : (
                  <span className="sr-topbar-avatar sr-topbar-avatar--placeholder">
                    {(currentUser?.username ?? '用').slice(0, 1)}
                  </span>
                )}
                <span>{currentUser?.username ?? '用户'}</span>
              </Link>
              <button
                type="button"
                className="sr-btn sr-btn-sm sr-btn-secondary sr-topbar-logout"
                onClick={handleLogout}
              >
                <LogOut size={14} />
                退出登录
              </button>
            </>
          ) : (
            <>
              <button
                type="button"
                className="sr-btn sr-btn-sm sr-btn-secondary"
                onClick={() => navigate('/login')}
              >
                登录
              </button>
              <button
                type="button"
                className="sr-btn sr-btn-sm sr-btn-primary"
                onClick={() => navigate('/register')}
              >
                注册
              </button>
            </>
          )}
        </div>
      </header>

      <main className="sr-content">
        <Outlet />
      </main>

      {accessToken && (
        <Link to="/items/create" className="sr-fab" aria-label="发布">
          <Plus size={24} />
        </Link>
      )}

      <nav className="sr-bottombar">
        {BOTTOM_TABS.map(({ to, label, Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `sr-bottombar-tab${isActive ? ' active' : ''}`
            }
          >
            <span className="sr-bottombar-tab-icon">
              <Icon size={20} />
              {to === '/messages' && unreadTotal > 0 && (
                <span className="sr-nav-badge">{unreadTotal > 99 ? '99+' : unreadTotal}</span>
              )}
            </span>
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </>
  );
}

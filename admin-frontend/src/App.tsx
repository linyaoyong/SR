import { lazy } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import AdminLayout from './components/AdminLayout';
import ProtectedAdminRoute from './components/ProtectedAdminRoute';
import AdminLoginPage from './pages/AdminLoginPage';

// 首屏外的页面按需加载，控制首包体积
const DashboardPage = lazy(() => import('./pages/DashboardPage'));
const UserAuditsPage = lazy(() => import('./pages/UserAuditsPage'));
const UserAuditDetailPage = lazy(() => import('./pages/UserAuditDetailPage'));
const ItemAuditsPage = lazy(() => import('./pages/ItemAuditsPage'));
const ItemAuditDetailPage = lazy(() => import('./pages/ItemAuditDetailPage'));
const BansPage = lazy(() => import('./pages/BansPage'));
const DisputesPage = lazy(() => import('./pages/DisputesPage'));
const LogsPage = lazy(() => import('./pages/LogsPage'));

// 管理员后台路由：登录页 + 受保护布局内嵌业务页面。
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<AdminLoginPage />} />
        <Route element={<ProtectedAdminRoute />}>
          <Route element={<AdminLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="/users/audits" element={<UserAuditsPage />} />
            <Route path="/users/audits/:id" element={<UserAuditDetailPage />} />
            <Route path="/items/audits" element={<ItemAuditsPage />} />
            <Route path="/items/audits/:id" element={<ItemAuditDetailPage />} />
            <Route path="/bans" element={<BansPage />} />
            <Route path="/disputes" element={<DisputesPage />} />
            <Route path="/logs" element={<LogsPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

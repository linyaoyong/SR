import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAdminAuthStore } from '../stores/adminAuthStore';

// 保护管理员路由：无 accessToken 时跳转登录页并记录来源路径
export default function ProtectedAdminRoute() {
  const accessToken = useAdminAuthStore((s) => s.accessToken);
  const location = useLocation();

  if (!accessToken) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}

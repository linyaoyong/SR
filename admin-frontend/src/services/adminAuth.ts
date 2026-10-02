import type { LoginResponse } from '../types/api';
import { http, requestData } from './http';

// 管理员认证服务，对接 POST /api/auth/admin/login
export const adminAuthService = {
  login: (body: { username: string; password: string }) =>
    requestData<LoginResponse>(http.post('/api/auth/admin/login', body)),
};

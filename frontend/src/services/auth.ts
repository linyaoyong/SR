import { http, requestData } from './http';
import type { LoginResponse, UserMeResponse } from '../types/api';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  confirmPassword?: string;
}

export const authService = {
  login: (body: LoginRequest) => requestData<LoginResponse>(http.post('/api/auth/login', body)),
  register: (body: RegisterRequest) => requestData<void>(http.post('/api/auth/register', body)),
  refresh: (refreshToken: string) =>
    requestData<LoginResponse>(
      http.post('/api/auth/refresh', null, { headers: { Authorization: `Bearer ${refreshToken}` } }),
    ),
  me: () => requestData<UserMeResponse>(http.get('/api/users/me')),
};

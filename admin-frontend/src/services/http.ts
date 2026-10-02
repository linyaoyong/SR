import axios, { AxiosError } from 'axios';
import type { ApiResponse } from '../types/api';
import { useAdminAuthStore } from '../stores/adminAuthStore';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15000,
});

// 请求拦截：自动携带管理员 accessToken
http.interceptors.request.use((config) => {
  const token = useAdminAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 响应拦截：401/403 触发登出，统一抛出业务错误信息
http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      useAdminAuthStore.getState().logout();
    }
    return Promise.reject(
      new Error(error.response?.data?.message || error.message || '请求失败'),
    );
  },
);

// 解包统一响应结构，code !== 0 视为业务失败
export async function requestData<T>(
  promise: Promise<{ data: ApiResponse<T> }>,
): Promise<T> {
  const response = await promise;
  if (response.data.code !== 0) throw new Error(response.data.message || '请求失败');
  return response.data.data;
}

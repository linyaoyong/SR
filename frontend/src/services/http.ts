import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types/api';
import { useAuthStore } from '../stores/authStore';

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

export function unwrapApiResponse<T>(body: ApiResponse<T>): T {
  if (body.code === 0) return body.data;
  throw new Error(body.message || '请求失败');
}

export function formatHttpError(error: unknown): string {
  const axiosError = error as AxiosError<ApiResponse<unknown>>;
  const status = axiosError.response?.status;
  const data = axiosError.response?.data;
  if (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string' && data.message) {
    return data.message;
  }
  if (status != null && status >= 500) {
    return '服务暂时不可用，请稍后再试';
  }
  if (axiosError.code === 'ERR_NETWORK' || axiosError.message === 'Network Error') {
    return '网络异常，请稍后再试';
  }
  if (axiosError.code === 'ECONNABORTED') {
    return '请求超时，请稍后再试';
  }
  return axiosError.message || '网络请求失败';
}

export const http = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
});

// 自定义 config 标记：_retry 防止重复重试，_skipAuthRefresh 让 refresh 接口本身不触发续期
interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  _skipAuthRefresh?: boolean;
}

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 并发 401 时共用同一个 refresh promise，避免重复刷新
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (refreshPromise) return refreshPromise;
  const { refreshToken, updateTokens } = useAuthStore.getState();
  if (!refreshToken) {
    throw new Error('no refresh token');
  }
  refreshPromise = (async () => {
    const { data } = await http.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
      '/api/auth/refresh',
      null,
      { headers: { Authorization: `Bearer ${refreshToken}` }, _skipAuthRefresh: true } as RetryableRequestConfig,
    );
    const session = unwrapApiResponse(data);
    updateTokens(session.accessToken, session.refreshToken);
    return session.accessToken;
  })();
  try {
    return await refreshPromise;
  } finally {
    refreshPromise = null;
  }
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const original = error.config as RetryableRequestConfig | undefined;
    const status = error.response?.status;

    // 401 且非 refresh 接口本身且未重试过：尝试用 refresh token 续期后重试原请求
    if (status === 401 && original && !original._skipAuthRefresh && !original._retry) {
      try {
        const newToken = await refreshAccessToken();
        original._retry = true;
        original.headers.Authorization = `Bearer ${newToken}`;
        return http(original);
      } catch {
        useAuthStore.getState().logout();
        return Promise.reject(new Error('登录已过期，请重新登录'));
      }
    }

    // refresh 接口本身 401，或重试后仍失败：清理登录态
    if (status === 401) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(new Error(formatHttpError(error)));
  },
);

export async function requestData<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return unwrapApiResponse(response.data);
}

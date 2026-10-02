import { describe, it, expect, beforeEach, vi } from 'vitest';
import type { LoginResponse } from '../types/api';

const ACCESS_KEY = 'sr_admin_access_token';
const REFRESH_KEY = 'sr_admin_refresh_token';
const USERNAME_KEY = 'sr_admin_username';

const ADMIN_SESSION: LoginResponse = {
  userId: 1,
  username: 'admin01',
  role: 'ADMIN',
  accessToken: 'access-token-abc',
  refreshToken: 'refresh-token-xyz',
  expiresIn: 3600,
};

const USER_SESSION: LoginResponse = {
  ...ADMIN_SESSION,
  username: 'user01',
  role: 'USER',
};

// 通过 vi.resetModules + 动态 import 重新加载 store 模块，
// 模拟浏览器刷新后首次加载场景，确保各测试间状态隔离。
async function loadStore() {
  vi.resetModules();
  const mod = await import('./adminAuthStore');
  return mod.useAdminAuthStore;
}

describe('adminAuthStore', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('setSession', () => {
    it('ADMIN role 时持久化 token/username 到 localStorage 并更新 store', async () => {
      const useAdminAuthStore = await loadStore();

      useAdminAuthStore.getState().setSession(ADMIN_SESSION);

      expect(localStorage.getItem(ACCESS_KEY)).toBe('access-token-abc');
      expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-token-xyz');
      expect(localStorage.getItem(USERNAME_KEY)).toBe('admin01');

      const state = useAdminAuthStore.getState();
      expect(state.accessToken).toBe('access-token-abc');
      expect(state.refreshToken).toBe('refresh-token-xyz');
      expect(state.username).toBe('admin01');
    });

    it('USER role 时抛错"当前账号不是管理员"且不写 localStorage', async () => {
      const useAdminAuthStore = await loadStore();

      expect(() => useAdminAuthStore.getState().setSession(USER_SESSION)).toThrow(
        '当前账号不是管理员',
      );

      expect(localStorage.getItem(ACCESS_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_KEY)).toBeNull();
      expect(localStorage.getItem(USERNAME_KEY)).toBeNull();

      const state = useAdminAuthStore.getState();
      expect(state.accessToken).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.username).toBeNull();
    });
  });

  describe('logout', () => {
    it('清空 localStorage 并将 store 状态置空', async () => {
      const useAdminAuthStore = await loadStore();
      useAdminAuthStore.getState().setSession(ADMIN_SESSION);

      useAdminAuthStore.getState().logout();

      expect(localStorage.getItem(ACCESS_KEY)).toBeNull();
      expect(localStorage.getItem(REFRESH_KEY)).toBeNull();
      expect(localStorage.getItem(USERNAME_KEY)).toBeNull();

      const state = useAdminAuthStore.getState();
      expect(state.accessToken).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.username).toBeNull();
    });
  });

  describe('初始化', () => {
    it('localStorage 有 token/username 时 store 正确恢复（模拟刷新后加载）', async () => {
      localStorage.setItem(ACCESS_KEY, 'persisted-access');
      localStorage.setItem(REFRESH_KEY, 'persisted-refresh');
      localStorage.setItem(USERNAME_KEY, 'persisted-admin');

      // 模拟页面刷新：store 模块首次加载，从 localStorage 读取
      const useAdminAuthStore = await loadStore();
      const state = useAdminAuthStore.getState();

      expect(state.accessToken).toBe('persisted-access');
      expect(state.refreshToken).toBe('persisted-refresh');
      expect(state.username).toBe('persisted-admin');
    });

    it('localStorage 为空时 store 初始化为空状态', async () => {
      const useAdminAuthStore = await loadStore();
      const state = useAdminAuthStore.getState();

      expect(state.accessToken).toBeNull();
      expect(state.refreshToken).toBeNull();
      expect(state.username).toBeNull();
    });
  });
});

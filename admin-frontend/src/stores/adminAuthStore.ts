import { create } from 'zustand';
import type { LoginResponse } from '../types/api';

interface AdminAuthState {
  accessToken: string | null;
  refreshToken: string | null;
  username: string | null;
  setSession: (session: LoginResponse) => void;
  logout: () => void;
}

const ACCESS_KEY = 'sr_admin_access_token';
const REFRESH_KEY = 'sr_admin_refresh_token';
const USERNAME_KEY = 'sr_admin_username';

export const useAdminAuthStore = create<AdminAuthState>((set) => ({
  accessToken: localStorage.getItem(ACCESS_KEY),
  refreshToken: localStorage.getItem(REFRESH_KEY),
  username: localStorage.getItem(USERNAME_KEY),
  setSession: (session) => {
    if (session.role !== 'ADMIN') throw new Error('当前账号不是管理员');
    localStorage.setItem(ACCESS_KEY, session.accessToken);
    localStorage.setItem(REFRESH_KEY, session.refreshToken);
    localStorage.setItem(USERNAME_KEY, session.username);
    set({
      accessToken: session.accessToken,
      refreshToken: session.refreshToken,
      username: session.username,
    });
  },
  logout: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USERNAME_KEY);
    set({ accessToken: null, refreshToken: null, username: null });
  },
}));

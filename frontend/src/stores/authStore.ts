import { create } from 'zustand';
import type { LoginResponse, UserMeResponse } from '../types/api';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  currentUser: UserMeResponse | null;
  setSession: (session: LoginResponse) => void;
  updateTokens: (accessToken: string, refreshToken: string) => void;
  setCurrentUser: (user: UserMeResponse) => void;
  logout: () => void;
}

const ACCESS_KEY = 'sr_user_access_token';
const REFRESH_KEY = 'sr_user_refresh_token';

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: localStorage.getItem(ACCESS_KEY),
  refreshToken: localStorage.getItem(REFRESH_KEY),
  currentUser: null,
  setSession: (session) => {
    localStorage.setItem(ACCESS_KEY, session.accessToken);
    localStorage.setItem(REFRESH_KEY, session.refreshToken);
    set({ accessToken: session.accessToken, refreshToken: session.refreshToken });
  },
  updateTokens: (accessToken, refreshToken) => {
    localStorage.setItem(ACCESS_KEY, accessToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
    set({ accessToken, refreshToken });
  },
  setCurrentUser: (user) => set({ currentUser: user }),
  logout: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    set({ accessToken: null, refreshToken: null, currentUser: null });
  },
}));

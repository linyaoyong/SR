import { beforeEach, describe, expect, it, vi } from 'vitest';

const ACCESS_KEY = 'sr_user_access_token';
const REFRESH_KEY = 'sr_user_refresh_token';

describe('authStore', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetModules();
  });

  it('initial state reads existing tokens from localStorage', async () => {
    localStorage.setItem(ACCESS_KEY, 'access-abc');
    localStorage.setItem(REFRESH_KEY, 'refresh-xyz');
    const { useAuthStore } = await import('./authStore');
    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('access-abc');
    expect(state.refreshToken).toBe('refresh-xyz');
    expect(state.currentUser).toBeNull();
  });

  it('setSession persists tokens to localStorage and state', async () => {
    const { useAuthStore } = await import('./authStore');
    useAuthStore.getState().setSession({
      userId: 7,
      username: 'alice',
      role: 'USER',
      accessToken: 'access-7',
      refreshToken: 'refresh-7',
      expiresIn: 7200,
    });
    expect(useAuthStore.getState().accessToken).toBe('access-7');
    expect(useAuthStore.getState().refreshToken).toBe('refresh-7');
    expect(localStorage.getItem(ACCESS_KEY)).toBe('access-7');
    expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-7');
  });

  it('setCurrentUser stores the user in state', async () => {
    const { useAuthStore } = await import('./authStore');
    const user = {
      id: 7,
      username: 'alice',
      creditScore: 80,
      role: 0,
      status: 0,
      usernameAuditStatus: 1,
      avatarAuditStatus: 1,
      descriptionAuditStatus: 1,
      showRentalHistory: 1,
    };
    useAuthStore.getState().setCurrentUser(user);
    expect(useAuthStore.getState().currentUser).toEqual(user);
  });

  it('logout clears localStorage and state', async () => {
    const { useAuthStore } = await import('./authStore');
    useAuthStore.getState().setSession({
      userId: 7,
      username: 'alice',
      role: 'USER',
      accessToken: 'access-7',
      refreshToken: 'refresh-7',
      expiresIn: 7200,
    });
    useAuthStore.getState().setCurrentUser({
      id: 7,
      username: 'alice',
      creditScore: 80,
      role: 0,
      status: 0,
      usernameAuditStatus: 1,
      avatarAuditStatus: 1,
      descriptionAuditStatus: 1,
      showRentalHistory: 1,
    });
    useAuthStore.getState().logout();
    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(useAuthStore.getState().refreshToken).toBeNull();
    expect(useAuthStore.getState().currentUser).toBeNull();
    expect(localStorage.getItem(ACCESS_KEY)).toBeNull();
    expect(localStorage.getItem(REFRESH_KEY)).toBeNull();
  });
});

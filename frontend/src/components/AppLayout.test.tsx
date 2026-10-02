import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../stores/authStore';
import AppLayout from './AppLayout';

const mocks = vi.hoisted(() => ({
  authService: {
    me: vi.fn(),
  },
  walletState: {
    fetchWallet: vi.fn(),
    reset: vi.fn(),
    hasDebt: vi.fn(() => false),
  },
  messageState: {
    unreadTotal: 0,
    connectSocket: vi.fn(),
    disconnectSocket: vi.fn(),
    fetchUnreadCount: vi.fn(),
    reset: vi.fn(),
  },
}));

vi.mock('../services/auth', () => ({ authService: mocks.authService }));
vi.mock('../stores/walletStore', () => ({
  useWalletStore: (selector: (state: typeof mocks.walletState) => unknown) =>
    selector(mocks.walletState),
}));
vi.mock('../stores/messageStore', () => ({
  useMessageStore: (selector: (state: typeof mocks.messageState) => unknown) =>
    selector(mocks.messageState),
}));

function renderLayout() {
  return render(
    <MemoryRouter initialEntries={['/profile']}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/items" element={<div data-testid="items-page">租赁内容</div>} />
          <Route path="/profile" element={<div>资料内容</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('AppLayout auth bootstrap', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.getState().logout();
  });

  it('loads current user when token exists but user state is empty after refresh', async () => {
    useAuthStore.getState().setSession({
      userId: 7,
      username: 'alice',
      role: 'USER',
      accessToken: 'access-7',
      refreshToken: 'refresh-7',
      expiresIn: 7200,
    });
    useAuthStore.setState({ currentUser: null });
    mocks.authService.me.mockResolvedValue({
      id: 7,
      username: 'alice',
      creditScore: 90,
      role: 0,
      status: 0,
      usernameAuditStatus: 1,
      avatarAuditStatus: 1,
      descriptionAuditStatus: 1,
      showRentalHistory: 1,
    });

    renderLayout();

    await waitFor(() => expect(mocks.authService.me).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('alice')).toBeInTheDocument();
  });

  it('links topbar user area to profile and logs out back to home', async () => {
    useAuthStore.getState().setSession({
      userId: 7,
      username: 'alice',
      role: 'USER',
      accessToken: 'access-7',
      refreshToken: 'refresh-7',
      expiresIn: 7200,
    });
    useAuthStore.setState({
      currentUser: {
        id: 7,
        username: 'alice',
        creditScore: 90,
        role: 0,
        status: 0,
        usernameAuditStatus: 1,
        avatarAuditStatus: 1,
        descriptionAuditStatus: 1,
        showRentalHistory: 1,
      },
    });

    renderLayout();

    expect(screen.getByRole('link', { name: /alice/ })).toHaveAttribute('href', '/profile');
    await userEvent.click(screen.getByRole('button', { name: '退出登录' }));

    await waitFor(() => expect(useAuthStore.getState().accessToken).toBeNull());
    expect(await screen.findByTestId('items-page')).toBeInTheDocument();
  });

  it('links the brand name to the rental list', () => {
    renderLayout();

    expect(screen.getByRole('link', { name: '邻享租借' })).toHaveAttribute('href', '/items');
  });
});

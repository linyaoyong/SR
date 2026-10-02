import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RegisterPage from './RegisterPage';
import { useAuthStore } from '../stores/authStore';

const mocks = vi.hoisted(() => ({
  authService: {
    register: vi.fn(),
    login: vi.fn(),
    me: vi.fn(),
  },
}));

vi.mock('../services/auth', () => ({ authService: mocks.authService }));

function renderRegisterPage() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/items" element={<div data-testid="items-page">租赁列表</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.getState().logout();
  });

  it('logs in after registration because register returns no session payload', async () => {
    mocks.authService.register.mockResolvedValue(undefined);
    mocks.authService.login.mockResolvedValue({
      userId: 8,
      username: 'new-user',
      role: 'USER',
      accessToken: 'access-8',
      refreshToken: 'refresh-8',
      expiresIn: 7200,
    });
    mocks.authService.me.mockResolvedValue({
      id: 8,
      username: 'new-user',
      creditScore: 80,
      role: 0,
      status: 0,
      usernameAuditStatus: 0,
      avatarAuditStatus: 1,
      descriptionAuditStatus: 1,
      showRentalHistory: 1,
    });

    renderRegisterPage();

    await userEvent.type(screen.getByLabelText('用户名'), 'new-user');
    await userEvent.type(screen.getByLabelText('密码'), 'secret1');
    await userEvent.type(screen.getByLabelText('确认密码'), 'secret1');
    await userEvent.click(screen.getByRole('button', { name: /注\s*册/ }));

    await waitFor(() => {
      expect(mocks.authService.register).toHaveBeenCalledWith({
        username: 'new-user',
        password: 'secret1',
      });
      expect(mocks.authService.login).toHaveBeenCalledWith({
        username: 'new-user',
        password: 'secret1',
      });
    });
    expect(useAuthStore.getState().accessToken).toBe('access-8');
    expect(await screen.findByTestId('items-page')).toBeInTheDocument();
  });
});

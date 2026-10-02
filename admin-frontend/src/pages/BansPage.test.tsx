import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import BansPage from './BansPage';

vi.mock('../services/admin', () => ({
  adminService: {
    banUser: vi.fn(),
    unbanUser: vi.fn(),
  },
}));

describe('BansPage', () => {
  it('does not show the redundant user-audit entry hint', () => {
    render(
      <MemoryRouter>
        <BansPage />
      </MemoryRouter>,
    );

    expect(screen.queryByText(/推荐从用户审核详情页进入封禁操作/)).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '封禁用户' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '解封用户' })).toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import App from './App';

vi.mock('./pages/ItemsPage', () => ({
  default: () => <div data-testid="items-page">租赁列表</div>,
}));
vi.mock('./pages/UserDetailPage', () => ({
  default: () => <div data-testid="user-detail-page">用户公开主页</div>,
}));

describe('App routes', () => {
  it('mounts the public user detail page at /users/:id', async () => {
    window.history.pushState({}, '', '/users/42');

    render(<App />);

    expect(await screen.findByTestId('user-detail-page')).toBeInTheDocument();
  });
});

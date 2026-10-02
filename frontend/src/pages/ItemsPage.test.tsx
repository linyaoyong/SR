import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../stores/authStore';
import ItemsPage from './ItemsPage';

const mocks = vi.hoisted(() => ({
  itemService: {
    categories: vi.fn(),
    list: vi.fn(),
    detail: vi.fn(),
    favorites: vi.fn(),
    addFavorite: vi.fn(),
    removeFavorite: vi.fn(),
  },
}));

vi.mock('../services/item', () => ({ itemService: mocks.itemService }));

const sampleItem = {
  id: 101,
  title: '电钻 ST-200',
  categoryId: 3,
  dailyPrice: 25,
  minRentDays: 1,
  depositAmount: 100,
  status: 1,
  auditStatus: 1,
  firstImageUrl: 'https://example.com/a.png',
  createTime: '2026-06-01 10:00:00',
};

const emptyPage = { records: [], total: 0, page: 1, size: 12, pages: 0 };
const onePage = {
  records: [sampleItem],
  total: 1,
  page: 1,
  size: 12,
  pages: 1,
};

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/items" element={<ItemsPage />} />
        <Route path="/login" element={<div data-testid="login-page" />} />
        <Route path="/items/create" element={<div data-testid="create-page" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ItemsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.getState().logout();
    mocks.itemService.categories.mockResolvedValue([]);
    mocks.itemService.favorites.mockResolvedValue([]);
  });

  it('renders item list with title and price', async () => {
    mocks.itemService.list.mockResolvedValue(onePage);
    renderAt('/items');
    await waitFor(() => {
      expect(screen.getByText('电钻 ST-200')).toBeInTheDocument();
    });
    expect(screen.getByText(/¥25/)).toBeInTheDocument();
  });

  it('shows login prompt empty state when unauthenticated and list is empty', async () => {
    mocks.itemService.list.mockResolvedValue(emptyPage);
    renderAt('/items');
    await waitFor(() => {
      expect(screen.getByText('登录后发布')).toBeInTheDocument();
    });
    expect(screen.queryByText('发布第一个物品')).not.toBeInTheDocument();
  });

  it('shows publish empty state when authenticated and list is empty', async () => {
    useAuthStore.getState().setSession({
      userId: 7,
      username: 'alice',
      role: 'USER',
      accessToken: 'access-7',
      refreshToken: 'refresh-7',
      expiresIn: 7200,
    });
    mocks.itemService.list.mockResolvedValue(emptyPage);
    renderAt('/items');
    await waitFor(() => {
      expect(screen.getByText('发布第一个物品')).toBeInTheDocument();
    });
  });

  it('redirects to /login when unauthenticated user clicks favorite', async () => {
    mocks.itemService.list.mockResolvedValue(onePage);
    renderAt('/items');
    const favButton = await screen.findByLabelText('收藏');
    await userEvent.click(favButton);
    await waitFor(() => {
      expect(screen.getByTestId('login-page')).toBeInTheDocument();
    });
  });

  it('uses category chips under the title instead of a category dropdown', async () => {
    mocks.itemService.categories.mockResolvedValue([
      { id: 3, name: '数码', sortOrder: 1, status: 1 },
      { id: 4, name: '户外', sortOrder: 2, status: 1 },
    ]);
    mocks.itemService.list.mockResolvedValue(onePage);
    renderAt('/items');

    expect(await screen.findByRole('button', { name: '全部' })).toHaveClass('active');
    expect(screen.getByRole('button', { name: '数码' })).toBeInTheDocument();
    expect(screen.queryByText('全部分类')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '数码' }));

    await waitFor(() => {
      expect(mocks.itemService.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ categoryId: 3, page: 1 }),
      );
    });
  });
});

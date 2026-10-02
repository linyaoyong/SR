import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import FavoritesPage from './FavoritesPage';

const mocks = vi.hoisted(() => ({
  itemService: {
    favorites: vi.fn(),
    removeFavorite: vi.fn(),
  },
}));

vi.mock('../services/item', () => ({ itemService: mocks.itemService }));

function renderFavorites() {
  return render(
    <MemoryRouter initialEntries={['/favorites']}>
      <Routes>
        <Route path="/favorites" element={<FavoritesPage />} />
        <Route path="/items/:id" element={<div data-testid="item-detail" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('FavoritesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.itemService.favorites.mockResolvedValue([
      {
        id: 1,
        itemId: 11,
        itemTitle: '露营灯',
        dailyPrice: 12,
        firstImageUrl: 'https://example.com/light.png',
      },
    ]);
  });

  it('uses a filled heart affordance for cancel favorite instead of a trash icon', async () => {
    const { container } = renderFavorites();

    const cancelButton = await screen.findByRole('button', { name: '取消收藏' });

    await waitFor(() => expect(cancelButton.querySelector('.lucide-heart')).toBeTruthy());
    expect(cancelButton.querySelector('.lucide-trash-2')).toBeNull();
    expect(cancelButton.querySelector('svg.lucide-heart')).toHaveAttribute('fill', 'currentColor');
    expect(container.querySelector('.lucide-trash-2')).toBeNull();
  });
});

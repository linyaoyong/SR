import { useCallback, useEffect, useState } from 'react';
import { Spin, message } from 'antd';
import { Link } from 'react-router-dom';
import ItemCard, { type ItemCardItem } from '../components/ItemCard';
import { itemService } from '../services/item';
import type { FavoriteItem } from '../types/api';

export default function FavoritesPage() {
  const [favorites, setFavorites] = useState<FavoriteItem[]>([]);
  const [loading, setLoading] = useState(true);

  const loadFavorites = useCallback(async () => {
    setLoading(true);
    try {
      const data = await itemService.favorites();
      setFavorites(data);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadFavorites();
  }, [loadFavorites]);

  const handleRemove = async (itemId: number) => {
    try {
      await itemService.removeFavorite(itemId);
      setFavorites((prev) => prev.filter((f) => f.itemId !== itemId));
      message.success('已取消收藏');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    }
  };

  if (loading) {
    return (
      <main className="sr-page">
        <section className="sr-section sr-detail-loading">
          <Spin />
        </section>
      </main>
    );
  }

  return (
    <main className="sr-page">
      <section className="sr-section">
        <h1 className="sr-title">我的收藏</h1>
        {favorites.length === 0 ? (
          <div className="sr-empty">
            <p>还没有收藏</p>
            <Link to="/items" className="sr-btn sr-btn-md sr-btn-primary">
              去发现物品
            </Link>
          </div>
        ) : (
          <div className="sr-item-grid">
            {favorites.map((favorite) => {
              const cardItem: ItemCardItem = {
                id: favorite.itemId,
                title: favorite.itemTitle,
                dailyPrice: favorite.dailyPrice,
                firstImageUrl: favorite.firstImageUrl,
              };
              return (
                <ItemCard
                  key={favorite.id}
                  item={cardItem}
                  onRemoveFavorite={handleRemove}
                />
              );
            })}
          </div>
        )}
      </section>
    </main>
  );
}

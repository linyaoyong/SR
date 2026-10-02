import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Input, Pagination, Spin, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import ItemCard, { type ItemCardItem } from '../components/ItemCard';
import { itemService } from '../services/item';
import { useAuthStore } from '../stores/authStore';
import type { Category, ItemListItem } from '../types/api';

const PAGE_SIZE = 12;

export default function ItemsPage() {
  const navigate = useNavigate();
  const accessToken = useAuthStore((state) => state.accessToken);

  const [categories, setCategories] = useState<Category[]>([]);
  const [items, setItems] = useState<ItemListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>(undefined);
  const [favorites, setFavorites] = useState<Set<number>>(new Set());

  useEffect(() => {
    itemService
      .categories()
      .then(setCategories)
      .catch(() => setCategories([]));
  }, []);

  const fetchItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await itemService.list({
        categoryId,
        keyword: keyword || undefined,
        page,
        size: PAGE_SIZE,
      });
      setItems(result.records);
      setTotal(result.total);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
      setItems([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [categoryId, keyword, page]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  useEffect(() => {
    if (!accessToken) {
      setFavorites(new Set());
      return;
    }
    itemService
      .favorites()
      .then((favs) => setFavorites(new Set(favs.map((f) => f.itemId))))
      .catch(() => setFavorites(new Set()));
  }, [accessToken]);

  const handleSearch = (value: string) => {
    setKeyword(value);
    setPage(1);
  };

  const handleCategoryChange = (value: number | undefined) => {
    setCategoryId(value);
    setPage(1);
  };

  const handleToggleFavorite = useCallback(
    async (item: ItemCardItem) => {
      const isFavorite = favorites.has(item.id);
      try {
        if (isFavorite) {
          await itemService.removeFavorite(item.id);
          setFavorites((prev) => {
            const next = new Set(prev);
            next.delete(item.id);
            return next;
          });
          message.success('已取消收藏');
        } else {
          await itemService.addFavorite(item.id);
          setFavorites((prev) => new Set(prev).add(item.id));
          message.success('已收藏');
        }
      } catch (err) {
        message.error(err instanceof Error ? err.message : '操作失败');
      }
    },
    [favorites],
  );

  const resolveCategoryName = useMemo(() => {
    const map = new Map(categories.map((c) => [c.id, c.name]));
    return (id?: number) => (id != null ? map.get(id) : undefined);
  }, [categories]);

  const isEmpty = !loading && items.length === 0 && !error;

  return (
    <main className="sr-page">
      <section className="sr-section">
        <div className="sr-items-header">
          <h1 className="sr-title">租赁</h1>
          <div className="sr-items-filters">
            <Input.Search
              placeholder="搜索物品关键词"
              allowClear
              onSearch={handleSearch}
              onChange={(e) => {
                if (!e.target.value) handleSearch('');
              }}
              style={{ maxWidth: 240 }}
            />
          </div>
        </div>
        <div className="sr-category-chips" aria-label="分类筛选">
          <button
            type="button"
            className={`sr-category-chip${categoryId == null ? ' active' : ''}`}
            onClick={() => handleCategoryChange(undefined)}
          >
            全部
          </button>
          {categories.map((category) => (
            <button
              key={category.id}
              type="button"
              className={`sr-category-chip${categoryId === category.id ? ' active' : ''}`}
              onClick={() => handleCategoryChange(category.id)}
            >
              {category.name}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="sr-items-loading">
            <Spin />
          </div>
        ) : error ? (
          <div className="sr-empty">
            <p>{error}</p>
            <Button onClick={() => fetchItems()}>重试</Button>
          </div>
        ) : isEmpty ? (
          <div className="sr-empty">
            <p>暂无物品</p>
            {accessToken ? (
              <Button type="primary" onClick={() => navigate('/items/create')}>
                发布第一个物品
              </Button>
            ) : (
              <Button type="primary" onClick={() => navigate('/login')}>
                登录后发布
              </Button>
            )}
          </div>
        ) : (
          <>
            <div className="sr-item-grid">
              {items.map((item) => (
                <ItemCard
                  key={item.id}
                  item={item}
                  categoryName={resolveCategoryName(item.categoryId)}
                  favoriteState={favorites.has(item.id)}
                  onToggleFavorite={accessToken ? handleToggleFavorite : undefined}
                />
              ))}
            </div>
            {total > PAGE_SIZE && (
              <div className="sr-items-pagination">
                <Pagination
                  current={page}
                  total={total}
                  pageSize={PAGE_SIZE}
                  onChange={setPage}
                  showSizeChanger={false}
                />
              </div>
            )}
          </>
        )}
      </section>
    </main>
  );
}

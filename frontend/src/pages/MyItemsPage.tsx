import { useCallback, useEffect, useState } from 'react';
import { Button, Pagination, Popconfirm, Spin, Tag, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { itemMutations, itemService } from '../services/item';
import { itemAuditText, itemStatusText } from '../types/status';
import type { ItemListItem } from '../types/api';

const PAGE_SIZE = 12;

export default function MyItemsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<ItemListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  // auditStatus=2 时需展示审核原因；ItemListItem 不含 auditReason，按需拉取详情。
  const [reasons, setReasons] = useState<Record<number, string>>({});
  const [actioningId, setActioningId] = useState<number | null>(null);

  const loadItems = useCallback(async (targetPage: number) => {
    setLoading(true);
    setError(null);
    try {
      const result = await itemMutations.mine(targetPage, PAGE_SIZE);
      setItems(result.records);
      setTotal(result.total);
      setReasons({});
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
      setItems([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadItems(page);
  }, [page, loadItems]);

  // 对当前页"要求整改"的物品拉取审核原因。
  useEffect(() => {
    const targets = items.filter((it) => it.auditStatus === 2);
    if (targets.length === 0) return;
    let active = true;
    (async () => {
      const entries = await Promise.all(
        targets.map((it) =>
          itemService
            .detail(it.id)
            .then((detail) => [it.id, detail.auditReason ?? ''] as const)
            .catch(() => [it.id, ''] as const),
        ),
      );
      if (!active) return;
      const map: Record<number, string> = {};
      entries.forEach(([id, reason]) => {
        if (reason) map[id] = reason;
      });
      setReasons(map);
    })();
    return () => {
      active = false;
    };
  }, [items]);

  const handleOffShelf = async (id: number) => {
    setActioningId(id);
    try {
      await itemMutations.offShelf(id);
      message.success('已下架');
      await loadItems(page);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioningId(null);
    }
  };

  const handleReList = async (id: number) => {
    setActioningId(id);
    try {
      await itemMutations.reList(id);
      message.success('已重新上架');
      await loadItems(page);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioningId(null);
    }
  };

  const handleDelete = async (id: number) => {
    setActioningId(id);
    try {
      await itemMutations.remove(id);
      message.success('已删除');
      await loadItems(page);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioningId(null);
    }
  };

  const isEmpty = !loading && items.length === 0 && !error;

  return (
    <main className="sr-page">
      <section className="sr-section">
        <div className="sr-items-header">
          <h1 className="sr-title">我的物品</h1>
          <Button type="primary" onClick={() => navigate('/items/create')}>
            发布物品
          </Button>
        </div>

        {loading ? (
          <div className="sr-items-loading">
            <Spin />
          </div>
        ) : error ? (
          <div className="sr-empty">
            <p>{error}</p>
            <Button onClick={() => loadItems(page)}>重试</Button>
          </div>
        ) : isEmpty ? (
          <div className="sr-empty">
            <p>还没有发布物品</p>
            <Button type="primary" onClick={() => navigate('/items/create')}>
              发布物品
            </Button>
          </div>
        ) : (
          <>
            <div className="sr-my-items-grid">
              {items.map((item) => {
                const reason = reasons[item.id];
                const isListed = item.status === 1;
                const isOffShelf = item.status === 2;
                return (
                  <article key={item.id} className="sr-card sr-my-items-card">
                    <div className="sr-my-items-card-header">
                      <h3
                        className="sr-my-items-card-title"
                        onClick={() => navigate(`/items/${item.id}`)}
                      >
                        {item.title}
                      </h3>
                      <Tag>{itemStatusText[item.status]}</Tag>
                      <Tag color={item.auditStatus === 2 ? 'warning' : undefined}>
                        {itemAuditText[item.auditStatus]}
                      </Tag>
                    </div>
                    {item.firstImageUrl ? (
                      <img
                        src={item.firstImageUrl}
                        alt={item.title}
                        style={{
                          width: '100%',
                          aspectRatio: '4 / 3',
                          objectFit: 'cover',
                          borderRadius: 'var(--radius-card)',
                          cursor: 'pointer',
                        }}
                        onClick={() => navigate(`/items/${item.id}`)}
                      />
                    ) : (
                      <div
                        className="sr-empty"
                        style={{ aspectRatio: '4 / 3', display: 'flex', alignItems: 'center' }}
                        onClick={() => navigate(`/items/${item.id}`)}
                      >
                        暂无图片
                      </div>
                    )}
                    <div className="sr-my-items-card-price">¥{item.dailyPrice}/天</div>
                    {item.auditStatus === 2 && reason && (
                      <p className="sr-my-items-card-reason">整改原因：{reason}</p>
                    )}
                    <div className="sr-my-items-card-actions">
                      <Button
                        size="small"
                        onClick={() => navigate(`/items/${item.id}/edit`)}
                      >
                        编辑
                      </Button>
                      {isListed && (
                        <Popconfirm
                          title="确认下架该物品？"
                          onConfirm={() => handleOffShelf(item.id)}
                          okText="下架"
                          cancelText="取消"
                        >
                          <Button size="small" loading={actioningId === item.id}>
                            下架
                          </Button>
                        </Popconfirm>
                      )}
                      {isOffShelf && (
                        <Popconfirm
                          title="确认重新上架？"
                          onConfirm={() => handleReList(item.id)}
                          okText="上架"
                          cancelText="取消"
                        >
                          <Button size="small" loading={actioningId === item.id}>
                            重新上架
                          </Button>
                        </Popconfirm>
                      )}
                      <Popconfirm
                        title="确认删除该物品？删除后无法恢复。"
                        onConfirm={() => handleDelete(item.id)}
                        okText="删除"
                        cancelText="取消"
                        okButtonProps={{ danger: true }}
                      >
                        <Button size="small" danger loading={actioningId === item.id}>
                          删除
                        </Button>
                      </Popconfirm>
                    </div>
                  </article>
                );
              })}
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

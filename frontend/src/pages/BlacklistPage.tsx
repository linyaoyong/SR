import { useCallback, useEffect, useState } from 'react';
import { Avatar, Button, Popconfirm, Spin, message } from 'antd';
import { Link } from 'react-router-dom';
import { userService } from '../services/user';
import type { UserPublicProfile } from '../types/api';

// 黑名单管理页：拉取黑名单列表 + 解除拉黑。
// 拉黑入口在其他页面（如用户主页 / 聊天），本页只做查看与解除。
// 字段对齐 docs/03-api-contract.md auth-service 黑名单接口。
export default function BlacklistPage() {
  const [list, setList] = useState<UserPublicProfile[]>([]);
  const [loading, setLoading] = useState(true);
  const [removingId, setRemovingId] = useState<number | null>(null);

  const loadList = useCallback(async () => {
    setLoading(true);
    try {
      const data = await userService.blacklist();
      setList(data);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载失败');
      setList([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const handleRemove = async (userId: number) => {
    setRemovingId(userId);
    try {
      await userService.removeBlacklist(userId);
      setList((prev) => prev.filter((u) => u.id !== userId));
      message.success('已解除拉黑');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setRemovingId(null);
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
        <div className="sr-items-header">
          <h1 className="sr-title">黑名单管理</h1>
          <Button onClick={loadList}>刷新</Button>
        </div>
        {list.length === 0 ? (
          <div className="sr-empty">
            <p>还没有拉黑用户</p>
            <Link to="/items" className="sr-btn sr-btn-md sr-btn-primary">
              去发现物品
            </Link>
          </div>
        ) : (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
            {list.map((user) => (
              <li
                key={user.id}
                className="sr-card"
                style={{
                  padding: 'var(--space-4)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: 'var(--space-3)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-3)' }}>
                  {user.avatarUrl ? (
                    <Avatar size={48} src={user.avatarUrl} />
                  ) : (
                    <Avatar size={48}>{user.username.slice(0, 1)}</Avatar>
                  )}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
                    <span style={{ fontWeight: 600 }}>{user.username}</span>
                    <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>
                      信用分：{user.creditScore}
                    </span>
                    {user.description && (
                      <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>
                        {user.description}
                      </span>
                    )}
                  </div>
                </div>
                <Popconfirm
                  title="确认解除拉黑？"
                  onConfirm={() => handleRemove(user.id)}
                  okText="解除"
                  cancelText="取消"
                >
                  <Button danger loading={removingId === user.id}>
                    解除拉黑
                  </Button>
                </Popconfirm>
              </li>
            ))}
          </ul>
        )}
        <div className="sr-actions" style={{ marginTop: 'var(--space-5)' }}>
          <Link to="/profile" className="sr-btn sr-btn-md sr-btn-secondary">
            返回我的
          </Link>
        </div>
      </section>
    </main>
  );
}

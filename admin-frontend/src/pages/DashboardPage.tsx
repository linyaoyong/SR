import { useCallback, useEffect, useState } from 'react';
import { Button, Row, Col, Spin } from 'antd';
import { Link } from 'react-router-dom';
import { adminService } from '../services/admin';
import type { AdminDashboardResponse } from '../types/api';

// 统计卡片配置：字段 → 标签 + 强调色（用 admin 视觉令牌）
const STAT_CARDS: {
  key: keyof AdminDashboardResponse;
  label: string;
  tone: 'primary' | 'warning' | 'success' | 'error';
}[] = [
  { key: 'itemAuditCount', label: '待审核物品', tone: 'warning' },
  { key: 'userAuditCount', label: '待审核用户资料', tone: 'warning' },
  { key: 'totalUsers', label: '用户总数', tone: 'primary' },
  { key: 'totalItems', label: '物品总数', tone: 'primary' },
  { key: 'bannedUsers', label: '已封禁用户', tone: 'error' },
];

// 快捷入口
const QUICK_LINKS: { to: string; label: string }[] = [
  { to: '/users/audits', label: '用户审核' },
  { to: '/items/audits', label: '物品审核' },
  { to: '/disputes', label: '异议查看' },
  { to: '/logs', label: '操作日志' },
];

const toneColor: Record<string, string> = {
  primary: 'var(--admin-color-primary)',
  warning: 'var(--admin-color-warning)',
  success: 'var(--admin-color-success)',
  error: 'var(--admin-color-error)',
};

// 后台首页仪表盘：聚合统计 + 快捷入口
export default function DashboardPage() {
  const [data, setData] = useState<AdminDashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminService.dashboard();
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return (
      <div className="admin-empty">
        <Spin />
      </div>
    );
  }

  if (error) {
    return (
      <div className="admin-alert admin-alert--error">
        <span>加载失败：{error}</span>
        <Button className="admin-btn-secondary" size="small" onClick={load} style={{ marginLeft: 12 }}>
          重试
        </Button>
      </div>
    );
  }

  if (!data) return null;

  return (
    <>
      <Row gutter={[16, 16]}>
        {STAT_CARDS.map((card) => (
          <Col key={card.key} xs={24} sm={12} md={8} lg={6}>
            <div className="admin-panel">
              <p className="admin-caption">{card.label}</p>
              <p
                style={{
                  fontSize: 28,
                  fontWeight: 600,
                  margin: '8px 0 0',
                  color: toneColor[card.tone],
                }}
              >
                {data[card.key]}
              </p>
            </div>
          </Col>
        ))}
      </Row>

      <section className="admin-panel">
        <h2 className="admin-title" style={{ fontSize: 16 }}>
          快捷入口
        </h2>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, marginTop: 12 }}>
          {QUICK_LINKS.map((link) => (
            <Link key={link.to} to={link.to}>
              <Button className="admin-btn-secondary">{link.label}</Button>
            </Link>
          ))}
        </div>
      </section>
    </>
  );
}

import { useCallback, useEffect, useState } from 'react';
import { Button, Select, Space } from 'antd';
import { adminService } from '../services/admin';
import type { AdminLog } from '../types/api';
import OperationLogTable from '../components/OperationLogTable';

// 可选每页条数，默认 20
const SIZE_OPTIONS = [10, 20, 50];
const DEFAULT_SIZE = 20;

// 操作日志页：后端 /api/admin/logs 仅返回当前页 List，不含 total/totalPages，
// 因此无法使用 antd Pagination 的 total；改用"上一页 / 下一页"按钮，
// 通过"当前页返回条数 < size"判断是否到达末页。
export default function LogsPage() {
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(DEFAULT_SIZE);
  const [list, setList] = useState<AdminLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminService.logs(page, size);
      setList(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [page, size]);

  useEffect(() => {
    void load();
  }, [load]);

  // 切换 size 时回到第一页
  const handleSizeChange = (next: number) => {
    setSize(next);
    setPage(1);
  };

  const hasMore = list.length >= size;
  const canPrev = page > 1;
  const canNext = hasMore && !loading;

  if (error) {
    return (
      <div className="admin-alert admin-alert--error">
        <span>加载失败：{error}</span>
        <Button
          className="admin-btn-secondary"
          size="small"
          onClick={load}
          style={{ marginLeft: 12 }}
        >
          重试
        </Button>
      </div>
    );
  }

  return (
    <>
      <section className="admin-panel">
        <Space style={{ justifyContent: 'space-between', width: '100%' }} wrap>
          <Space>
            <span className="admin-caption">每页条数</span>
            <Select
              value={size}
              onChange={handleSizeChange}
              size="small"
              style={{ width: 80 }}
              options={SIZE_OPTIONS.map((s) => ({ label: `${s}`, value: s }))}
            />
            <span className="admin-caption">
              第 {page} 页 · 已加载 {list.length} 条
            </span>
          </Space>
          <Space>
            <Button
              className="admin-btn-secondary"
              disabled={!canPrev || loading}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
            >
              上一页
            </Button>
            <Button
              className="admin-btn-secondary"
              disabled={!canNext}
              onClick={() => setPage((p) => p + 1)}
            >
              下一页
            </Button>
          </Space>
        </Space>
      </section>

      <section className="admin-panel">
        <OperationLogTable logs={list} loading={loading} />
      </section>
    </>
  );
}

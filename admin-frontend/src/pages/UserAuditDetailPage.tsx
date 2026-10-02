import { useCallback, useEffect, useState } from 'react';
import { Button, Spin, message } from 'antd';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { adminService } from '../services/admin';
import type { UserAuditItem } from '../types/api';
import UserAuditPanel from '../components/UserAuditPanel';

// 从列表页跳转时通过 location.state 携带 UserAuditItem；
// state 为空（如刷新）时回退到 userAudits() 列表查找匹配项。
// 后端 listUserAudits(null) 返回所有待审核(0)或要求整改(2)的记录，刷新可恢复完整字段。
interface DetailLocationState {
  item?: UserAuditItem;
}

export default function UserAuditDetailPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const [item, setItem] = useState<UserAuditItem | null>(
    (location.state as DetailLocationState | null)?.item ?? null,
  );
  const [loading, setLoading] = useState(item === null);
  const [error, setError] = useState<string | null>(null);

  const userId = id ? Number(id) : NaN;

  const loadFromList = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await adminService.userAudits();
      const found = list.find((x) => x.userId === userId);
      if (!found) {
        setError('未找到该用户的审核记录');
        return;
      }
      setItem(found);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  // 封禁/解封后刷新当前详情数据（不跳转，重新从列表拉取最新 status）
  const refreshCurrent = useCallback(async () => {
    try {
      const list = await adminService.userAudits();
      const found = list.find((x) => x.userId === userId);
      if (found) {
        setItem(found);
      }
    } catch {
      // 刷新失败不影响已成功的操作反馈，忽略
    }
  }, [userId]);

  useEffect(() => {
    if (!item) void loadFromList();
    // 仅在首次挂载且无 state 时拉取，避免 item 变化重复拉取
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (loading) {
    return (
      <div className="admin-empty">
        <Spin />
      </div>
    );
  }

  if (error || !item) {
    return (
      <div className="admin-alert admin-alert--error">
        <span>{error ?? '未找到审核记录'}</span>
        <div style={{ marginTop: 12 }}>
          <Link to="/users/audits">
            <Button className="admin-btn-secondary" size="small">
              返回列表
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <>
      <div style={{ marginBottom: 12 }}>
        <Link to="/users/audits">
          <Button className="admin-btn-secondary" size="small">
            返回列表
          </Button>
        </Link>
      </div>
      <UserAuditPanel
        item={item}
        onChanged={() => {
          message.success('操作完成，已返回列表');
          navigate('/users/audits');
        }}
        onRefresh={refreshCurrent}
      />
    </>
  );
}

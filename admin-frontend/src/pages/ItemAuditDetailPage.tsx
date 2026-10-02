import { useCallback, useEffect, useState } from 'react';
import { Button, Spin, message } from 'antd';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { adminService } from '../services/admin';
import type { ItemAuditItem } from '../types/api';
import ItemAuditPanel from '../components/ItemAuditPanel';

// 从列表页跳转时通过 location.state 携带 ItemAuditItem；
// state 为空（如刷新）时回退到 itemAudits() 列表查找匹配项。
interface DetailLocationState {
  item?: ItemAuditItem;
}

export default function ItemAuditDetailPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const [item, setItem] = useState<ItemAuditItem | null>(
    (location.state as DetailLocationState | null)?.item ?? null,
  );
  const [loading, setLoading] = useState(item === null);
  const [error, setError] = useState<string | null>(null);

  const itemId = id ? Number(id) : NaN;

  const loadFromList = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const list = await adminService.itemAudits();
      const found = list.find((x) => x.id === itemId);
      if (!found) {
        setError('未找到该物品的审核记录');
        return;
      }
      setItem(found);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [itemId]);

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
          <Link to="/items/audits">
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
        <Link to="/items/audits">
          <Button className="admin-btn-secondary" size="small">
            返回列表
          </Button>
        </Link>
      </div>
      <ItemAuditPanel
        item={item}
        onChanged={() => {
          message.success('操作完成，已返回列表');
          navigate('/items/audits');
        }}
      />
    </>
  );
}

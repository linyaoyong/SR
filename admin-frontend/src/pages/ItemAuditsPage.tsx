import { useCallback, useEffect, useState } from 'react';
import { Button, Input, Modal, Popconfirm, Radio, Spin, Table, Tooltip, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { Link } from 'react-router-dom';
import { adminService } from '../services/admin';
import type { ItemAuditItem } from '../types/api';
import AuditStatusBadge from '../components/AuditStatusBadge';

// 筛选项：'all' 表示全部，其余为审核状态数值的字符串形式
type FilterValue = 'all' | '0' | '1' | '2';

const FILTER_OPTIONS: { label: string; value: FilterValue }[] = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: '0' },
  { label: '审核通过', value: '1' },
  { label: '要求整改', value: '2' },
];

// 物品状态映射：用于 tooltip 文案展示（status 字段含义：0=待审核/1=上架/2=下架/3=强制下架）
const itemStatusText: Record<number, string> = {
  0: '待审核',
  1: '上架',
  2: '下架',
  3: '强制下架',
};

const formatAuditTime = (raw?: string) => {
  if (!raw) return '-';
  const d = dayjs(raw);
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : raw;
};

const renderEllipsis = (text?: string) => {
  const value = text && text.trim() ? text : '-';
  return (
    <Tooltip title={value === '-' ? undefined : value}>
      <span className="admin-table-ellipsis">{value}</span>
    </Tooltip>
  );
};

// 物品审核列表页：拉取 itemAudits 列表，支持筛选与通过/要求整改/强制下架操作。
// 后端约定：物品审核 body 不含 fieldName；auditItem 返回 ItemAuditActionResponse（itemId 非 id）。
// 默认筛选"待审核"，让管理员一进页面就能看到需要处理的记录
export default function ItemAuditsPage() {
  const [filter, setFilter] = useState<FilterValue>('0');
  const [list, setList] = useState<ItemAuditItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  // 要求整改 Modal：当前操作的行 + 输入的原因
  const [reformTarget, setReformTarget] = useState<ItemAuditItem | null>(null);
  const [reformReason, setReformReason] = useState('');

  const statusParam = filter === 'all' ? undefined : Number(filter);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminService.itemAudits(statusParam);
      setList(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [statusParam]);

  useEffect(() => {
    void load();
  }, [load]);

  // 通过审核：body 不含 auditReason，不含 fieldName
  const handleApprove = async (row: ItemAuditItem) => {
    setActionLoading(true);
    try {
      const result = await adminService.auditItem(row.id, { auditStatus: 1 });
      // 后端返回 ItemAuditActionResponse（itemId/auditTime/auditAdminId）
      const timeText = result?.auditTime ? formatAuditTime(result.auditTime) : '';
      message.success(timeText ? `已通过审核，时间 ${timeText}` : '已通过审核');
      await load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  // 打开要求整改 Modal
  const openReform = (row: ItemAuditItem) => {
    setReformTarget(row);
    setReformReason('');
  };

  // 提交要求整改
  const submitReform = async () => {
    if (!reformTarget) return;
    const reason = reformReason.trim();
    if (!reason) {
      message.warning('请输入整改原因');
      return;
    }
    setActionLoading(true);
    try {
      await adminService.auditItem(reformTarget.id, {
        auditStatus: 2,
        auditReason: reason,
      });
      message.success('已要求整改');
      setReformTarget(null);
      setReformReason('');
      await load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  // 强制下架
  const handleForceOffShelf = async (row: ItemAuditItem) => {
    setActionLoading(true);
    try {
      await adminService.forceOffShelf(row.id);
      message.success('已强制下架');
      await load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const columns: ColumnsType<ItemAuditItem> = [
    { title: '物品 ID', dataIndex: 'id', key: 'id', width: 90 },
    { title: '标题', dataIndex: 'title', key: 'title' },
    { title: '物主 ID', dataIndex: 'ownerId', key: 'ownerId', width: 90 },
    {
      title: '物品状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => (
        <span title={itemStatusText[status] ?? '未知'}>状态: {status}</span>
      ),
    },
    {
      title: '审核状态',
      dataIndex: 'auditStatus',
      key: 'auditStatus',
      width: 110,
      render: (status: number) => <AuditStatusBadge auditStatus={status} />,
    },
    {
      title: '审核原因',
      dataIndex: 'auditReason',
      key: 'auditReason',
      width: 240,
      render: (reason?: string) => renderEllipsis(reason),
    },
    {
      title: '审核时间',
      dataIndex: 'auditTime',
      key: 'auditTime',
      width: 160,
      render: (raw?: string) => formatAuditTime(raw),
    },
    {
      title: '审核管理员',
      dataIndex: 'auditAdminId',
      key: 'auditAdminId',
      width: 110,
      render: (adminId?: number) => (adminId != null ? adminId : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 320,
      render: (_, row) => (
        <span style={{ display: 'inline-flex', gap: 8 }}>
          <Button
            size="small"
            type="primary"
            className="admin-btn-primary"
            loading={actionLoading}
            onClick={() => handleApprove(row)}
          >
            通过
          </Button>
          <Button
            size="small"
            className="admin-btn-secondary"
            onClick={() => openReform(row)}
          >
            要求整改
          </Button>
          <Popconfirm
            title="确认强制下架该物品？"
            okText="确定"
            cancelText="取消"
            onConfirm={() => handleForceOffShelf(row)}
          >
            <Button size="small" className="admin-btn-danger" loading={actionLoading}>
              强制下架
            </Button>
          </Popconfirm>
          <Link to={`/items/audits/${row.id}`} state={{ item: row }}>
            <Button size="small" className="admin-btn-secondary">
              详情
            </Button>
          </Link>
        </span>
      ),
    },
  ];

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
        <Radio.Group
          optionType="button"
          buttonStyle="solid"
          value={filter}
          onChange={(e) => setFilter(e.target.value as FilterValue)}
          options={FILTER_OPTIONS}
        />
      </section>

      <section className="admin-panel">
        <Table<ItemAuditItem>
          rowKey={(r) => r.id}
          columns={columns}
          dataSource={list}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          size="middle"
        />
      </section>

      <Modal
        title="要求整改"
        open={reformTarget !== null}
        okText="提交"
        cancelText="取消"
        confirmLoading={actionLoading}
        destroyOnHidden
        onOk={submitReform}
        onCancel={() => setReformTarget(null)}
      >
        <p className="admin-caption" style={{ marginBottom: 8 }}>
          物品：{reformTarget?.title ?? ''}（ID {reformTarget?.id ?? ''}）· 物主：
          {reformTarget?.ownerId ?? ''}
        </p>
        <Input.TextArea
          placeholder="请输入整改原因"
          value={reformReason}
          onChange={(e) => setReformReason(e.target.value)}
          maxLength={255}
          autoSize={{ minRows: 3, maxRows: 6 }}
        />
      </Modal>
    </>
  );
}

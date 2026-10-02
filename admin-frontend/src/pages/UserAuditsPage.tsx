import { useCallback, useEffect, useState } from 'react';
import { Button, Modal, Radio, Spin, Table, Input, Tooltip, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';
import { adminService } from '../services/admin';
import type { UserAuditItem } from '../types/api';
import AuditStatusBadge from '../components/AuditStatusBadge';

// 筛选项：'all' 表示全部，其余为审核状态数值的字符串形式
type FilterValue = 'all' | '0' | '1' | '2';

const FILTER_OPTIONS: { label: string; value: FilterValue }[] = [
  { label: '全部', value: 'all' },
  { label: '待审核', value: '0' },
  { label: '审核通过', value: '1' },
  { label: '要求整改', value: '2' },
];

const fieldNameText: Record<string, string> = {
  username: '用户名',
  avatar: '头像',
  description: '描述',
};

const renderEllipsis = (text?: string) => {
  const value = text && text.trim() ? text : '-';
  return (
    <Tooltip title={value === '-' ? undefined : value}>
      <span className="admin-table-ellipsis">{value}</span>
    </Tooltip>
  );
};

// 用户审核列表页：拉取 userAudits 列表，支持筛选与通过/要求整改操作
// 默认筛选"待审核"，让管理员一进页面就能看到需要处理的记录
export default function UserAuditsPage() {
  const [filter, setFilter] = useState<FilterValue>('0');
  const [list, setList] = useState<UserAuditItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  // 要求整改 Modal：当前操作的行 + 输入的原因
  const [reformTarget, setReformTarget] = useState<UserAuditItem | null>(null);
  const [reformReason, setReformReason] = useState('');

  const statusParam = filter === 'all' ? undefined : Number(filter);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminService.userAudits(statusParam);
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

  // 通过审核
  const handleApprove = async (row: UserAuditItem) => {
    setActionLoading(true);
    try {
      await adminService.auditUser(row.userId, {
        auditStatus: 1,
        fieldName: row.fieldName,
      });
      message.success('已通过审核');
      await load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  // 打开要求整改 Modal
  const openReform = (row: UserAuditItem) => {
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
      await adminService.auditUser(reformTarget.userId, {
        auditStatus: 2,
        auditReason: reason,
        fieldName: reformTarget.fieldName,
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

  const columns: ColumnsType<UserAuditItem> = [
    { title: '用户 ID', dataIndex: 'userId', key: 'userId', width: 100 },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    {
      title: '审核字段',
      dataIndex: 'fieldName',
      key: 'fieldName',
      width: 180,
      render: (_: string, row: UserAuditItem) => {
        const names = row.fieldNames && row.fieldNames.length > 0
            ? row.fieldNames
            : [row.fieldName];
        const text = names
            .map((f) => fieldNameText[f] ?? f)
            .join('、');
        return text;
      },
    },
    {
      title: '审核状态',
      dataIndex: 'auditStatus',
      key: 'auditStatus',
      render: (status: number) => <AuditStatusBadge auditStatus={status} />,
    },
    {
      title: '审核原因',
      dataIndex: 'reason',
      key: 'reason',
      width: 240,
      render: (reason?: string) => renderEllipsis(reason),
    },
    {
      title: '操作',
      key: 'action',
      width: 240,
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
          <Link to={`/users/audits/${row.userId}`} state={{ item: row }}>
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
        <Table<UserAuditItem>
          rowKey={(r) => r.userId}
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
          用户：{reformTarget?.username ?? ''}（ID {reformTarget?.userId ?? ''}）· 字段：
          {reformTarget?.fieldName ? fieldNameText[reformTarget.fieldName] ?? reformTarget.fieldName : ''}
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

import { useCallback, useEffect, useState } from 'react';
import { Button, Input, Modal, Radio, Spin, Table, Tag, Tooltip, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { adminService } from '../services/admin';
import type { Dispute } from '../types/api';
import { disputeStatusText } from '../types/status';
import DisputeDetailPanel from '../components/DisputeDetailPanel';

// 筛选项：'all' 表示全部，其余为异议状态数值的字符串形式
type FilterValue = 'all' | '0' | '1' | '2';

const FILTER_OPTIONS: { label: string; value: FilterValue }[] = [
  { label: '全部', value: 'all' },
  { label: '待处理', value: '0' },
  { label: '处理中', value: '1' },
  { label: '已裁定', value: '2' },
];

// 状态 → Tag 颜色映射：0 待处理=橙、1 处理中=蓝、2 已裁定=绿
const statusColor: Record<number, string> = {
  0: 'orange',
  1: 'blue',
  2: 'green',
};

const formatTime = (raw?: string) => {
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

// 异议审核列表页：拉取 disputes 列表，支持按状态筛选与裁定操作。
// 后端约定：resolveDispute body 只含 adminRemark，adminId 由网关 X-User-Id 头注入。
// 默认筛选"待处理"，让管理员一进页面就能看到需要处理的异议
export default function DisputesPage() {
  const [filter, setFilter] = useState<FilterValue>('0');
  const [list, setList] = useState<Dispute[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // 详情抽屉：当前选中的异议
  const [selected, setSelected] = useState<Dispute | null>(null);
  // 裁定 Modal：当前操作的异议 + 输入的备注
  const [resolveTarget, setResolveTarget] = useState<Dispute | null>(null);
  const [adminRemark, setAdminRemark] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const statusParam = filter === 'all' ? undefined : Number(filter);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await adminService.disputes(statusParam);
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

  // 提交裁定：body 只含 adminRemark，不含 adminId
  const submitResolve = async () => {
    if (!resolveTarget) return;
    setActionLoading(true);
    try {
      await adminService.resolveDispute(resolveTarget.id, adminRemark.trim() || undefined);
      message.success('已裁定');
      setResolveTarget(null);
      setAdminRemark('');
      setSelected(null);
      await load();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const columns: ColumnsType<Dispute> = [
    { title: '异议 ID', dataIndex: 'id', key: 'id', width: 90 },
    { title: '订单 ID', dataIndex: 'orderId', key: 'orderId', width: 90 },
    { title: '申请人 ID', dataIndex: 'applicantId', key: 'applicantId', width: 100 },
    {
      title: '原因',
      dataIndex: 'reason',
      key: 'reason',
      width: 240,
      render: (reason?: string) => renderEllipsis(reason),
    },
    {
      title: '期望扣除押金',
      dataIndex: 'expectedDepositDeduction',
      key: 'expectedDepositDeduction',
      width: 130,
      render: (v?: number) => (v != null ? `¥${v}` : '-'),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => (
        <Tag color={statusColor[status] ?? 'default'}>
          {disputeStatusText[status] ?? `状态: ${status}`}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      render: (raw?: string) => formatTime(raw),
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      render: (raw?: string) => formatTime(raw),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_, row) => {
        const canResolve = row.status === 0 || row.status === 1;
        return (
          <span style={{ display: 'inline-flex', gap: 8 }}>
            <Button
              size="small"
              className="admin-btn-secondary"
              onClick={() => setSelected(row)}
            >
              查看详情
            </Button>
            {canResolve && (
              <Button
                size="small"
                type="primary"
                className="admin-btn-primary"
                onClick={() => {
                  setResolveTarget(row);
                  setAdminRemark('');
                }}
              >
                裁定
              </Button>
            )}
          </span>
        );
      },
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
        <Table<Dispute>
          rowKey={(r) => r.id}
          columns={columns}
          dataSource={list}
          pagination={{ pageSize: 20, showSizeChanger: false }}
          size="middle"
        />
      </section>

      <DisputeDetailPanel
        dispute={selected}
        open={selected !== null}
        onClose={() => setSelected(null)}
        onResolve={(d) => {
          setResolveTarget(d);
          setAdminRemark('');
        }}
      />

      <Modal
        title="裁定异议"
        open={resolveTarget !== null}
        okText="提交"
        cancelText="取消"
        confirmLoading={actionLoading}
        destroyOnHidden
        onOk={submitResolve}
        onCancel={() => setResolveTarget(null)}
      >
        <p className="admin-caption" style={{ marginBottom: 8 }}>
          异议 #{resolveTarget?.id ?? ''} · 订单 {resolveTarget?.orderId ?? ''}
        </p>
        <Input.TextArea
          placeholder="请输入裁定备注"
          value={adminRemark}
          onChange={(e) => setAdminRemark(e.target.value)}
          maxLength={500}
          autoSize={{ minRows: 3, maxRows: 6 }}
        />
      </Modal>
    </>
  );
}

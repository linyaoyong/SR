import { Table, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { AdminLog } from '../types/api';
import { operationTypeText, targetTypeText } from '../types/status';

// 时间格式化：无效时回退原值
const formatTime = (raw?: string) => {
  if (!raw) return '-';
  const d = dayjs(raw);
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm:ss') : raw;
};

// 可空字段统一展示为 -，避免空白
const orDash = (v?: string) => (v && v.trim() !== '' ? v : '-');
const mappedOrRaw = (value: string | undefined, map: Record<string, string>) =>
  value ? map[value] ?? value : '-';
const ellipsisText = (value?: string) => {
  const text = orDash(value);
  return (
    <Tooltip title={text === '-' ? undefined : text}>
      <span className="admin-table-ellipsis">{text}</span>
    </Tooltip>
  );
};

const columns: ColumnsType<AdminLog> = [
  { title: '日志 ID', dataIndex: 'id', key: 'id', width: 90 },
  { title: '管理员 ID', dataIndex: 'adminId', key: 'adminId', width: 100 },
  {
    title: '操作类型',
    dataIndex: 'operationType',
    key: 'operationType',
    width: 140,
    render: (v?: string) => mappedOrRaw(v, operationTypeText),
  },
  {
    title: '目标类型',
    dataIndex: 'targetType',
    key: 'targetType',
    width: 110,
    render: (v?: string) => mappedOrRaw(v, targetTypeText),
  },
  { title: '目标 ID', dataIndex: 'targetId', key: 'targetId', width: 100 },
  {
    title: '备注',
    dataIndex: 'remark',
    key: 'remark',
    width: 260,
    render: (v?: string) => ellipsisText(v),
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    render: (raw?: string) => formatTime(raw),
  },
];

// 操作日志表格：纯展示组件，由父页面控制数据与分页。
export interface OperationLogTableProps {
  logs: AdminLog[];
  loading?: boolean;
}

export default function OperationLogTable({ logs, loading }: OperationLogTableProps) {
  return (
    <Table<AdminLog>
      rowKey="id"
      columns={columns}
      dataSource={logs}
      loading={loading}
      pagination={false}
      size="middle"
      locale={{ emptyText: '暂无日志' }}
    />
  );
}

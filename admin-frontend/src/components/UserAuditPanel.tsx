import { useState } from 'react';
import { Button, Descriptions, Input, Modal, Popconfirm, message } from 'antd';
import { adminService } from '../services/admin';
import { auditStatusText } from '../types/status';
import type { UserAuditItem } from '../types/api';
import AuditStatusBadge from './AuditStatusBadge';

interface UserAuditPanelProps {
  item: UserAuditItem;
  // 审核操作（通过/要求整改）成功后的回调（例如返回上一页）
  onChanged?: () => void;
  // 封禁/解封成功后的回调（用于刷新当前详情数据，不跳转）
  onRefresh?: () => void | Promise<void>;
}

// fieldName 中文映射
const fieldNameText: Record<string, string> = {
  username: '用户名',
  avatar: '头像',
  description: '描述',
};

// 用户状态映射：0=正常 1=已封禁
const userStatusText: Record<number, string> = {
  0: '正常',
  1: '已封禁',
};

const userStatusBadgeClass: Record<number, string> = {
  0: 'admin-badge--success',
  1: 'admin-badge--error',
};

// 用户审核详情面板：展示完整用户资料（含三字段审核总览）并提供
// 通过/要求整改/封禁/解封操作。后端列表响应已携带完整 profile，直接使用。
export default function UserAuditPanel({ item, onChanged, onRefresh }: UserAuditPanelProps) {
  const [actionLoading, setActionLoading] = useState(false);
  const [reformOpen, setReformOpen] = useState(false);
  const [reformReason, setReformReason] = useState('');
  const [banOpen, setBanOpen] = useState(false);
  const [banReason, setBanReason] = useState('');

  const handleApprove = async () => {
    setActionLoading(true);
    try {
      await adminService.auditUser(item.userId, {
        auditStatus: 1,
        fieldName: item.fieldName,
      });
      message.success('已通过审核');
      onChanged?.();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const submitReform = async () => {
    const reason = reformReason.trim();
    if (!reason) {
      message.warning('请输入整改原因');
      return;
    }
    setActionLoading(true);
    try {
      await adminService.auditUser(item.userId, {
        auditStatus: 2,
        auditReason: reason,
        fieldName: item.fieldName,
      });
      message.success('已要求整改');
      setReformOpen(false);
      setReformReason('');
      onChanged?.();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const submitBan = async () => {
    const reason = banReason.trim();
    setActionLoading(true);
    try {
      await adminService.banUser(item.userId, reason || undefined);
      message.success('已封禁用户，可到操作日志追踪');
      setBanOpen(false);
      setBanReason('');
      await onRefresh?.();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUnban = async () => {
    setActionLoading(true);
    try {
      await adminService.unbanUser(item.userId);
      message.success('已解封用户');
      await onRefresh?.();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  // 三字段审核状态总览条目
  const fieldStatusEntries: { label: string; status?: number }[] = [
    { label: '用户名', status: item.usernameAuditStatus },
    { label: '头像', status: item.avatarAuditStatus },
    { label: '描述', status: item.descriptionAuditStatus },
  ];

  const userStatus = item.status ?? 0;
  const showRentalHistoryText =
    item.showRentalHistory == null ? '-' : item.showRentalHistory === 1 ? '公开' : '不公开';

  return (
    <section className="admin-panel">
      <Descriptions title="用户资料审核详情" column={2} bordered size="small">
        <Descriptions.Item label="用户头像" span={2}>
          {item.avatarUrl ? (
            <img
              src={item.avatarUrl}
              alt="用户头像"
              style={{
                width: 96,
                height: 96,
                borderRadius: 'var(--admin-radius)',
                objectFit: 'cover',
                display: 'block',
              }}
            />
          ) : (
            <span className="admin-caption">未上传</span>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="用户 ID">{item.userId}</Descriptions.Item>
        <Descriptions.Item label="用户名">{item.username}</Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>
          {item.description ? item.description : <span className="admin-caption">未填写</span>}
        </Descriptions.Item>
        <Descriptions.Item label="信用分">
          {item.creditScore != null ? item.creditScore : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="用户状态">
          <span className={`admin-badge ${userStatusBadgeClass[userStatus] ?? 'admin-badge--muted'}`}>
            {userStatusText[userStatus] ?? `状态: ${userStatus}`}
          </span>
        </Descriptions.Item>
        <Descriptions.Item label="是否公开租赁历史">{showRentalHistoryText}</Descriptions.Item>
        <Descriptions.Item label="当前审核字段">
          {fieldNameText[item.fieldName] ?? item.fieldName}
        </Descriptions.Item>
        <Descriptions.Item label="该字段审核状态" span={2}>
          <AuditStatusBadge auditStatus={item.auditStatus} />
          {item.reason ? (
            <span className="admin-caption" style={{ marginLeft: 8 }}>
              原因：{item.reason}
            </span>
          ) : null}
        </Descriptions.Item>
        <Descriptions.Item label="三字段审核总览" span={2}>
          <span style={{ display: 'inline-flex', gap: 8, flexWrap: 'wrap' }}>
            {fieldStatusEntries.map((entry) => {
              const status = entry.status;
              const text =
                status != null ? auditStatusText[status] ?? `状态: ${status}` : '未提交';
              const cls =
                status != null
                  ? status === 1
                    ? 'admin-badge--success'
                    : status === 2
                      ? 'admin-badge--warning'
                      : 'admin-badge--muted'
                  : 'admin-badge--muted';
              return (
                <span key={entry.label} className={`admin-badge ${cls}`}>
                  {entry.label}：{text}
                </span>
              );
            })}
          </span>
        </Descriptions.Item>
      </Descriptions>

      <div style={{ marginTop: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <Button
          type="primary"
          className="admin-btn-primary"
          loading={actionLoading}
          onClick={handleApprove}
        >
          通过
        </Button>
        <Button
          className="admin-btn-secondary"
          onClick={() => setReformOpen(true)}
        >
          要求整改
        </Button>
        {userStatus === 0 ? (
          <Button
            className="admin-btn-danger"
            loading={actionLoading}
            onClick={() => setBanOpen(true)}
          >
            封禁此用户
          </Button>
        ) : null}
        {userStatus === 1 ? (
          <Popconfirm
            title="确认解封该用户？"
            okText="确定"
            cancelText="取消"
            onConfirm={handleUnban}
          >
            <Button className="admin-btn-primary" loading={actionLoading}>
              解封此用户
            </Button>
          </Popconfirm>
        ) : null}
      </div>

      <Modal
        title="要求整改"
        open={reformOpen}
        okText="提交"
        cancelText="取消"
        confirmLoading={actionLoading}
        destroyOnHidden
        onOk={submitReform}
        onCancel={() => setReformOpen(false)}
      >
        <p className="admin-caption" style={{ marginBottom: 8 }}>
          用户：{item.username}（ID {item.userId}）· 字段：
          {fieldNameText[item.fieldName] ?? item.fieldName}
        </p>
        <Input.TextArea
          placeholder="请输入整改原因"
          value={reformReason}
          onChange={(e) => setReformReason(e.target.value)}
          maxLength={255}
          autoSize={{ minRows: 3, maxRows: 6 }}
        />
      </Modal>

      <Modal
        title="封禁用户"
        open={banOpen}
        okText="确认封禁"
        cancelText="取消"
        confirmLoading={actionLoading}
        destroyOnHidden
        onOk={submitBan}
        onCancel={() => setBanOpen(false)}
      >
        <p className="admin-caption" style={{ marginBottom: 8 }}>
          用户：{item.username}（ID {item.userId}）
        </p>
        <Input.TextArea
          placeholder="请输入封禁原因（可选）"
          value={banReason}
          onChange={(e) => setBanReason(e.target.value)}
          maxLength={255}
          autoSize={{ minRows: 3, maxRows: 6 }}
        />
      </Modal>
    </section>
  );
}

import { useState } from 'react';
import { Button, Descriptions, Image, Input, Modal, Popconfirm, Tag, Typography, message } from 'antd';
import dayjs from 'dayjs';
import { adminService } from '../services/admin';
import type { ItemAuditItem } from '../types/api';
import AuditStatusBadge from './AuditStatusBadge';

// 物品状态映射（详情页用）：0=待上架 1=已上架 2=已下架 3=强制下架
const itemStatusText: Record<number, string> = {
  0: '待上架',
  1: '已上架',
  2: '已下架',
  3: '强制下架',
};

const itemStatusBadgeClass: Record<number, string> = {
  0: 'admin-badge--muted',
  1: 'admin-badge--success',
  2: 'admin-badge--warning',
  3: 'admin-badge--error',
};

// 计价方式映射：0=免费 1=按天
const priceTypeText: Record<number, string> = {
  0: '免费',
  1: '按天',
};

const formatAuditTime = (raw?: string) => {
  if (!raw) return '-';
  const d = dayjs(raw);
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : raw;
};

// 按英文/中文逗号或空白分割标签
const splitTags = (raw?: string): string[] => {
  if (!raw) return [];
  return raw
    .split(/[,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean);
};

interface ItemAuditPanelProps {
  item: ItemAuditItem;
  // 操作成功后的回调（例如刷新或返回上一页）
  onChanged?: () => void;
}

// 物品审核详情面板：展示完整物品资料（图片+文字）并提供通过/要求整改/强制下架操作。
// 后端列表响应已携带完整物品资料，直接使用，不调用单独详情接口。
export default function ItemAuditPanel({ item, onChanged }: ItemAuditPanelProps) {
  const [actionLoading, setActionLoading] = useState(false);
  const [reformOpen, setReformOpen] = useState(false);
  const [reformReason, setReformReason] = useState('');

  // 通过审核：body 不含 auditReason，不含 fieldName
  const handleApprove = async () => {
    setActionLoading(true);
    try {
      const result = await adminService.auditItem(item.id, { auditStatus: 1 });
      const timeText = result?.auditTime ? formatAuditTime(result.auditTime) : '';
      message.success(timeText ? `已通过审核，时间 ${timeText}` : '已通过审核');
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
      await adminService.auditItem(item.id, {
        auditStatus: 2,
        auditReason: reason,
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

  const handleForceOffShelf = async () => {
    setActionLoading(true);
    try {
      await adminService.forceOffShelf(item.id);
      message.success('已强制下架');
      onChanged?.();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActionLoading(false);
    }
  };

  const imageUrls = item.imageUrls ?? [];
  const tags = splitTags(item.tags);
  const isDaily = item.priceType === 1;

  return (
    <section className="admin-panel">
      {/* 图片放最顶部，审核员先看图 */}
      <div style={{ marginBottom: 16 }}>
        <p className="admin-caption" style={{ marginBottom: 8 }}>
          物品图片
        </p>
        {imageUrls.length > 0 ? (
          <Image.PreviewGroup>
            <span style={{ display: 'inline-flex', gap: 8, flexWrap: 'wrap' }}>
              {imageUrls.map((url, idx) => (
                <Image
                  key={`${url}-${idx}`}
                  src={url}
                  alt={`物品图片 ${idx + 1}`}
                  width={96}
                  height={96}
                  style={{
                    borderRadius: 'var(--admin-radius)',
                    objectFit: 'cover',
                  }}
                />
              ))}
            </span>
          </Image.PreviewGroup>
        ) : (
          <span className="admin-caption">未上传图片</span>
        )}
      </div>

      <Descriptions title="物品内容审核详情" column={2} bordered size="small">
        <Descriptions.Item label="物品 ID">{item.id}</Descriptions.Item>
        <Descriptions.Item label="标题">{item.title}</Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>
          {item.description ? (
            <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
              {item.description}
            </Typography.Paragraph>
          ) : (
            <span className="admin-caption">未填写描述</span>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="分类">
          {item.categoryName ? item.categoryName : <span className="admin-caption">未分类</span>}
        </Descriptions.Item>
        <Descriptions.Item label="物主 ID">{item.ownerId}</Descriptions.Item>
        <Descriptions.Item label="标签" span={2}>
          {tags.length > 0 ? (
            <span style={{ display: 'inline-flex', gap: 4, flexWrap: 'wrap' }}>
              {tags.map((tag) => (
                <Tag key={tag}>{tag}</Tag>
              ))}
            </span>
          ) : (
            <span className="admin-caption">无标签</span>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="计价方式">
          {item.priceType != null ? priceTypeText[item.priceType] ?? `类型: ${item.priceType}` : '-'}
        </Descriptions.Item>
        {isDaily ? (
          <Descriptions.Item label="日租金">
            {item.dailyPrice != null ? `¥${item.dailyPrice}/天` : '-'}
          </Descriptions.Item>
        ) : null}
        <Descriptions.Item label="押金">
          {item.depositAmount != null ? `¥${item.depositAmount}` : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="数量">
          {item.quantity != null ? item.quantity : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="物品状态">
          <span
            className={`admin-badge ${itemStatusBadgeClass[item.status] ?? 'admin-badge--muted'}`}
          >
            {itemStatusText[item.status] ?? `状态: ${item.status}`}
          </span>
        </Descriptions.Item>
        <Descriptions.Item label="审核状态">
          <AuditStatusBadge auditStatus={item.auditStatus} />
        </Descriptions.Item>
        <Descriptions.Item label="审核原因" span={2}>
          {item.auditReason ? item.auditReason : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="审核时间">{formatAuditTime(item.auditTime)}</Descriptions.Item>
        <Descriptions.Item label="审核管理员">
          {item.auditAdminId != null ? item.auditAdminId : '-'}
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
        <Button className="admin-btn-secondary" onClick={() => setReformOpen(true)}>
          要求整改
        </Button>
        <Popconfirm
          title="确认强制下架该物品？"
          okText="确定"
          cancelText="取消"
          onConfirm={handleForceOffShelf}
        >
          <Button className="admin-btn-danger" loading={actionLoading}>
            强制下架
          </Button>
        </Popconfirm>
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
          物品：{item.title}（ID {item.id}）· 物主：{item.ownerId}
        </p>
        <Input.TextArea
          placeholder="请输入整改原因"
          value={reformReason}
          onChange={(e) => setReformReason(e.target.value)}
          maxLength={255}
          autoSize={{ minRows: 3, maxRows: 6 }}
        />
      </Modal>
    </section>
  );
}

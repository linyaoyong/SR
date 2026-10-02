import { Button, Descriptions, Drawer, Image } from 'antd';
import dayjs from 'dayjs';
import type { Dispute } from '../types/api';
import { disputeStatusText } from '../types/status';

const formatTime = (raw?: string) => {
  if (!raw) return '-';
  const d = dayjs(raw);
  return d.isValid() ? d.format('YYYY-MM-DD HH:mm') : raw;
};

// 防御性解析 imageUrls：后端返回 JSON 数组字符串，解析失败时返回空数组
const parseImageUrls = (raw?: string): string[] => {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed.filter((u): u is string => typeof u === 'string');
    }
  } catch {
    // ignore：imageUrls 不是合法 JSON 时按无图片处理
  }
  return [];
};

interface DisputeDetailPanelProps {
  dispute: Dispute | null;
  open: boolean;
  onClose: () => void;
  // 裁定回调：交由父组件统一打开裁定 Modal，避免详情面板与列表各自维护 state
  onResolve?: (dispute: Dispute) => void;
}

// 异议详情抽屉：展示 Dispute 全部字段（含 imageUrls 缩略图），
// 仅在 status 0/1 时显示"裁定"按钮，点击后通过 onResolve 交父组件处理。
export default function DisputeDetailPanel({
  dispute,
  open,
  onClose,
  onResolve,
}: DisputeDetailPanelProps) {
  const images = dispute ? parseImageUrls(dispute.imageUrls) : [];
  const canResolve = !!dispute && (dispute.status === 0 || dispute.status === 1);

  return (
    <Drawer
      title={dispute ? `异议详情 #${dispute.id}` : '异议详情'}
      open={open}
      onClose={onClose}
      width={560}
      destroyOnHidden
    >
      {dispute && (
        <>
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="异议 ID">{dispute.id}</Descriptions.Item>
            <Descriptions.Item label="订单 ID">{dispute.orderId}</Descriptions.Item>
            <Descriptions.Item label="申请人 ID">{dispute.applicantId}</Descriptions.Item>
            <Descriptions.Item label="原因">{dispute.reason}</Descriptions.Item>
            <Descriptions.Item label="描述">{dispute.description || '-'}</Descriptions.Item>
            <Descriptions.Item label="期望扣除押金">
              {dispute.expectedDepositDeduction != null
                ? `¥${dispute.expectedDepositDeduction}`
                : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              {disputeStatusText[dispute.status] ?? `状态: ${dispute.status}`}
            </Descriptions.Item>
            <Descriptions.Item label="图片">
              {images.length === 0 ? (
                <span className="admin-caption">无图片</span>
              ) : (
                <Image.PreviewGroup>
                  {images.map((url) => (
                    <Image
                      key={url}
                      src={url}
                      width={80}
                      height={80}
                      style={{ objectFit: 'cover', marginRight: 8 }}
                    />
                  ))}
                </Image.PreviewGroup>
              )}
            </Descriptions.Item>
            <Descriptions.Item label="处理管理员">
              {dispute.adminId != null ? dispute.adminId : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="管理员备注">
              {dispute.adminRemark || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatTime(dispute.createTime)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{formatTime(dispute.updateTime)}</Descriptions.Item>
          </Descriptions>

          {canResolve && onResolve && (
            <div style={{ marginTop: 16 }}>
              <Button
                type="primary"
                className="admin-btn-primary"
                onClick={() => onResolve(dispute)}
              >
                裁定
              </Button>
            </div>
          )}
        </>
      )}
    </Drawer>
  );
}

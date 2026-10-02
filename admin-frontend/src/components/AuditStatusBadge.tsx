import { auditStatusText } from '../types/status';

// 审核状态 → 徽标颜色 class 映射
// 0 待审核=灰、1 审核通过=绿、2 要求整改=橙
const badgeClassByStatus: Record<number, string> = {
  0: 'admin-badge--muted',
  1: 'admin-badge--success',
  2: 'admin-badge--warning',
};

interface AuditStatusBadgeProps {
  auditStatus: number;
}

// 审核状态徽标：用户审核与物品审核复用
export default function AuditStatusBadge({ auditStatus }: AuditStatusBadgeProps) {
  const text = auditStatusText[auditStatus] ?? '未知';
  const cls = badgeClassByStatus[auditStatus] ?? 'admin-badge--muted';
  return <span className={`admin-badge ${cls}`}>{text}</span>;
}

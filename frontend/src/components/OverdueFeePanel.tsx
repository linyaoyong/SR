import { Alert } from 'antd';
import { Link } from 'react-router-dom';

interface OverdueFeePanelProps {
  overdueMinutes: number;
  overdueFeeAmount: number;
  overdueSettled: number; // 0 未结算 / 1 已结算（docs/03-api-contract.md 第 9.2 节）
}

// 订单逾期费用面板。字段对齐 docs/03-api-contract.md 第 9.2 节 RentalOrderResponse。
export default function OverdueFeePanel({
  overdueMinutes,
  overdueFeeAmount,
  overdueSettled,
}: OverdueFeePanelProps) {
  const hasOverdue = overdueMinutes > 0 || overdueFeeAmount > 0;
  if (!hasOverdue && overdueSettled === 0) {
    return null;
  }

  const settled = overdueSettled === 1;
  const hours = Math.floor(overdueMinutes / 60);
  const mins = overdueMinutes % 60;
  const durationText =
    overdueMinutes <= 0 ? '无' : hours > 0 ? `${hours} 小时 ${mins} 分钟` : `${mins} 分钟`;

  return (
    <section className="sr-card sr-overdue-panel" aria-label="逾期费用面板">
      <h3 className="sr-overdue-panel-title">逾期费用</h3>
      <dl className="sr-overdue-panel-grid">
        <div>
          <dt>逾期时长</dt>
          <dd>{durationText}</dd>
        </div>
        <div>
          <dt>逾期费用</dt>
          <dd>¥{overdueFeeAmount}</dd>
        </div>
        <div>
          <dt>结算状态</dt>
          <dd>{settled ? '已结算' : '未结算'}</dd>
        </div>
      </dl>
      {!settled && overdueFeeAmount > 0 && (
        <Alert
          type="warning"
          showIcon
          message="逾期费用尚未结算，结算前请保持钱包余额充足。"
          action={
            <Link to="/wallet" className="sr-btn sr-btn-sm sr-btn-primary">
              去充值
            </Link>
          }
        />
      )}
    </section>
  );
}

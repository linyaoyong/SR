import { useEffect, useState } from 'react';
import { Alert, Button, InputNumber, message } from 'antd';
import { Link } from 'react-router-dom';
import { rentalService } from '../services/rental';

interface PaymentPanelProps {
  orderId: number;
  rentAmount: number;
  paidRentAmount: number;
  depositAmount: number;
  frozenDepositAmount: number;
  onUpdated: (order: unknown) => void;
  // 是否展示支付租金 / 冻结押金 / 去充值 等操作入口；出租者只读，不操作。
  canOperate?: boolean;
}

// 订单付款面板：展示租金/押金进度与待付金额，并触发支付租金 / 冻结押金。
// 字段对齐 docs/03-api-contract.md 第 10.1、10.2 节。
export default function PaymentPanel({
  orderId,
  rentAmount,
  paidRentAmount,
  depositAmount,
  frozenDepositAmount,
  onUpdated,
  canOperate = true,
}: PaymentPanelProps) {
  const [payAmount, setPayAmount] = useState<number | null>(rentAmount - paidRentAmount);
  const [freezeAmount, setFreezeAmount] = useState<number | null>(
    depositAmount - frozenDepositAmount,
  );
  const [submitting, setSubmitting] = useState<'pay' | 'freeze' | null>(null);
  const [warning, setWarning] = useState<string | null>(null);

  const unpaidRent = Math.max(0, rentAmount - paidRentAmount);
  const unfrozenDeposit = Math.max(0, depositAmount - frozenDepositAmount);

  // After a successful pay/freeze the parent refreshes the order and these
  // deltas change; sync the input fields to the new outstanding amounts so
  // the user doesn't see a stale value from before the action.
  useEffect(() => {
    setPayAmount(unpaidRent);
  }, [unpaidRent]);
  useEffect(() => {
    setFreezeAmount(unfrozenDeposit);
  }, [unfrozenDeposit]);

  const run = async (
    kind: 'pay' | 'freeze',
    amount: number | null,
    action: (id: number, amount: number) => Promise<unknown>,
  ) => {
    if (!amount || amount <= 0) {
      message.warning('请输入大于 0 的金额');
      return;
    }
    setSubmitting(kind);
    setWarning(null);
    try {
      const updated = await action(orderId, amount);
      message.success(kind === 'pay' ? '已支付租金' : '已冻结押金');
      onUpdated(updated);
    } catch (err) {
      const msg = err instanceof Error ? err.message : '操作失败';
      // 余额不足 / 钱包欠费相关错误提示用户去充值。
      if (/余额不足|欠费|钱包/.test(msg)) {
        setWarning(msg);
      } else {
        message.error(msg);
      }
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <section className="sr-card sr-payment-panel" aria-label="订单付款面板">
      <h3 className="sr-payment-panel-title">租金与押金</h3>
      <dl className="sr-payment-panel-grid">
        <div>
          <dt>租金总额</dt>
          <dd>¥{rentAmount}</dd>
        </div>
        <div>
          <dt>已付租金</dt>
          <dd>¥{paidRentAmount}</dd>
        </div>
        <div>
          <dt>待付租金</dt>
          <dd>
            <span>¥{unpaidRent}</span>
          </dd>
        </div>
        <div>
          <dt>押金金额</dt>
          <dd>¥{depositAmount}</dd>
        </div>
        <div>
          <dt>已冻结押金</dt>
          <dd>¥{frozenDepositAmount}</dd>
        </div>
        <div>
          <dt>待冻结押金</dt>
          <dd>
            <span>¥{unfrozenDeposit}</span>
          </dd>
        </div>
      </dl>
      {canOperate && (
        <div className="sr-payment-panel-actions">
          <div className="sr-payment-panel-action">
            <label htmlFor="sr-pay-amount">支付金额</label>
            <InputNumber
              id="sr-pay-amount"
              aria-label="支付金额"
              value={payAmount}
              min={0.01}
              precision={2}
              style={{ width: '100%' }}
              onChange={(value) => setPayAmount(typeof value === 'number' ? value : null)}
            />
            <Button
              type="primary"
              loading={submitting === 'pay'}
              onClick={() => run('pay', payAmount, rentalService.payRent)}
            >
              支付租金
            </Button>
          </div>
          <div className="sr-payment-panel-action">
            <label htmlFor="sr-freeze-amount">冻结金额</label>
            <InputNumber
              id="sr-freeze-amount"
              aria-label="冻结金额"
              value={freezeAmount}
              min={0.01}
              precision={2}
              style={{ width: '100%' }}
              onChange={(value) => setFreezeAmount(typeof value === 'number' ? value : null)}
            />
            <Button
              loading={submitting === 'freeze'}
              onClick={() => run('freeze', freezeAmount, rentalService.freezeDeposit)}
            >
              冻结押金
            </Button>
          </div>
          <Link to="/wallet" className="sr-btn sr-btn-md sr-btn-secondary sr-payment-panel-recharge">
            去充值
          </Link>
        </div>
      )}
      {warning && (
        <Alert
          type="warning"
          showIcon
          message={warning}
          action={
            <Link to="/wallet" className="sr-btn sr-btn-sm sr-btn-primary">
              去充值
            </Link>
          }
          style={{ marginTop: 'var(--space-3)' }}
        />
      )}
    </section>
  );
}

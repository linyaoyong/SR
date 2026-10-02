import { useState } from 'react';
import { Button, InputNumber } from 'antd';
import type { WalletAccount } from '../types/api';

interface WalletSummaryProps {
  wallet: WalletAccount;
  onRecharge?: (amount: number) => void;
  rechargeLoading?: boolean;
}

// 钱包概览：余额 / 冻结 / 欠费提示 + 模拟充值表单。
// 字段对齐 docs/03-api-contract.md wallet-service 与 docs/04-database-design.md wallet_accounts。
export default function WalletSummary({
  wallet,
  onRecharge,
  rechargeLoading,
}: WalletSummaryProps) {
  const [amount, setAmount] = useState<number | null>(100);
  const inDebt = wallet.balance <= 0;

  const handleRecharge = () => {
    if (!amount || amount <= 0) return;
    onRecharge?.(amount);
  };

  return (
    <section className="sr-card" aria-label="钱包概览" style={{ padding: 'var(--space-5)' }}>
      <h3 style={{ margin: '0 0 var(--space-4)', font: 'var(--font-display)' }}>钱包余额</h3>
      {inDebt && (
        <div className="sr-alert sr-alert--danger" role="alert" style={{ marginBottom: 'var(--space-4)' }}>
          钱包余额不足，欠费状态下无法发起新的租借交易，请尽快充值。
        </div>
      )}
      <dl
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
          gap: 'var(--space-4)',
          margin: '0 0 var(--space-5)',
        }}
      >
        <div>
          <dt style={{ color: 'var(--color-text-muted)' }}>可用余额</dt>
          <dd style={{ margin: 0, fontSize: 20, fontWeight: 600, color: inDebt ? '#991b1b' : undefined }}>
            ¥{wallet.balance}
          </dd>
        </div>
        <div>
          <dt style={{ color: 'var(--color-text-muted)' }}>冻结金额</dt>
          <dd style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>¥{wallet.frozenAmount}</dd>
        </div>
        <div>
          <dt style={{ color: 'var(--color-text-muted)' }}>账户状态</dt>
          <dd style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>
            {wallet.status === 1 ? '欠费' : '正常'}
          </dd>
        </div>
      </dl>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-3)', alignItems: 'flex-end' }}>
        <div style={{ flex: '1 1 200px' }}>
          <label htmlFor="sr-recharge-amount" style={{ display: 'block', marginBottom: 'var(--space-2)' }}>
            模拟充值金额
          </label>
          <InputNumber
            id="sr-recharge-amount"
            aria-label="模拟充值金额"
            value={amount}
            min={0.01}
            precision={2}
            style={{ width: '100%' }}
            onChange={(value) => setAmount(typeof value === 'number' ? value : null)}
          />
        </div>
        <Button type="primary" loading={rechargeLoading} onClick={handleRecharge}>
          模拟充值
        </Button>
      </div>
    </section>
  );
}

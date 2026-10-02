import { Link } from 'react-router-dom';
import { Pagination, Spin } from 'antd';
import { walletTransactionDirectionText, walletTransactionTypeText } from '../types/status';
import type { WalletTransaction } from '../types/api';

interface TransactionListProps {
  transactions: WalletTransaction[];
  loading: boolean;
  page: number;
  size: number;
  total: number;
  onPageChange: (page: number) => void;
}

// 钱包流水列表。字段对齐 docs/03-api-contract.md 第 10.4 节。
export default function TransactionList({
  transactions,
  loading,
  page,
  size,
  total,
  onPageChange,
}: TransactionListProps) {
  if (loading) {
    return (
      <div className="sr-detail-loading">
        <Spin />
      </div>
    );
  }

  if (transactions.length === 0) {
    return <div className="sr-empty">暂无流水记录</div>;
  }

  return (
    <section className="sr-card" aria-label="钱包流水" style={{ padding: 'var(--space-5)' }}>
      <h3 style={{ margin: '0 0 var(--space-4)', font: 'var(--font-display)' }}>钱包流水</h3>
      <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
        {transactions.map((tx) => {
          const directionText = walletTransactionDirectionText[tx.direction] ?? '-';
          const isIncome = tx.direction === 1;
          const isExpense = tx.direction === 2;
          return (
            <li
              key={tx.id}
              style={{
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-card)',
                padding: 'var(--space-3) var(--space-4)',
                display: 'flex',
                flexWrap: 'wrap',
                gap: 'var(--space-3)',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
                <div style={{ display: 'flex', gap: 'var(--space-3)', alignItems: 'center' }}>
                  <span style={{ fontWeight: 600 }}>
                    {walletTransactionTypeText[tx.type] ?? '未知'}
                  </span>
                  <span
                    style={{
                      padding: '0 var(--space-2)',
                      borderRadius: 4,
                      fontSize: 12,
                      color: isIncome ? '#166534' : isExpense ? '#991b1b' : undefined,
                      background: isIncome ? '#f0fdf4' : isExpense ? '#fef2f2' : undefined,
                    }}
                  >
                    {directionText}
                  </span>
                </div>
                <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>
                  流水号：{tx.transactionNo}
                  {tx.remark ? ` · 备注：${tx.remark}` : ''}
                </div>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 'var(--space-1)' }}>
                <span
                  style={{
                    fontWeight: 600,
                    color: isIncome ? '#166534' : isExpense ? '#991b1b' : undefined,
                  }}
                >
                  {isIncome ? '+' : isExpense ? '-' : ''}
                  ¥{tx.amount}
                </span>
                <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>
                  余额 ¥{tx.balanceAfter}
                </span>
                {tx.createTime && (
                  <span style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>{tx.createTime}</span>
                )}
                {tx.orderId && (
                  <Link to={`/orders/${tx.orderId}`} style={{ fontSize: 12 }}>
                    关联订单 #{tx.orderId}
                  </Link>
                )}
              </div>
            </li>
          );
        })}
      </ul>
      {total > size && (
        <div className="sr-items-pagination">
          <Pagination
            current={page}
            total={total}
            pageSize={size}
            onChange={onPageChange}
            showSizeChanger={false}
          />
        </div>
      )}
    </section>
  );
}

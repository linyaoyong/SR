import { useCallback, useEffect, useState } from 'react';
import { Button, Spin, message } from 'antd';
import { RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import TransactionList from '../components/TransactionList';
import WalletSummary from '../components/WalletSummary';
import { walletService } from '../services/wallet';
import { useWalletStore } from '../stores/walletStore';
import type { WalletTransaction } from '../types/api';

const PAGE_SIZE = 10;

export default function WalletPage() {
  const wallet = useWalletStore((s) => s.wallet);
  const walletLoading = useWalletStore((s) => s.loading);
  const loaded = useWalletStore((s) => s.loaded);
  const fetchWallet = useWalletStore((s) => s.fetchWallet);
  const refreshAfterRecharge = useWalletStore((s) => s.refreshAfterRecharge);

  const [transactions, setTransactions] = useState<WalletTransaction[]>([]);
  const [txLoading, setTxLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [rechargeLoading, setRechargeLoading] = useState(false);

  const loadTransactions = useCallback(async (targetPage: number) => {
    setTxLoading(true);
    try {
      const result = await walletService.transactions(targetPage, PAGE_SIZE);
      setTransactions(result);
      setTotal(result.length);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载流水失败');
      setTransactions([]);
      setTotal(0);
    } finally {
      setTxLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!loaded) fetchWallet();
  }, [loaded, fetchWallet]);

  useEffect(() => {
    loadTransactions(page);
  }, [page, loadTransactions]);

  const handleRecharge = async (amount: number) => {
    setRechargeLoading(true);
    try {
      await walletService.recharge(amount);
      message.success('充值成功');
      // 刷新钱包 store，让顶栏欠费标记同步消失。
      await refreshAfterRecharge();
      // 充值后刷新流水第一页，便于看到新记录。
      setPage(1);
      if (page === 1) {
        loadTransactions(1);
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '充值失败');
    } finally {
      setRechargeLoading(false);
    }
  };

  const handleRefreshAll = () => {
    fetchWallet();
    loadTransactions(page);
  };

  if (walletLoading && !wallet) {
    return (
      <main className="sr-page">
        <section className="sr-section sr-detail-loading">
          <Spin />
        </section>
      </main>
    );
  }

  if (!wallet) {
    return (
      <main className="sr-page">
        <section className="sr-section">
          <div className="sr-empty">
            <p>钱包加载失败</p>
            <Button onClick={() => fetchWallet()}>重试</Button>
          </div>
        </section>
      </main>
    );
  }

  const inDebt = wallet.balance <= 0;

  return (
    <main className="sr-page">
      <section className="sr-section">
        <div className="sr-items-header">
          <h1 className="sr-title">我的钱包</h1>
          <Button
            aria-label="刷新钱包"
            icon={<RefreshCw size={16} />}
            onClick={handleRefreshAll}
          >
            刷新
          </Button>
        </div>
        {inDebt && (
          <div
            className="sr-alert sr-alert--danger"
            role="alert"
            style={{ marginBottom: 'var(--space-4)' }}
          >
            钱包已欠费，余额不足将无法发起新的租借交易。请在下方模拟充值。
          </div>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-5)' }}>
          <WalletSummary
            wallet={wallet}
            onRecharge={handleRecharge}
            rechargeLoading={rechargeLoading}
          />
          <TransactionList
            transactions={transactions}
            loading={txLoading}
            page={page}
            size={PAGE_SIZE}
            total={total}
            onPageChange={setPage}
          />
        </div>
        <div className="sr-actions" style={{ marginTop: 'var(--space-5)' }}>
          <Link to="/profile" className="sr-btn sr-btn-md sr-btn-secondary">
            返回我的
          </Link>
        </div>
      </section>
    </main>
  );
}

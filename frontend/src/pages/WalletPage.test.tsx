import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import WalletPage from './WalletPage';

const mocks = vi.hoisted(() => ({
  walletService: {
    transactions: vi.fn(),
    recharge: vi.fn(),
  },
  walletState: {
    wallet: {
      id: 1,
      userId: 7,
      balance: 100,
      frozenAmount: 0,
      status: 0,
    },
    loading: false,
    loaded: true,
    fetchWallet: vi.fn(),
    refreshAfterRecharge: vi.fn(),
  },
}));

vi.mock('../services/wallet', () => ({ walletService: mocks.walletService }));
vi.mock('../stores/walletStore', () => ({
  useWalletStore: (selector: (state: typeof mocks.walletState) => unknown) =>
    selector(mocks.walletState),
}));

describe('WalletPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders transaction list when backend returns the wallet transaction array', async () => {
    mocks.walletService.transactions.mockResolvedValue([
      {
        id: 11,
        transactionNo: 'TX001',
        userId: 7,
        type: 1,
        direction: 1,
        amount: 50,
        balanceBefore: 50,
        balanceAfter: 100,
        frozenBefore: 0,
        frozenAfter: 0,
        remark: '模拟充值',
      },
    ]);

    render(
      <MemoryRouter>
        <WalletPage />
      </MemoryRouter>,
    );

    await waitFor(() => expect(mocks.walletService.transactions).toHaveBeenCalledWith(1, 10));
    expect(await screen.findByText(/流水号：TX001/)).toBeInTheDocument();
    expect(screen.getByText('+¥50')).toBeInTheDocument();
  });

  it('uses the same refresh icon treatment as the order page refresh button', async () => {
    mocks.walletService.transactions.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <WalletPage />
      </MemoryRouter>,
    );

    const refresh = await screen.findByRole('button', { name: '刷新钱包' });
    expect(refresh.querySelector('svg')).toBeTruthy();
  });
});

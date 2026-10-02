import { http, requestData } from './http';
import type { WalletAccount, WalletTransaction } from '../types/api';

export const walletService = {
  getMe: () => requestData<WalletAccount>(http.get('/api/wallet/me')),
  recharge: (amount: number) => requestData<WalletAccount>(http.post('/api/wallet/recharge', { amount })),
  transactions: (page = 1, size = 20) =>
    requestData<WalletTransaction[]>(http.get('/api/wallet/transactions', { params: { page, size } })),
};

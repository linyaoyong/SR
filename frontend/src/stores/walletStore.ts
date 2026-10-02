import { create } from 'zustand';
import type { WalletAccount } from '../types/api';
import { walletService } from '../services/wallet';

interface WalletState {
  wallet: WalletAccount | null;
  loading: boolean;
  loaded: boolean;
  fetchWallet: () => Promise<void>;
  refreshAfterRecharge: () => Promise<void>;
  reset: () => void;
  hasDebt: () => boolean;
}

export const useWalletStore = create<WalletState>((set, get) => ({
  wallet: null,
  loading: false,
  loaded: false,
  fetchWallet: async () => {
    set({ loading: true });
    try {
      const wallet = await walletService.getMe();
      set({ wallet, loaded: true, loading: false });
    } catch {
      set({ loading: false });
    }
  },
  refreshAfterRecharge: async () => {
    await get().fetchWallet();
  },
  reset: () => set({ wallet: null, loading: false, loaded: false }),
  hasDebt: () => {
    const wallet = get().wallet;
    return !!wallet && wallet.balance <= 0;
  },
}));

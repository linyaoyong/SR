import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../stores/authStore';
import OrderDetailPage from './OrderDetailPage';

const mocks = vi.hoisted(() => ({
  orderDetail: vi.fn(),
  reviews: vi.fn(),
  payRent: vi.fn(),
  freezeDeposit: vi.fn(),
  ship: vi.fn(),
  receive: vi.fn(),
  returnOrder: vi.fn(),
  complete: vi.fn(),
  cancelOrder: vi.fn(),
}));

vi.mock('../services/rental', () => ({
  rentalService: {
    orderDetail: mocks.orderDetail,
    reviews: mocks.reviews,
    payRent: mocks.payRent,
    freezeDeposit: mocks.freezeDeposit,
    ship: mocks.ship,
    receive: mocks.receive,
    returnOrder: mocks.returnOrder,
    complete: mocks.complete,
    cancelOrder: mocks.cancelOrder,
  },
}));

function buildOrder(overrides: Partial<Record<string, unknown>> = {}) {
  return {
    id: 1,
    orderNo: 'ORD-20260628-0001',
    applicationId: 11,
    proposalId: 21,
    itemId: 101,
    itemSnapshotId: 901,
    ownerId: 2,
    renterId: 7,
    quantity: 1,
    deliveryType: 1,
    rentStartTime: '2026-07-01 10:00:00',
    rentEndTime: '2026-07-05 10:00:00',
    dailyPrice: 25,
    rentAmount: 100,
    depositAmount: 200,
    paidRentAmount: 0,
    frozenDepositAmount: 0,
    status: 0,
    overdueMinutes: 0,
    overdueFeeAmount: 0,
    overdueSettled: 0,
    createTime: '2026-06-28 10:00:00',
    ...overrides,
  };
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="/orders/:id/snapshot" element={<div data-testid="snapshot-page" />} />
        <Route path="/orders/:id/dispute" element={<div data-testid="dispute-page" />} />
        <Route path="/items/:id" element={<div data-testid="item-page" />} />
        <Route path="/users/:id" element={<div data-testid="user-page" />} />
        <Route path="/wallet" element={<div data-testid="wallet-page" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('OrderDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.getState().logout();
    // 默认登录为租借者 id=7
    useAuthStore.getState().setSession({
      userId: 7,
      username: 'renter',
      role: 'USER',
      accessToken: 'access-7',
      refreshToken: 'refresh-7',
      expiresIn: 7200,
    });
    useAuthStore.getState().setCurrentUser({
      id: 7,
      username: 'renter',
      creditScore: 700,
      role: 0,
      status: 0,
      usernameAuditStatus: 1,
      avatarAuditStatus: 1,
      descriptionAuditStatus: 1,
      showRentalHistory: 1,
    });
  });

  it('renders snapshot fallback fields (itemSnapshotId, dailyPrice, rentAmount)', async () => {
    // 使用 status=3（租借中）以避免 PaymentPanel 与订单信息卡同时渲染相同金额。
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 3 }));
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByText('ORD-20260628-0001')).toBeInTheDocument();
    });
    expect(screen.getByText(/901/)).toBeInTheDocument();
    expect(screen.getByText(/¥25/)).toBeInTheDocument();
    expect(screen.getByText(/¥100/)).toBeInTheDocument();
  });

  it('status 0 (待付款): shows 支付租金 / 冻结押金 / 取消订单 actions for renter', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 0 }));
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '取消订单' })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: '支付租金' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '冻结押金' })).toBeInTheDocument();
  });

  it('status 1 + express: owner sees 发货 button; renter does not', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 1, deliveryType: 1 }));
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: '发货' })).not.toBeInTheDocument();
    });
  });

  it('status 1 + meetup: renter sees 确认收到 button', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 1, deliveryType: 0 }));
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '确认收到' })).toBeInTheDocument();
    });
  });

  it('status 2 (已发货): renter sees 确认收到 button', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 2, deliveryType: 1 }));
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '确认收到' })).toBeInTheDocument();
    });
  });

  it('status 3 (租借中): renter sees 提交归还 button', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 3 }));
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '提交归还' })).toBeInTheDocument();
    });
  });

  it('status 3 + meetup: renter can return without entering express tracking fields', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 3, deliveryType: 0 }));
    mocks.returnOrder.mockResolvedValue(null);
    renderAt('/orders/1');

    await userEvent.click(await screen.findByRole('button', { name: '提交归还' }));

    expect(screen.queryByLabelText('归还快递单号')).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '确认提交归还' }));

    expect(mocks.returnOrder).toHaveBeenCalledWith(1, undefined);
  });

  it('status 4 (待归还确认): owner sees 确认完成 button', async () => {
    // 切换到物主身份
    useAuthStore.getState().setSession({
      userId: 2,
      username: 'owner',
      role: 'USER',
      accessToken: 'access-2',
      refreshToken: 'refresh-2',
      expiresIn: 7200,
    });
    useAuthStore.getState().setCurrentUser({
      id: 2,
      username: 'owner',
      creditScore: 700,
      role: 0,
      status: 0,
      usernameAuditStatus: 1,
      avatarAuditStatus: 1,
      descriptionAuditStatus: 1,
      showRentalHistory: 1,
    });
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 4 }));
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '确认完成' })).toBeInTheDocument();
    });
  });

  it('status 5 (已完成): shows 评价订单 button + dispute link for renter', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 5 }));
    mocks.reviews.mockResolvedValue([]);
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '评价订单' })).toBeInTheDocument();
    });
    expect(screen.getByRole('link', { name: '发起异议' })).toHaveAttribute(
      'href',
      '/orders/1/dispute',
    );
  });

  it('status 5 (已完成): provides direct links to item and owner homepage', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder({ status: 5 }));
    mocks.reviews.mockResolvedValue([]);
    renderAt('/orders/1');

    expect(await screen.findByRole('link', { name: '查看物品' })).toHaveAttribute(
      'href',
      '/items/101',
    );
    expect(screen.getByRole('link', { name: '查看物主主页' })).toHaveAttribute(
      'href',
      '/users/2',
    );
  });

  it('renders snapshot link', async () => {
    mocks.orderDetail.mockResolvedValue(buildOrder());
    renderAt('/orders/1');
    await waitFor(() => {
      expect(screen.getByRole('link', { name: '查看物品快照' })).toHaveAttribute(
        'href',
        '/orders/1/snapshot',
      );
    });
  });
});

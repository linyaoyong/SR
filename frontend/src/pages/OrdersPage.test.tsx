import { StrictMode } from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../stores/authStore';
import type { RentalApplication, RentalOrder } from '../types/api';
import OrdersPage from './OrdersPage';

const mocks = vi.hoisted(() => ({
  rentalService: {
    orders: vi.fn(),
    applications: vi.fn(),
  },
  itemService: {
    detail: vi.fn(),
  },
  userService: {
    publicProfile: vi.fn(),
  },
  messageError: vi.fn(),
}));

vi.mock('../services/rental', () => ({ rentalService: mocks.rentalService }));
vi.mock('../services/item', () => ({ itemService: mocks.itemService }));
vi.mock('../services/user', () => ({ userService: mocks.userService }));
vi.mock('antd', async () => {
  const actual = await vi.importActual<typeof import('antd')>('antd');
  return {
    ...actual,
    message: {
      ...actual.message,
      error: mocks.messageError,
    },
  };
});

const baseOrder: RentalOrder = {
  id: 1,
  orderNo: 'RO202606290001',
  applicationId: 1,
  proposalId: 1,
  itemId: 11,
  itemSnapshotId: 1,
  ownerId: 22,
  renterId: 7,
  quantity: 1,
  deliveryType: 0,
  rentStartTime: '2026-06-29 10:00:00',
  rentEndTime: '2026-06-30 10:00:00',
  dailyPrice: 10,
  rentAmount: 10,
  depositAmount: 50,
  paidRentAmount: 0,
  frozenDepositAmount: 0,
  status: 0,
  overdueMinutes: 0,
  overdueFeeAmount: 0,
  overdueSettled: 0,
};

const baseApplication: RentalApplication = {
  id: 91,
  itemId: 31,
  itemTitle: '无人机',
  renterId: 7,
  ownerId: 44,
  status: 0,
  currentProposalId: 1,
  ownerConfirmed: 0,
  renterConfirmed: 0,
  createTime: '2026-06-29 09:00:00',
  currentProposal: {
    id: 1,
    applicationId: 91,
    versionNo: 1,
    operatorId: 7,
    quantity: 1,
    deliveryType: 0,
    rentStartTime: '2026-06-30 10:00:00',
    rentEndTime: '2026-07-01 10:00:00',
    meetupLocation: '南门',
    rentAmount: 88,
    depositAmount: 200,
  },
};

function renderOrders(strict = false) {
  const tree = (
    <MemoryRouter initialEntries={['/orders']}>
      <Routes>
        <Route path="/orders" element={<OrdersPage />} />
      </Routes>
    </MemoryRouter>
  );
  return render(
    strict ? <StrictMode>{tree}</StrictMode> : tree,
  );
}

describe('OrdersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.getState().logout();
    useAuthStore.setState({
      currentUser: {
        id: 7,
        username: 'alice',
        creditScore: 90,
        role: 0,
        status: 0,
        usernameAuditStatus: 1,
        avatarAuditStatus: 1,
        descriptionAuditStatus: 1,
        showRentalHistory: 1,
      },
    });
    mocks.rentalService.orders.mockResolvedValue([
      baseOrder,
      {
        ...baseOrder,
        id: 2,
        orderNo: 'RO202606290002',
        ownerId: 7,
        renterId: 33,
        itemId: 12,
      },
    ]);
    mocks.rentalService.applications.mockResolvedValue([
      baseApplication,
      { ...baseApplication, id: 92, itemId: 32, renterId: 55, ownerId: 7, itemTitle: '投影仪' },
    ]);
    mocks.itemService.detail.mockImplementation((id: number) => Promise.resolve({
      id,
      ownerId: id === 11 ? 22 : id === 12 ? 7 : id === 31 ? 44 : 7,
      title: id === 11 ? '任天堂 Switch' : id === 12 ? '露营天幕' : id === 31 ? '无人机' : '投影仪',
      description: '',
      categoryId: 1,
      tags: '',
      quantity: 1,
      rentedCount: 0,
      supportDelivery: 1,
      supportMeetup: 1,
      priceType: 1,
      dailyPrice: 10,
      minRentDays: 1,
      freeRent: 0,
      depositEnabled: 1,
      depositAmount: 50,
      creditDepositEnabled: 0,
      minCreditScore: 0,
      freeDepositScore: 0,
      reducedDepositScore: 0,
      reducedDepositAmount: 0,
      status: 1,
      auditStatus: 1,
      images: [{ id: id * 10, url: `/uploads/items/${id}.jpg`, sortOrder: 0 }],
    }));
    mocks.userService.publicProfile.mockImplementation((id: number) => Promise.resolve({
      id,
      username: id === 44 ? '无人机物主' : id === 55 ? '借用同学' : id === 22 ? 'Switch 物主' : '露营租客',
      avatarUrl: `/uploads/avatars/${id}.jpg`,
      creditScore: id === 55 ? 92 : 88,
      showRentalHistory: 1,
    }));
  });

  it('switches renter and owner orders in one content panel with active filter by default', async () => {
    renderOrders();

    const title = await screen.findByRole('heading', { name: '我的订单' });
    const refresh = screen.getByRole('button', { name: '刷新订单' });
    const content = screen.getByRole('region', { name: '订单内容' });

    await waitFor(() => expect(within(content).getByText(/RO202606290001/)).toBeInTheDocument());
    expect(within(content).queryByRole('heading', { name: '我租借的' })).not.toBeInTheDocument();
    expect(await within(content).findByText('任天堂 Switch')).toBeInTheDocument();
    expect(within(content).getByText('Switch 物主')).toBeInTheDocument();
    expect(within(content).getAllByText('信用分 88').length).toBeGreaterThanOrEqual(1);
    expect(within(content).getByText('申请：无人机')).toBeInTheDocument();
    expect(within(content).getByText('无人机物主')).toBeInTheDocument();
    expect(within(content).queryByText(/RO202606290002/)).not.toBeInTheDocument();
    expect(within(content).queryByText('申请：投影仪')).not.toBeInTheDocument();
    expect(screen.getByText('进行中')).toBeInTheDocument();

    await userEvent.click(screen.getByText('我借出的'));

    await waitFor(() => expect(within(content).getByText(/RO202606290002/)).toBeInTheDocument());
    expect(within(content).queryByRole('heading', { name: '我借出的' })).not.toBeInTheDocument();
    expect(await within(content).findByText('露营天幕')).toBeInTheDocument();
    expect(within(content).getByText('露营租客')).toBeInTheDocument();
    expect(within(content).getByText('申请：投影仪')).toBeInTheDocument();
    expect(within(content).getByText('借用同学')).toBeInTheDocument();
    expect(within(content).queryByText(/RO202606290001/)).not.toBeInTheDocument();
    expect(within(content).queryByText('申请：无人机')).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: '租借申请' })).not.toBeInTheDocument();
    expect(title.compareDocumentPosition(refresh) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('uses a stable keyed error toast when initial loading is retried by StrictMode', async () => {
    mocks.rentalService.orders.mockRejectedValue(new Error('服务暂时不可用，请稍后再试'));
    mocks.rentalService.applications.mockResolvedValue([]);

    renderOrders(true);

    await waitFor(() => expect(mocks.messageError).toHaveBeenCalled());
    expect(mocks.messageError.mock.calls.length).toBeGreaterThanOrEqual(1);
    expect(mocks.messageError.mock.calls.every(([arg]) =>
      typeof arg === 'object'
      && arg !== null
      && 'key' in arg
      && arg.key === 'orders-load-error'
      && 'content' in arg
      && arg.content === '服务暂时不可用，请稍后再试',
    )).toBe(true);
  });
});

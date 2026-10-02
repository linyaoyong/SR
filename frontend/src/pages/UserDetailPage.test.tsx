import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import UserDetailPage from './UserDetailPage';

const mocks = vi.hoisted(() => ({
  userService: {
    publicProfile: vi.fn(),
  },
  rentalService: {
    userHistory: vi.fn(),
    userReviews: vi.fn(),
  },
}));

vi.mock('../services/user', () => ({ userService: mocks.userService }));
vi.mock('../services/rental', () => ({ rentalService: mocks.rentalService }));

describe('UserDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.rentalService.userHistory.mockResolvedValue([]);
    mocks.rentalService.userReviews.mockResolvedValue([]);
  });

  it('shows empty rental history text when owner allows history but has no records yet', async () => {
    mocks.userService.publicProfile.mockResolvedValue({
      id: 2,
      username: 'owner',
      creditScore: 100,
      showRentalHistory: 1,
    });

    render(
      <MemoryRouter>
        <UserDetailPage userId={2} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('该用户暂无租借记录')).toBeInTheDocument();
    expect(screen.queryByText('该用户已开放租借历史展示')).not.toBeInTheDocument();
  });

  it('shows public rental history and reviews when available', async () => {
    mocks.userService.publicProfile.mockResolvedValue({
      id: 2,
      username: 'owner',
      creditScore: 101,
      showRentalHistory: 1,
    });
    mocks.rentalService.userHistory.mockResolvedValue([
      {
        id: 8,
        orderNo: 'ORD-8',
        itemId: 11,
        itemSnapshotId: 91,
        itemSnapshotTitle: '露营灯',
        ownerId: 2,
        renterId: 7,
        quantity: 1,
        deliveryType: 0,
        rentStartTime: '2026-07-01 10:00:00',
        rentEndTime: '2026-07-02 10:00:00',
        dailyPrice: 10,
        rentAmount: 10,
        depositAmount: 50,
        paidRentAmount: 10,
        frozenDepositAmount: 50,
        status: 5,
        overdueMinutes: 0,
        overdueFeeAmount: 0,
        overdueSettled: 1,
        completedTime: '2026-06-30 12:30:00',
      },
    ]);
    mocks.rentalService.userReviews.mockResolvedValue([
      {
        id: 3,
        orderId: 8,
        itemId: 11,
        reviewerId: 7,
        reviewerName: 'renterAlice',
        revieweeId: 2,
        rating: 5,
        content: '准时交付',
        status: 0,
        createTime: '2026-06-30 13:45:00',
      },
    ]);

    render(
      <MemoryRouter>
        <UserDetailPage userId={2} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('露营灯')).toBeInTheDocument();
    expect(screen.queryByText('ORD-8')).not.toBeInTheDocument();
    expect(screen.getByText('准时交付')).toBeInTheDocument();
    expect(screen.getByText('5 分')).toBeInTheDocument();
    expect(screen.getByText('完成于 2026-06-30 12:30 · 出租')).toBeInTheDocument();
    expect(screen.getByText('2026-06-30 13:45')).toBeInTheDocument();
    expect(screen.getByText('评价者：renterAlice')).toBeInTheDocument();
  });
});

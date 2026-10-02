import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OrderSnapshotPage from './OrderSnapshotPage';

const mocks = vi.hoisted(() => ({
  orderDetail: vi.fn(),
}));

vi.mock('../services/rental', () => ({
  rentalService: {
    orderDetail: mocks.orderDetail,
  },
}));

function renderSnapshot() {
  return render(
    <MemoryRouter initialEntries={['/orders/1/snapshot']}>
      <Routes>
        <Route path="/orders/:id/snapshot" element={<OrderSnapshotPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('OrderSnapshotPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.orderDetail.mockResolvedValue({
      id: 1,
      orderNo: 'ORD-1',
      applicationId: 11,
      proposalId: 21,
      itemId: 101,
      itemSnapshotId: 901,
      itemSnapshotTitle: 'Switch 2',
      itemSnapshotImageUrls: '/uploads/items/switch-a.jpg,/uploads/items/switch-b.jpg',
      ownerId: 2,
      renterId: 7,
      quantity: 1,
      deliveryType: 0,
      rentStartTime: '2026-07-01 10:00:00',
      rentEndTime: '2026-07-05 10:00:00',
      dailyPrice: 25,
      rentAmount: 100,
      depositAmount: 200,
      paidRentAmount: 0,
      frozenDepositAmount: 0,
      status: 3,
      overdueMinutes: 0,
      overdueFeeAmount: 0,
      overdueSettled: 0,
      createTime: '2026-06-28 10:00:00',
    });
  });

  it('shows item snapshot title and images', async () => {
    renderSnapshot();

    expect(await screen.findByText('Switch 2')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Switch 2 图片 1' })).toHaveAttribute(
      'src',
      '/uploads/items/switch-a.jpg',
    );
    expect(screen.getByRole('img', { name: 'Switch 2 图片 2' })).toHaveAttribute(
      'src',
      '/uploads/items/switch-b.jpg',
    );
  });
});

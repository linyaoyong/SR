import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { RentalApplication } from '../types/api';
import RentalApplicationCard from './RentalApplicationCard';

const mocks = vi.hoisted(() => ({
  rentalService: {
    orders: vi.fn(),
    applicationDetail: vi.fn(),
    confirmProposal: vi.fn(),
    cancelApplication: vi.fn(),
    updateProposal: vi.fn(),
  },
}));

vi.mock('../services/rental', () => ({ rentalService: mocks.rentalService }));

const application: RentalApplication = {
  id: 91,
  itemId: 31,
  itemTitle: '无人机',
  renterId: 7,
  ownerId: 44,
  status: 0,
  currentProposalId: 1,
  ownerConfirmed: 0,
  renterConfirmed: 1,
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

describe('RentalApplicationCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lets the owner confirm a pending proposal without showing disabled payment actions', () => {
    render(
      <MemoryRouter>
        <RentalApplicationCard
          application={application}
          currentUserId={44}
          onChanged={vi.fn()}
        />
      </MemoryRouter>,
    );

    expect(screen.getByRole('button', { name: '确认协商' })).toBeInTheDocument();
    expect(screen.queryByText(/预支付租金/)).not.toBeInTheDocument();
    expect(screen.queryByText(/预冻结押金/)).not.toBeInTheDocument();
  });

  it('loads proposal detail for list applications before rendering negotiation actions', async () => {
    mocks.rentalService.applicationDetail.mockResolvedValue(application);

    render(
      <MemoryRouter>
        <RentalApplicationCard
          application={{ ...application, currentProposal: null }}
          currentUserId={44}
          onChanged={vi.fn()}
        />
      </MemoryRouter>,
    );

    expect(screen.queryByText('版本 v-')).not.toBeInTheDocument();
    expect(await screen.findByRole('button', { name: '确认协商' })).toBeInTheDocument();
    expect(mocks.rentalService.applicationDetail).toHaveBeenCalledWith(91);
  });
});

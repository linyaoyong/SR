import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PaymentPanel from './PaymentPanel';

const mocks = vi.hoisted(() => ({
  payRent: vi.fn(),
  freezeDeposit: vi.fn(),
}));

vi.mock('../services/rental', () => ({
  rentalService: {
    payRent: mocks.payRent,
    freezeDeposit: mocks.freezeDeposit,
  },
}));

function renderPanel(props: Partial<React.ComponentProps<typeof PaymentPanel>> = {}) {
  const defaultProps: React.ComponentProps<typeof PaymentPanel> = {
    orderId: 1,
    rentAmount: 100,
    paidRentAmount: 30,
    depositAmount: 200,
    frozenDepositAmount: 50,
    onUpdated: vi.fn(),
    ...props,
  };
  return render(
    <MemoryRouter>
      <PaymentPanel {...defaultProps} />
    </MemoryRouter>,
  );
}

describe('PaymentPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows unpaid rent delta = rentAmount - paidRentAmount', () => {
    renderPanel({ rentAmount: 100, paidRentAmount: 30 });
    expect(screen.getByText('¥70')).toBeInTheDocument();
  });

  it('shows unfrozen deposit delta = depositAmount - frozenDepositAmount', () => {
    renderPanel({ depositAmount: 200, frozenDepositAmount: 50 });
    expect(screen.getByText('¥150')).toBeInTheDocument();
  });

  it('renders 去充值 link to /wallet', () => {
    renderPanel();
    const link = screen.getByRole('link', { name: '去充值' });
    expect(link).toHaveAttribute('href', '/wallet');
  });

  it('renders pay rent and freeze deposit buttons', () => {
    renderPanel();
    expect(screen.getByRole('button', { name: '支付租金' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '冻结押金' })).toBeInTheDocument();
  });

  it('calls payRent with the entered amount', async () => {
    const onUpdated = vi.fn();
    const updatedOrder = { id: 1, paidRentAmount: 100 };
    mocks.payRent.mockResolvedValue(updatedOrder);
    renderPanel({ onUpdated });
    const input = screen.getByLabelText('支付金额');
    await userEvent.clear(input);
    await userEvent.type(input, '70');
    await userEvent.click(screen.getByRole('button', { name: '支付租金' }));
    await vi.waitFor(() => {
      expect(mocks.payRent).toHaveBeenCalledWith(1, 70);
      expect(onUpdated).toHaveBeenCalledWith(updatedOrder);
    });
  });

  it('calls freezeDeposit with the entered amount', async () => {
    const onUpdated = vi.fn();
    const updatedOrder = { id: 1, frozenDepositAmount: 200 };
    mocks.freezeDeposit.mockResolvedValue(updatedOrder);
    renderPanel({ onUpdated });
    const input = screen.getByLabelText('冻结金额');
    await userEvent.clear(input);
    await userEvent.type(input, '150');
    await userEvent.click(screen.getByRole('button', { name: '冻结押金' }));
    await vi.waitFor(() => {
      expect(mocks.freezeDeposit).toHaveBeenCalledWith(1, 150);
      expect(onUpdated).toHaveBeenCalledWith(updatedOrder);
    });
  });

  it('shows wallet warning when pay/freeze fails with debt error', async () => {
    mocks.payRent.mockRejectedValue(new Error('钱包余额不足，请先充值'));
    renderPanel();
    const input = screen.getByLabelText('支付金额');
    await userEvent.clear(input);
    await userEvent.type(input, '70');
    await userEvent.click(screen.getByRole('button', { name: '支付租金' }));
    await vi.waitFor(() => {
      expect(screen.getByText(/钱包余额不足/)).toBeInTheDocument();
    });
  });
});

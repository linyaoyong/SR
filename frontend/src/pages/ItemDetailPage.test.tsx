import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '../stores/authStore';
import ItemDetailPage from './ItemDetailPage';

const mocks = vi.hoisted(() => ({
  authService: { me: vi.fn() },
  itemService: {
    detail: vi.fn(),
    favorites: vi.fn(),
    addFavorite: vi.fn(),
    removeFavorite: vi.fn(),
  },
  messageService: {
    openConversation: vi.fn(),
  },
  rentalService: {
    createApplication: vi.fn(),
    itemReviews: vi.fn(),
  },
  userService: {
    publicProfile: vi.fn(),
  },
}));

vi.mock('../services/auth', () => ({ authService: mocks.authService }));
vi.mock('../services/item', () => ({ itemService: mocks.itemService }));
vi.mock('../services/message', () => ({ messageService: mocks.messageService }));
vi.mock('../services/rental', () => ({ rentalService: mocks.rentalService }));
vi.mock('../services/user', () => ({ userService: mocks.userService }));

const item = {
  id: 11,
  ownerId: 22,
  title: '露营灯',
  description: '夜间照明',
  categoryId: 1,
  quantity: 1,
  rentedCount: 0,
  supportDelivery: 1,
  deliveryCity: '',
  supportMeetup: 1,
  meetupLocation: '图书馆南门',
  priceType: 1,
  dailyPrice: 10,
  minRentDays: 1,
  freeRent: 0,
  depositEnabled: 1,
  depositAmount: 50,
  creditDepositEnabled: 0,
  minCreditScore: 60,
  freeDepositScore: 90,
  reducedDepositScore: 80,
  reducedDepositAmount: 20,
  status: 1,
  auditStatus: 1,
  images: [],
};

function renderDetail() {
  return render(
    <MemoryRouter initialEntries={['/items/11']}>
      <Routes>
        <Route path="/items/:id" element={<ItemDetailPage />} />
        <Route path="/messages/:conversationId" element={<div data-testid="conversation-page" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ItemDetailPage messaging', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    useAuthStore.getState().logout();
    useAuthStore.getState().setSession({
      userId: 7,
      username: 'renter',
      role: 'USER',
      accessToken: 'access-7',
      refreshToken: 'refresh-7',
      expiresIn: 7200,
    });
    useAuthStore.setState({
      currentUser: {
        id: 7,
        username: 'renter',
        creditScore: 100,
        role: 0,
        status: 0,
        usernameAuditStatus: 1,
        avatarAuditStatus: 1,
        descriptionAuditStatus: 1,
        showRentalHistory: 1,
      },
    });
    mocks.itemService.detail.mockResolvedValue(item);
    mocks.itemService.favorites.mockResolvedValue([]);
    mocks.rentalService.itemReviews.mockResolvedValue([]);
    mocks.userService.publicProfile.mockResolvedValue({
      id: 22,
      username: 'owner',
      avatarUrl: 'https://example.com/avatar.png',
      creditScore: 96,
      showRentalHistory: 1,
    });
  });

  it('opens or creates a conversation with item owner from detail page', async () => {
    mocks.messageService.openConversation.mockResolvedValue({
      id: 33,
      itemId: 11,
      userAId: 7,
      userBId: 22,
      unreadCount: 0,
    });

    renderDetail();

    await userEvent.click(await screen.findByRole('button', { name: '联系物主' }));

    await waitFor(() => {
      expect(mocks.messageService.openConversation).toHaveBeenCalledWith({ targetUserId: 22, itemId: 11 });
      expect(screen.getByTestId('conversation-page')).toBeInTheDocument();
    });
  });

  it('renders item detail with carousel, tag chips, and owner summary', async () => {
    mocks.itemService.detail.mockResolvedValue({
      ...item,
      tags: '户外, 照明 露营',
      images: [
        { id: 1, url: 'https://example.com/light-1.png', sortOrder: 1 },
        { id: 2, url: 'https://example.com/light-2.png', sortOrder: 2 },
      ],
    });

    const { container } = renderDetail();

    expect(await screen.findByRole('region', { name: '物品图片轮播' })).toBeInTheDocument();
    expect(container.querySelector('.sr-detail-images')).toBeNull();
    expect(screen.getByRole('heading', { name: '标签' })).toBeInTheDocument();
    expect(screen.getByText('户外')).toHaveClass('sr-tag-chip');
    expect(screen.getByText('照明')).toHaveClass('sr-tag-chip');
    expect(await screen.findByText('owner')).toBeInTheDocument();
    expect(screen.getByText('信用分 96')).toBeInTheDocument();
  });

  it('uses a centered apply dialog with prominent recalculated total', async () => {
    mocks.itemService.detail.mockResolvedValue({ ...item, quantity: 3 });
    renderDetail();

    await userEvent.click(await screen.findByRole('button', { name: '申请租借' }));

    const dialog = await screen.findByRole('dialog', { name: '申请租借' });
    expect(dialog.querySelector('.sr-apply-modal-body')).toBeTruthy();
    expect(document.body.querySelector('.ant-drawer')).toBeNull();
    const dialogView = within(dialog);
    expect(dialogView.getByText('预计总金额')).toBeInTheDocument();
    const totalValue = dialog.querySelector('.sr-money-total-value');
    expect(totalValue).toBeTruthy();
    await waitFor(() => expect(totalValue).toHaveTextContent('¥60.00'), {
      timeout: 3000,
    });

    const quantity = screen.getByRole('spinbutton', { name: '数量' });
    await userEvent.clear(quantity);
    await userEvent.type(quantity, '2');

    await waitFor(() => expect(totalValue).toHaveTextContent('¥70.00'), {
      timeout: 3000,
    });
  });

  it('fills meetup location as a real form value when switching delivery type', async () => {
    renderDetail();

    await userEvent.click(await screen.findByRole('button', { name: '申请租借' }));
    await screen.findByRole('dialog', { name: '申请租借' });

    await userEvent.click(screen.getByRole('combobox', { name: '交付方式' }));
    await userEvent.click(await screen.findByTitle('面交'));

    await waitFor(() => {
      expect(screen.getByRole('textbox', { name: '面交地点' })).toHaveValue('图书馆南门');
    });
  });

  it('shows item and owner information after submitting a meetup rental application', async () => {
    mocks.itemService.detail.mockResolvedValue({
      ...item,
      supportDelivery: 0,
      supportMeetup: 1,
      meetupLocation: '图书馆南门',
    });
    mocks.rentalService.createApplication.mockResolvedValue({
      id: 91,
      itemId: 11,
      renterId: 7,
      ownerId: 22,
      status: 0,
      currentProposalId: 1,
      ownerConfirmed: 0,
      renterConfirmed: 0,
      createTime: '2026-06-30 10:00:00',
      currentProposal: {
        id: 1,
        applicationId: 91,
        versionNo: 1,
        operatorId: 7,
        quantity: 1,
        deliveryType: 0,
        rentStartTime: '2026-07-01 10:00:00',
        rentEndTime: '2026-07-02 10:00:00',
        meetupLocation: '图书馆南门',
        rentAmount: 10,
        depositAmount: 50,
      },
    });

    renderDetail();

    await userEvent.click(await screen.findByRole('button', { name: '申请租借' }));
    await screen.findByRole('dialog', { name: '申请租借' });
    await userEvent.click(screen.getByRole('button', { name: '提交申请' }));

    await waitFor(() => {
      expect(mocks.rentalService.createApplication).toHaveBeenCalledWith(
        expect.objectContaining({ deliveryType: 0, meetupLocation: '图书馆南门' }),
      );
    });
    const applicationCard = await screen.findByLabelText('租借申请卡片');
    expect(within(applicationCard).getByText('申请：露营灯')).toBeInTheDocument();
    expect(within(applicationCard).getByText('owner')).toBeInTheDocument();
    expect(within(applicationCard).getByText('信用分 96')).toBeInTheDocument();
  });

  it('disables rental application when item quantity is 0', async () => {
    mocks.itemService.detail.mockResolvedValue({ ...item, quantity: 0 });

    renderDetail();

    const applyButton = await screen.findByRole('button', { name: '库存不足' });
    expect(applyButton).toBeDisabled();
    expect(screen.getByText('可租数量：0')).toBeInTheDocument();
  });

  it('disables rental application when all quantity is already rented', async () => {
    mocks.itemService.detail.mockResolvedValue({ ...item, quantity: 2, rentedCount: 2 });

    renderDetail();

    const applyButton = await screen.findByRole('button', { name: '库存不足' });
    expect(applyButton).toBeDisabled();
    expect(screen.getByText('可租数量：0')).toBeInTheDocument();
  });

  it('shows latest item reviews below the gallery', async () => {
    mocks.rentalService.itemReviews.mockResolvedValue([
      {
        id: 3,
        orderId: 8,
        itemId: 11,
        reviewerId: 7,
        revieweeId: 22,
        rating: 5,
        content: '新评价',
        imageUrls: '["/files/reviews/new.jpg"]',
        status: 0,
        createTime: '2026-06-30 13:00:00',
      },
      {
        id: 2,
        orderId: 6,
        itemId: 11,
        reviewerId: 8,
        revieweeId: 22,
        rating: 4,
        content: '旧评价',
        status: 0,
        createTime: '2026-06-29 13:00:00',
      },
    ]);

    renderDetail();

    const reviewRegion = await screen.findByRole('region', { name: '物品历史评价' });
    expect(within(reviewRegion).getByText('新评价')).toBeInTheDocument();
    expect(within(reviewRegion).getByText('旧评价')).toBeInTheDocument();
    expect(within(reviewRegion).getByAltText('评价图片')).toHaveAttribute('src', '/files/reviews/new.jpg');
    expect(within(reviewRegion).getByText('2026-06-30 13:00')).toBeInTheDocument();
  });
});

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ConversationPage from './ConversationPage';

const mocks = vi.hoisted(() => ({
  authState: {
    currentUser: { id: 1, username: 'alice' },
    accessToken: 'test-token',
  },
  messageState: {
    conversations: [
      {
        id: 9,
        itemId: 10,
        userAId: 1,
        userBId: 2,
        itemTitle: '索尼微单相机',
        itemFirstImageUrl: '/uploads/items/camera.jpg',
        peerUserId: 2,
        peerUsername: '林同学',
        peerAvatarUrl: '/uploads/avatars/lin.jpg',
        lastMessageContent: '可以面交',
        lastMessageTime: '2026-06-29 10:00:00',
        unreadCount: 0,
      },
    ],
    fetchConversations: vi.fn(),
    appendLocalMessage: vi.fn(),
  },
  itemService: {
    detail: vi.fn(),
  },
  rentalService: {
    orders: vi.fn(),
  },
}));

vi.mock('../components/ChatPanel', () => ({
  default: () => <div data-testid="chat-panel" />,
}));

vi.mock('../components/MessageComposer', () => ({
  default: () => <div data-testid="message-composer" />,
}));

vi.mock('../components/ApplyRentalModal', () => ({
  default: ({ open }: { open: boolean }) =>
    open ? <div data-testid="apply-modal" /> : null,
}));
vi.mock('../services/item', () => ({ itemService: mocks.itemService }));
vi.mock('../services/rental', () => ({ rentalService: mocks.rentalService }));

vi.mock('../stores/authStore', () => ({
  useAuthStore: Object.assign(
    (selector: (state: typeof mocks.authState) => unknown) =>
      selector(mocks.authState),
    { getState: () => mocks.authState },
  ),
}));

vi.mock('../stores/messageStore', () => ({
  useMessageStore: (selector: (state: typeof mocks.messageState) => unknown) =>
    selector(mocks.messageState),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/messages/9']}>
      <Routes>
        <Route path="/messages/:conversationId" element={<ConversationPage />} />
        <Route path="/orders/:id" element={<div data-testid="order-detail" />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ConversationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.authState.currentUser = { id: 1, username: 'alice' };
    mocks.itemService.detail.mockResolvedValue({ ownerId: 2, quantity: 1, rentedCount: 0 });
    mocks.rentalService.orders.mockResolvedValue([]);
  });

  it('renders peer header, item quick card and quick apply modal', async () => {
    renderPage();

    expect(screen.getAllByText('林同学').length).toBeGreaterThan(0);
    expect(screen.getAllByText('索尼微单相机').length).toBeGreaterThan(0);
    expect(screen.getAllByAltText('林同学头像')[0]).toHaveAttribute(
      'src',
      '/uploads/avatars/lin.jpg',
    );

    await userEvent.click(await screen.findByRole('button', { name: '申请租借' }));

    expect(await screen.findByTestId('apply-modal')).toBeInTheDocument();
  });

  it('hides quick apply for the item owner and routes item card to existing order', async () => {
    mocks.authState.currentUser = { id: 2, username: 'owner' };
    mocks.itemService.detail.mockResolvedValue({ ownerId: 2, quantity: 1, rentedCount: 0 });
    mocks.rentalService.orders.mockResolvedValue([
      { id: 77, itemId: 10, ownerId: 2, renterId: 1, status: 1 },
    ]);

    renderPage();

    const itemButtons = await screen.findAllByRole('button', { name: /索尼微单相机/ });
    expect(itemButtons.length).toBeGreaterThan(0);
    expect(screen.queryByRole('button', { name: '申请租借' })).not.toBeInTheDocument();

    await userEvent.click(itemButtons[itemButtons.length - 1]);

    expect(await screen.findByTestId('order-detail')).toBeInTheDocument();
  });

  it('hides quick apply when the item has no available stock left', async () => {
    mocks.itemService.detail.mockResolvedValue({ ownerId: 2, quantity: 2, rentedCount: 2 });

    renderPage();

    await screen.findByTestId('chat-panel');
    expect(screen.queryByRole('button', { name: '申请租借' })).not.toBeInTheDocument();
  });
});

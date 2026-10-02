import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ChatPanel from './ChatPanel';

const mocks = vi.hoisted(() => ({
  messageState: {
    messagesByConversation: {
      9: [
        {
          id: 1,
          conversationId: 9,
          senderId: 2,
          receiverId: 1,
          messageType: 3,
          cardType: 1,
          content: '租借申请：无人机',
          relatedApplicationId: 91,
          isRead: 0,
          createTime: '2026-06-29 10:00:00',
        },
      ],
    },
    loadMessages: vi.fn().mockResolvedValue([]),
    markConversationRead: vi.fn().mockResolvedValue(undefined),
    setActiveConversation: vi.fn(),
  },
}));

vi.mock('../stores/messageStore', () => ({
  useMessageStore: (selector: (state: typeof mocks.messageState) => unknown) =>
    selector(mocks.messageState),
}));

describe('ChatPanel', () => {
  let scrollIntoView: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    vi.clearAllMocks();
    scrollIntoView = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    });
    Object.defineProperty(HTMLElement.prototype, 'scrollHeight', {
      configurable: true,
      value: 1200,
    });
    Object.defineProperty(HTMLElement.prototype, 'clientHeight', {
      configurable: true,
      value: 400,
    });
  });

  it('renders card messages as standalone business cards instead of plain chat bubbles', async () => {
    const { container } = render(
      <MemoryRouter>
        <ChatPanel conversationId={9} currentUserId={1} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('租借申请：无人机')).toBeInTheDocument();
    expect(container.querySelector('.sr-msg-card-bubble')).toBeTruthy();
    expect(container.querySelector('.sr-msg-card-bubble')?.closest('.sr-msg-bubble')).toBeNull();
  });

  it('scrolls to the newest message when opening a conversation', async () => {
    const { container } = render(
      <MemoryRouter>
        <ChatPanel conversationId={9} currentUserId={1} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('租借申请：无人机')).toBeInTheDocument();
    expect(container.querySelector<HTMLElement>('.sr-msg-panel-body')?.scrollTop).toBe(1200);
  });
});

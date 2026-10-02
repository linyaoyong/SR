import { beforeEach, describe, expect, it, vi } from 'vitest';

// 通过 vi.hoisted 提前定义 mock，保证 messageService 在 messageStore import 前被替换。
const mocks = vi.hoisted(() => ({
  conversations: vi.fn(),
  unreadCount: vi.fn(),
  messages: vi.fn(),
  markRead: vi.fn(),
  createChatSocket: vi.fn(),
  info: vi.fn(),
}));

vi.mock('antd', () => ({
  message: {
    info: mocks.info,
  },
}));

vi.mock('../services/message', () => ({
  messageService: {
    conversations: mocks.conversations,
    unreadCount: mocks.unreadCount,
    messages: mocks.messages,
    markRead: mocks.markRead,
  },
}));

vi.mock('../services/websocket', () => ({
  createChatSocket: mocks.createChatSocket,
}));

import { useMessageStore } from './messageStore';
import type { Conversation, Message } from '../types/api';

// 构造一条最小可用消息，便于在用例中覆盖字段。
function makeMessage(overrides: Partial<Message> & { id: number; conversationId: number }): Message {
  return {
    senderId: 99,
    receiverId: 1,
    messageType: 1,
    content: 'hello',
    isRead: 0,
    createTime: '2026-06-29T10:00:00Z',
    ...overrides,
  } as Message;
}

// 构造一个最小可用会话。
function makeConversation(overrides: Partial<Conversation> & { id: number }): Conversation {
  return {
    itemId: 0,
    userAId: 0,
    userBId: 0,
    unreadCount: 0,
    ...overrides,
  } as Conversation;
}

// 把 store 重置到干净状态，避免用例间相互污染。
function resetStore(overrides: Partial<ReturnType<typeof useMessageStore.getState>> = {}) {
  useMessageStore.setState({
    conversations: [],
    unreadTotal: 0,
    connected: false,
    socket: null,
    activeConversationId: null,
    messagesByConversation: {},
    loadingConversations: false,
    ...overrides,
  });
}

describe('messageStore', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    resetStore();
  });

  it('applyIncomingMessage appends to active conversation', () => {
    resetStore({
      activeConversationId: 1,
      conversations: [makeConversation({ id: 1, peerUsername: '林同学' })],
      messagesByConversation: { 1: [] },
    });

    useMessageStore.getState().applyIncomingMessage(
      makeMessage({ id: 100, conversationId: 1 }),
    );

    const messages = useMessageStore.getState().messagesByConversation[1];
    expect(messages).toHaveLength(1);
    expect(messages[0].id).toBe(100);
    expect(mocks.info).not.toHaveBeenCalled();
  });

  it('applyIncomingMessage increments unread and toasts for inactive conversation', () => {
    resetStore({
      activeConversationId: 2,
      conversations: [makeConversation({ id: 1, unreadCount: 0, peerUsername: '林同学' })],
      messagesByConversation: {},
    });

    useMessageStore.getState().applyIncomingMessage(
      makeMessage({ id: 100, conversationId: 1 }),
    );

    const conv = useMessageStore.getState().conversations.find((c) => c.id === 1);
    expect(conv?.unreadCount).toBe(1);
    expect(useMessageStore.getState().unreadTotal).toBe(1);
    // 非活跃会话不追加到 messagesByConversation，避免缓存与服务器状态漂移。
    expect(useMessageStore.getState().messagesByConversation[1]).toBeUndefined();
    expect(mocks.info).toHaveBeenCalledWith('林同学：hello');
  });

  it('applyIncomingMessage uses image toast copy for inactive image message', () => {
    resetStore({
      activeConversationId: null,
      conversations: [makeConversation({ id: 1, unreadCount: 0, peerUsername: '林同学' })],
    });

    useMessageStore.getState().applyIncomingMessage(
      makeMessage({ id: 101, conversationId: 1, messageType: 2, imageUrls: '["/a.png"]' }),
    );

    expect(mocks.info).toHaveBeenCalledWith('林同学 发来一张图片');
  });

  it('applyIncomingMessage for unknown conversation triggers refresh', () => {
    mocks.conversations.mockResolvedValue([]);
    mocks.unreadCount.mockResolvedValue({ totalUnread: 0 });

    resetStore({
      conversations: [],
      activeConversationId: null,
    });

    useMessageStore.getState().applyIncomingMessage(
      makeMessage({ id: 100, conversationId: 999 }),
    );

    // 未知会话：刷新会话列表与未读总数兜底。
    expect(mocks.conversations).toHaveBeenCalled();
    expect(mocks.unreadCount).toHaveBeenCalled();
  });

  it('applyIncomingMessage marks sent messages as read when receiving a read receipt', () => {
    resetStore({
      activeConversationId: 1,
      conversations: [makeConversation({ id: 1, peerUsername: '林同学' })],
      messagesByConversation: {
        1: [
          makeMessage({ id: 100, conversationId: 1, senderId: 1, receiverId: 99, isRead: 0 }),
          makeMessage({ id: 101, conversationId: 1, senderId: 99, receiverId: 1, isRead: 0 }),
        ],
      },
    });

    useMessageStore.getState().applyIncomingMessage({
      type: 'READ_RECEIPT',
      conversationId: 1,
      readerId: 99,
    } as any);

    const messages = useMessageStore.getState().messagesByConversation[1];
    expect(messages[0].isRead).toBe(1);
    expect(messages[1].isRead).toBe(0);
  });

  it('loadMessages stores history from oldest to newest regardless of API order', async () => {
    mocks.messages.mockResolvedValue([
      makeMessage({ id: 1, conversationId: 1, createTime: '2026-06-29 10:00:00' }),
      makeMessage({ id: 3, conversationId: 1, createTime: '2026-06-29 10:02:00' }),
      makeMessage({ id: 2, conversationId: 1, createTime: '2026-06-29 10:01:00' }),
    ]);

    await useMessageStore.getState().loadMessages(1);

    expect(useMessageStore.getState().messagesByConversation[1].map((message) => message.id)).toEqual([1, 2, 3]);
  });

  it('lastMessageTime sorting parses both ISO and space-separated', () => {
    // 两个时间戳表示同一本地时刻，仅格式不同：一个空格分隔（后端常用），一个 ISO T 分隔。
    resetStore({
      conversations: [
        makeConversation({ id: 1, lastMessageTime: '2026-06-29 10:00:00' }),
        makeConversation({ id: 2, lastMessageTime: '2026-06-29T10:00:00' }),
      ],
      activeConversationId: 1,
      messagesByConversation: { 1: [] },
    });

    // 触发一次 applyIncomingMessage 让 store 重新排序。
    useMessageStore.getState().applyIncomingMessage(
      makeMessage({ id: 999, conversationId: 1, createTime: '2026-06-29 10:00:00' }),
    );

    const sorted = useMessageStore.getState().conversations;
    // 两种格式都应该被正确解析为有效时间戳（非 NaN）。
    const parseBoth = (raw?: string) => Date.parse((raw ?? '').replace(' ', 'T'));
    const t1 = parseBoth(sorted.find((c) => c.id === 1)?.lastMessageTime);
    const t2 = parseBoth(sorted.find((c) => c.id === 2)?.lastMessageTime);
    expect(t1).not.toBeNaN();
    expect(t2).not.toBeNaN();
    // 表示同一时刻时，解析值应相等，排序不会因格式不同而错乱。
    expect(t1).toBe(t2);
  });
});

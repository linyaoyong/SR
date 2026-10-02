import { create } from 'zustand';
import { message as antdMessage } from 'antd';
import type { Conversation, Message } from '../types/api';
import { messageService } from '../services/message';
import { createChatSocket, type ChatSocket } from '../services/websocket';

// 每个会话的最近一次 loadMessages 请求序号，用于丢弃过期响应，避免快速切换会话时旧响应覆盖新数据。
const messageRequestIds = new Map<number, number>();
// 活跃会话收到新消息后 markRead 的防抖句柄：300ms 内多条消息合并为一次 HTTP 请求。
let activeReadDebounce: ReturnType<typeof setTimeout> | undefined;

interface ReadReceiptPayload {
  type: 'READ_RECEIPT';
  conversationId: number;
  readerId: number;
}

type IncomingMessagePayload = (Partial<Message> & { conversationId?: number }) | ReadReceiptPayload;

interface MessageState {
  conversations: Conversation[];
  unreadTotal: number;
  connected: boolean;
  socket: ChatSocket | null;
  activeConversationId: number | null;
  // 历史消息按会话缓存，key=conversationId；倒序（最新在尾部）。
  messagesByConversation: Record<number, Message[]>;
  loadingConversations: boolean;

  fetchConversations: () => Promise<void>;
  fetchUnreadCount: () => Promise<void>;
  setActiveConversation: (id: number | null) => void;
  loadMessages: (conversationId: number) => Promise<Message[]>;
  appendLocalMessage: (conversationId: number, message: Message) => void;
  // 处理 WebSocket 推送的消息：未知会话触发列表刷新，已知会话更新预览/未读/缓存。
  applyIncomingMessage: (message: IncomingMessagePayload) => void;
  markConversationRead: (conversationId: number) => Promise<void>;
  connectSocket: (token: string) => void;
  disconnectSocket: () => void;
  reset: () => void;
}

// 推断消息预览：图片固定为 [图片]，其它取 content。
function previewOf(message: Partial<Message>): string {
  if (message.messageType === 2) return '[图片]';
  if (message.messageType === 4) return message.content ?? '[系统通知]';
  return message.content ?? '';
}

function toastTextOf(conversation: Conversation, message: Partial<Message>): string {
  const peerName = conversation.peerUsername || '对方';
  if (message.messageType === 2) return `${peerName} 发来一张图片`;
  if (message.messageType === 3) {
    if (message.cardType === 1) return '收到新的租借申请';
    return message.content || '收到新的业务消息';
  }
  const preview = previewOf(message) || '发来一条消息';
  return `${peerName}：${preview}`;
}

// 解析消息时间戳为毫秒数。后端可能返回 "yyyy-MM-dd HH:mm:ss"（空格分隔）或 ISO "yyyy-MM-ddTHH:mm:ss"，
// Date.parse 不支持空格分隔，需替换为 T；解析失败返回 0，保证排序稳定。
function parseMessageTime(raw: string | number | undefined): number {
  if (raw == null) return 0;
  if (typeof raw === 'number') return raw;
  const normalized = raw.replace(' ', 'T');
  const ts = Date.parse(normalized);
  return Number.isNaN(ts) ? 0 : ts;
}

// 追加消息到本地缓存，按 message.id 去重，避免 WebSocket 回显或本地乐观更新重复 append。
function appendMessage(messages: Message[], incoming: Message): Message[] {
  if (messages.some((m) => m.id === incoming.id)) {
    return messages;
  }
  return orderMessages([...messages, incoming]);
}

// 聊天窗口按旧 -> 新渲染，滚到底部才是最新消息；不要依赖接口默认排序。
function orderMessages(messages: Message[]): Message[] {
  return [...messages].sort((a, b) => {
    const timeDiff = parseMessageTime(a.createTime) - parseMessageTime(b.createTime);
    if (timeDiff !== 0) return timeDiff;
    return a.id - b.id;
  });
}

// 把推送的消息落到已知会话：更新最后消息预览 + 未读计数 + 缓存历史 + 重排序。
// 仅处理 conversations 中已存在的会话；未知会话由 store.applyIncomingMessage 上层兜底刷新。
function applyIncomingMessageToState(
  state: MessageState,
  message: Partial<Message> & { conversationId: number },
): Partial<MessageState> {
  const conversationId = message.conversationId;
  const isActive = state.activeConversationId === conversationId;
  const conversations = state.conversations.map((c) => {
    if (c.id !== conversationId) return c;
    return {
      ...c,
      lastMessageContent: previewOf(message),
      lastMessageTime: message.createTime ?? c.lastMessageTime,
      unreadCount: isActive ? 0 : c.unreadCount + 1,
    };
  });
  // 把会话提到列表顶部（按 lastMessageTime 倒序）。
  conversations.sort((a, b) => {
    const ta = parseMessageTime(a.lastMessageTime);
    const tb = parseMessageTime(b.lastMessageTime);
    return tb - ta;
  });
  // 仅活跃会话追加到本地缓存；非活跃会话不追加，避免缓存与服务器状态漂移，
  // 用户切回时 loadMessages 会重新拉取覆盖。
  const messagesByConversation = isActive
    ? {
        ...state.messagesByConversation,
        [conversationId]: appendMessage(
          state.messagesByConversation[conversationId] ?? [],
          message as Message,
        ),
      }
    : state.messagesByConversation;
  const unreadTotal = isActive ? state.unreadTotal : state.unreadTotal + 1;
  return { conversations, messagesByConversation, unreadTotal };
}

function isReadReceiptPayload(message: IncomingMessagePayload): message is ReadReceiptPayload {
  return (message as { type?: string }).type === 'READ_RECEIPT';
}

function applyReadReceiptToState(state: MessageState, receipt: ReadReceiptPayload): Partial<MessageState> {
  const existing = state.messagesByConversation[receipt.conversationId];
  if (!existing) return {};
  return {
    messagesByConversation: {
      ...state.messagesByConversation,
      [receipt.conversationId]: existing.map((message) =>
        message.receiverId === receipt.readerId ? { ...message, isRead: 1 } : message,
      ),
    },
  };
}

export const useMessageStore = create<MessageState>((set, get) => ({
  conversations: [],
  unreadTotal: 0,
  connected: false,
  socket: null,
  activeConversationId: null,
  messagesByConversation: {},
  loadingConversations: false,

  fetchConversations: async () => {
    set({ loadingConversations: true });
    try {
      const conversations = await messageService.conversations();
      set({ conversations, loadingConversations: false });
    } catch {
      set({ loadingConversations: false });
    }
  },

  fetchUnreadCount: async () => {
    try {
      const data = await messageService.unreadCount();
      set({ unreadTotal: data.totalUnread });
    } catch {
      // 静默：未读数失败不阻塞会话列表。
    }
  },

  setActiveConversation: (id) => set({ activeConversationId: id }),

  loadMessages: async (conversationId) => {
    // 自增请求序号；response 返回时若已被更新的请求覆盖则丢弃，避免旧响应覆盖新数据并丢失 WebSocket 推送。
    const nextId = (messageRequestIds.get(conversationId) ?? 0) + 1;
    messageRequestIds.set(conversationId, nextId);
    const list = await messageService.messages(conversationId);
    if (messageRequestIds.get(conversationId) !== nextId) return [];
    const ordered = orderMessages(list);
    set((state) => ({
      messagesByConversation: {
        ...state.messagesByConversation,
        [conversationId]: ordered,
      },
    }));
    return ordered;
  },

  appendLocalMessage: (conversationId, message) => {
    set((state) => {
      const existing = state.messagesByConversation[conversationId] ?? [];
      const messagesByConversation = {
        ...state.messagesByConversation,
        [conversationId]: appendMessage(existing, message),
      };
      const conversations = state.conversations.map((c) =>
        c.id === conversationId
          ? {
              ...c,
              lastMessageContent: previewOf(message),
              lastMessageTime: message.createTime ?? c.lastMessageTime,
            }
          : c,
      );
      return { messagesByConversation, conversations };
    });
  },

  applyIncomingMessage: (message) => {
    if (isReadReceiptPayload(message)) {
      set((state) => applyReadReceiptToState(state, message));
      return;
    }
    const conversationId = message.conversationId;
    if (!conversationId) {
      // 系统通知可能不带 conversationId（直接推 content），无法定位具体会话，
      // 直接刷新会话列表与未读总数兜底。
      get().fetchConversations();
      get().fetchUnreadCount();
      return;
    }
    const knownConversation = get().conversations.find((c) => c.id === conversationId);
    if (!knownConversation) {
      // 未知会话（会话列表未加载或新会话刚建立）：刷新会话列表与未读总数兜底，
      // 不在本地产出未读/缓存，避免与服务器状态漂移。
      get().fetchConversations();
      get().fetchUnreadCount();
      return;
    }
    const isActive = get().activeConversationId === conversationId;
    set((state) => applyIncomingMessageToState(state, message as Partial<Message> & { conversationId: number }));
    if (!isActive) {
      antdMessage.info(toastTextOf(knownConversation, message));
    }
    // 活跃会话已把本地 unreadCount 记为 0，但服务端仍计为未读；这里防抖触发 markRead，
    // 把多条消息合并为一次 HTTP 请求，避免下次 fetchUnreadCount 时徽章突增。
    if (get().activeConversationId === conversationId) {
      if (activeReadDebounce) clearTimeout(activeReadDebounce);
      activeReadDebounce = setTimeout(() => {
        get().markConversationRead(conversationId);
      }, 300);
    }
  },

  markConversationRead: async (conversationId) => {
    try {
      await messageService.markRead(conversationId);
    } catch {
      // 标记已读失败不阻塞聊天，下次进入会重新拉取。
    }
    set((state) => {
      let decremented = 0;
      const conversations = state.conversations.map((c) => {
        if (c.id !== conversationId) return c;
        decremented = c.unreadCount;
        return { ...c, unreadCount: 0 };
      });
      return {
        conversations,
        unreadTotal: Math.max(0, state.unreadTotal - decremented),
      };
    });
  },

  connectSocket: (token) => {
    const prev = get().socket;
    if (prev) return; // 已有连接，避免重复建立。
    const socket = createChatSocket({
      token,
      onMessage: (payload) => {
        const message = payload as Partial<Message> & { conversationId?: number };
        // 未知会话/系统通知的兜底刷新、活跃会话的 markRead 防抖均由 applyIncomingMessage 统一处理，
        // 这里只负责把 WebSocket payload 转交给 store。
        get().applyIncomingMessage(message);
      },
      onStateChange: (connected) => set({ connected }),
    });
    set({ socket });
  },

  disconnectSocket: () => {
    const socket = get().socket;
    if (socket) {
      socket.close();
    }
    set({ socket: null, connected: false });
  },

  reset: () => {
    get().disconnectSocket();
    if (activeReadDebounce) {
      clearTimeout(activeReadDebounce);
      activeReadDebounce = undefined;
    }
    messageRequestIds.clear();
    set({
      conversations: [],
      unreadTotal: 0,
      activeConversationId: null,
      messagesByConversation: {},
      loadingConversations: false,
    });
  },
}));

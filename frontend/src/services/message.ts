import { http, requestData } from './http';
import type { Conversation, Message, UnreadCount } from '../types/api';

// 消息接口字段对齐 docs/03-api-contract.md 第 11.1 节。
// 历史消息接口返回 List<MessageResponse>（数组，非分页外壳），故直接用 requestData<Message[]>。
export interface SendMessageBody {
  messageType: number; // 1 文本、2 图片、3 卡片
  content?: string;
  imageUrls?: string; // JSON 数组字符串
  cardType?: number;
  relatedApplicationId?: number;
  relatedOrderId?: number;
}

export const messageService = {
  conversations: () =>
    requestData<Conversation[]>(http.get('/api/messages/conversations')),
  openConversation: (body: { targetUserId: number; itemId: number }) =>
    requestData<Conversation>(http.post('/api/messages/conversations/open', body)),
  messages: (conversationId: number, page = 1, size = 20) =>
    requestData<Message[]>(
      http.get(`/api/messages/conversations/${conversationId}/messages`, {
        params: { page, size },
      }),
    ),
  send: (conversationId: number, body: SendMessageBody) =>
    requestData<Message>(
      http.post(`/api/messages/conversations/${conversationId}/messages`, body),
    ),
  markRead: (conversationId: number) =>
    requestData<null>(http.put(`/api/messages/conversations/${conversationId}/read`)),
  unreadCount: () =>
    requestData<UnreadCount>(http.get('/api/messages/unread-count')),
  uploadImage: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return requestData<{ url: string }>(
      http.post('/api/messages/images', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    );
  },
};

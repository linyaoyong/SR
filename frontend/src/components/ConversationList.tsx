import { useMemo } from 'react';
import { Bell, MessageSquare } from 'lucide-react';
import type { Conversation } from '../types/api';

interface ConversationListProps {
  conversations: Conversation[];
  activeConversationId?: number;
  onSelect: (conversationId: number) => void;
}

function isSystemConversation(conversation: Conversation): boolean {
  return conversation.itemId === 0 && (conversation.userAId === 0 || conversation.peerUserId === 0);
}

function titleOf(conversation: Conversation): string {
  if (isSystemConversation(conversation)) {
    return '系统通知';
  }
  return conversation.itemTitle || (conversation.itemId > 0 ? `物品 #${conversation.itemId}` : `会话 #${conversation.id}`);
}

function peerNameOf(conversation: Conversation): string {
  if (isSystemConversation(conversation)) {
    return '系统通知';
  }
  return conversation.peerUsername || (conversation.peerUserId ? `用户 #${conversation.peerUserId}` : '对方用户');
}

function formatTime(time?: string): string {
  if (!time) return '';
  const date = new Date(time.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return time;
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  const pad = (n: number) => n.toString().padStart(2, '0');
  if (sameDay) return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  if (date.getFullYear() !== now.getFullYear()) {
    return `${date.getFullYear()}\u2011${pad(date.getMonth() + 1)}\u2011${pad(date.getDate())}`;
  }
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function unreadText(count: number): string {
  return count > 99 ? '99+' : String(count);
}

export default function ConversationList({
  conversations,
  activeConversationId,
  onSelect,
}: ConversationListProps) {
  const sorted = useMemo(() => {
    return [...conversations].sort((a, b) => {
      const ta = a.lastMessageTime ? Date.parse(a.lastMessageTime.replace(' ', 'T')) : 0;
      const tb = b.lastMessageTime ? Date.parse(b.lastMessageTime.replace(' ', 'T')) : 0;
      return tb - ta;
    });
  }, [conversations]);

  if (conversations.length === 0) {
    return (
      <div className="sr-msg-empty">
        <p>还没有对话</p>
      </div>
    );
  }

  return (
    <ul className="sr-msg-list">
      {sorted.map((conversation) => {
        const isSystem = isSystemConversation(conversation);
        const active = conversation.id === activeConversationId;
        const title = titleOf(conversation);
        const peerName = peerNameOf(conversation);
        return (
          <li key={conversation.id}>
            <button
              type="button"
              className={`sr-msg-list-item${active ? ' active' : ''}`}
              onClick={() => onSelect(conversation.id)}
            >
              <span className="sr-msg-list-thumb">
                {isSystem ? (
                  <Bell size={18} />
                ) : conversation.itemFirstImageUrl ? (
                  <img src={conversation.itemFirstImageUrl} alt={`${title}图片`} />
                ) : (
                  <MessageSquare size={18} />
                )}
              </span>
              <span className="sr-msg-list-body">
                <span className="sr-msg-list-title">{title}</span>
                <span className="sr-msg-list-subline">
                  <span className="sr-msg-list-peer">
                    {conversation.peerAvatarUrl ? (
                      <img src={conversation.peerAvatarUrl} alt={`${peerName}头像`} />
                    ) : (
                      <span>{peerName.slice(0, 1)}</span>
                    )}
                  </span>
                  <span className="sr-msg-list-peer-name">{peerName}</span>
                  <span className="sr-msg-list-preview">
                    {conversation.lastMessageContent || '暂无消息'}
                  </span>
                </span>
              </span>
              <span className="sr-msg-list-meta">
                <span className="sr-msg-list-time">
                  {formatTime(conversation.lastMessageTime)}
                </span>
                {conversation.unreadCount > 0 && (
                  <span className="sr-msg-list-badge">{unreadText(conversation.unreadCount)}</span>
                )}
              </span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}

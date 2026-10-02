import { useEffect, useRef, useState } from 'react';
import { Image as AntImage, Spin, Tag } from 'antd';
import { Link } from 'react-router-dom';
import { useMessageStore } from '../stores/messageStore';
import { cardTypeText } from '../types/status';
import type { Message } from '../types/api';

interface ChatPanelProps {
  conversationId: number;
  currentUserId: number;
}

// 解析 imageUrls（JSON 数组字符串）为 URL 列表，容错返回空数组。
function parseImageUrls(raw?: string): string[] {
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed.filter((u): u is string => typeof u === 'string');
  } catch {
    // 容错：非 JSON 时按逗号切分。
    return raw
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);
  }
  return [];
}

// 卡片消息跳转目标：申请/协商 -> 申请详情；订单/评价提醒/异议 -> 订单详情。
function cardHref(message: Message): string {
  if (message.relatedOrderId) return `/orders/${message.relatedOrderId}`;
  if (message.relatedApplicationId) return `/orders`;
  return '/orders';
}

// 格式化消息发送时间：今天 HH:mm；昨天 昨天 HH:mm；今年 MM-DD HH:mm；跨年 YYYY-MM-DD HH:mm。
function formatMessageTime(time?: string): string {
  if (!time) return '';
  const date = new Date(time.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) return time;
  const now = new Date();
  const pad = (n: number) => n.toString().padStart(2, '0');
  const hhmm = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  const sameDay = date.toDateString() === now.toDateString();
  if (sameDay) return hhmm;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) return `昨天 ${hhmm}`;
  if (date.getFullYear() === now.getFullYear()) {
    return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hhmm}`;
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${hhmm}`;
}

function MessageBubble({ message, own }: { message: Message; own: boolean }) {
  if (message.messageType === 4) {
    return (
      <div className="sr-msg-row sr-msg-row-system">
        <span className="sr-msg-system">{message.content || '[系统通知]'}</span>
        {message.createTime && (
          <span className="sr-msg-time">{formatMessageTime(message.createTime)}</span>
        )}
      </div>
    );
  }

  const images = message.messageType === 2 ? parseImageUrls(message.imageUrls) : [];

  if (message.messageType === 3) {
    return (
      <div className={`sr-msg-row sr-msg-row-card${own ? ' own' : ''}`}>
        <Link to={cardHref(message)} className="sr-msg-card-bubble">
          <span className="sr-msg-card-label">
            <Tag color="blue">{message.cardType ? cardTypeText[message.cardType] : '卡片'}</Tag>
          </span>
          <span className="sr-msg-card-text">{message.content || '查看详情'}</span>
        </Link>
        <div className="sr-msg-meta">
          {message.createTime && (
            <span className="sr-msg-time">{formatMessageTime(message.createTime)}</span>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className={`sr-msg-row${own ? ' own' : ''}`}>
      <div className="sr-msg-bubble">
        {message.messageType === 1 && <span className="sr-msg-text">{message.content}</span>}
        {message.messageType === 2 && (
          <div className="sr-msg-images">
            {images.length === 0 ? (
              <span className="sr-msg-text">[图片]</span>
            ) : (
              <AntImage.PreviewGroup>
                {images.map((url) => (
                  <AntImage
                    key={url}
                    src={url}
                    alt="消息图片"
                    width={120}
                    className="sr-msg-image"
                  />
                ))}
              </AntImage.PreviewGroup>
            )}
          </div>
        )}
      </div>
      <div className="sr-msg-meta">
        {message.createTime && (
          <span className="sr-msg-time">{formatMessageTime(message.createTime)}</span>
        )}
        {own && message.messageType !== 3 && (
          <span className={`sr-msg-read${message.isRead ? '' : ' unread'}`}>{message.isRead ? '已读' : '未读'}</span>
        )}
      </div>
    </div>
  );
}

export default function ChatPanel({ conversationId, currentUserId }: ChatPanelProps) {
  const messages = useMessageStore((s) => s.messagesByConversation[conversationId]);
  const loadMessages = useMessageStore((s) => s.loadMessages);
  const markConversationRead = useMessageStore((s) => s.markConversationRead);
  const setActiveConversation = useMessageStore((s) => s.setActiveConversation);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const bodyRef = useRef<HTMLDivElement | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  // 进入会话：拉历史 + 标记已读 + 设为活跃会话（避免未读自增）。
  useEffect(() => {
    let cancelled = false;
    setActiveConversation(conversationId);
    setLoading(true);
    setError(null);
    loadMessages(conversationId)
      .then(() => {
        if (!cancelled) setLoading(false);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '加载失败');
          setLoading(false);
        }
      });
    markConversationRead(conversationId);
    return () => {
      // 离开会话时清空活跃标记。
      setActiveConversation(null);
      cancelled = true;
    };
  }, [conversationId, loadMessages, markConversationRead, setActiveConversation]);

  // 新消息到达后滚动到底部。
  useEffect(() => {
    if (loading) return;
    const scrollToBottom = () => {
      if (bodyRef.current) {
        const top = bodyRef.current.scrollHeight;
        bodyRef.current.scrollTop = top;
        if (typeof bodyRef.current.scrollTo === 'function') {
          bodyRef.current.scrollTo({ top, behavior: 'auto' });
        }
        return;
      }
      if (typeof bottomRef.current?.scrollIntoView === 'function') {
        bottomRef.current.scrollIntoView({ behavior: 'auto', block: 'end' });
      }
    };
    scrollToBottom();
    const frameId = window.requestAnimationFrame(scrollToBottom);
    const timerId = window.setTimeout(scrollToBottom, 0);
    return () => {
      window.cancelAnimationFrame(frameId);
      window.clearTimeout(timerId);
    };
  }, [loading, messages]);

  return (
    <section className="sr-msg-panel">
      <div className="sr-msg-panel-body" ref={bodyRef}>
        {loading ? (
          <div className="sr-msg-loading">
            <Spin />
          </div>
        ) : error ? (
          <div className="sr-msg-empty">
            <p>{error}</p>
          </div>
        ) : !messages || messages.length === 0 ? (
          <div className="sr-msg-empty">
            <p>还没有消息，发一条打个招呼吧</p>
          </div>
        ) : (
          messages.map((message) => (
            <MessageBubble
              key={message.id}
              message={message}
              own={message.senderId === currentUserId}
            />
          ))
        )}
        <div ref={bottomRef} />
      </div>
    </section>
  );
}

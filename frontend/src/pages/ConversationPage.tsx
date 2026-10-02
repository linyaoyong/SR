import { useEffect, useState } from 'react';
import { ArrowLeft, MessageSquare } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import ApplyRentalModal from '../components/ApplyRentalModal';
import ChatPanel from '../components/ChatPanel';
import ConversationList from '../components/ConversationList';
import MessageComposer from '../components/MessageComposer';
import { itemService } from '../services/item';
import { rentalService } from '../services/rental';
import { useAuthStore } from '../stores/authStore';
import { useMessageStore } from '../stores/messageStore';
import type { ItemDetail } from '../types/api';

export default function ConversationPage() {
  const { conversationId } = useParams<{ conversationId: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((s) => s.currentUser);
  const conversations = useMessageStore((s) => s.conversations);
  const fetchConversations = useMessageStore((s) => s.fetchConversations);
  const appendLocalMessage = useMessageStore((s) => s.appendLocalMessage);
  const [applyOpen, setApplyOpen] = useState(false);
  const [itemDetail, setItemDetail] = useState<ItemDetail | null>(null);
  const [existingOrderId, setExistingOrderId] = useState<number | null>(null);

  const id = Number(conversationId);
  const validId = !Number.isNaN(id) && id > 0;
  const activeConversation = conversations.find((c) => c.id === id);
  const peerName = activeConversation?.peerUsername || '对方用户';
  const itemTitle = activeConversation?.itemTitle
    || (activeConversation?.itemId ? `物品 #${activeConversation.itemId}` : '相关物品');
  const availableQuantity = itemDetail
    ? Math.max(0, itemDetail.quantity - (itemDetail.rentedCount ?? 0))
    : 0;
  const canApply = Boolean(
    activeConversation
    && activeConversation.itemId > 0
    && currentUser
    && itemDetail?.ownerId !== currentUser.id
    && availableQuantity > 0
    && !existingOrderId,
  );

  useEffect(() => {
    if (conversations.length === 0) {
      fetchConversations();
    }
  }, [conversations.length, fetchConversations]);

  useEffect(() => {
    if (!activeConversation?.itemId) {
      setItemDetail(null);
      setExistingOrderId(null);
      return;
    }
    let active = true;
    itemService
      .detail(activeConversation.itemId)
      .then((item) => {
        if (active) setItemDetail(item);
      })
      .catch(() => {
        if (active) setItemDetail(null);
      });
    rentalService
      .orders()
      .then((orders) => {
        if (!active) return;
        const matched = orders.find((order) =>
          order.itemId === activeConversation.itemId
          && currentUser
          && (order.ownerId === currentUser.id || order.renterId === currentUser.id)
          && (!activeConversation.peerUserId
            || order.ownerId === activeConversation.peerUserId
            || order.renterId === activeConversation.peerUserId),
        );
        setExistingOrderId(matched?.id ?? null);
      })
      .catch(() => {
        if (active) setExistingOrderId(null);
      });
    return () => {
      active = false;
    };
  }, [activeConversation?.itemId, activeConversation?.peerUserId, currentUser]);

  if (!validId || !currentUser) {
    return (
      <main className="sr-page">
        <section className="sr-section">
          <div className="sr-msg-empty">
            <p>会话不存在</p>
            <button
              type="button"
              className="sr-btn sr-btn-sm sr-btn-secondary"
              onClick={() => navigate('/messages')}
            >
              返回消息列表
            </button>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="sr-page sr-msg-page">
      <section className="sr-section sr-msg-layout">
        <aside className="sr-msg-sidebar">
          <button
            type="button"
            className="sr-btn sr-btn-sm sr-btn-secondary sr-msg-back"
            onClick={() => navigate('/messages')}
          >
            <ArrowLeft size={16} /> 消息列表
          </button>
          <ConversationList
            conversations={conversations}
            activeConversationId={id}
            onSelect={(target) => navigate(`/messages/${target}`)}
          />
        </aside>
        <section className="sr-msg-main">
          {activeConversation && (
            <header className="sr-msg-chat-header">
              <div className="sr-msg-chat-peer">
                <span className="sr-msg-chat-avatar">
                  {activeConversation.peerAvatarUrl ? (
                    <img src={activeConversation.peerAvatarUrl} alt={`${peerName}头像`} />
                  ) : (
                    <span>{peerName.slice(0, 1)}</span>
                  )}
                </span>
                <span className="sr-msg-chat-name">{peerName}</span>
              </div>
              {activeConversation.itemId > 0 && (
                <button
                  type="button"
                  className="sr-msg-context-card"
                  onClick={() => navigate(
                    existingOrderId ? `/orders/${existingOrderId}` : `/items/${activeConversation.itemId}`,
                  )}
                >
                  <span className="sr-msg-context-thumb">
                    {activeConversation.itemFirstImageUrl ? (
                      <img src={activeConversation.itemFirstImageUrl} alt={`${itemTitle}图片`} />
                    ) : (
                      <MessageSquare size={16} />
                    )}
                  </span>
                  <span className="sr-msg-context-title">{itemTitle}</span>
                </button>
              )}
              {canApply && (
                <button
                  type="button"
                  className="sr-btn sr-btn-sm sr-btn-primary sr-msg-chat-apply"
                  onClick={() => setApplyOpen(true)}
                >
                  申请租借
                </button>
              )}
            </header>
          )}
          <ChatPanel conversationId={id} currentUserId={currentUser.id} />
          <MessageComposer
            conversationId={id}
            onSent={(message) => appendLocalMessage(id, message)}
          />
        </section>
      </section>
      {activeConversation && activeConversation.itemId > 0 && (
        <ApplyRentalModal
          itemId={activeConversation.itemId}
          open={applyOpen}
          currentUserId={currentUser.id}
          onClose={() => setApplyOpen(false)}
        />
      )}
    </main>
  );
}

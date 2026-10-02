import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ConversationList from '../components/ConversationList';
import { useMessageStore } from '../stores/messageStore';

export default function MessagesPage() {
  const navigate = useNavigate();
  const conversations = useMessageStore((s) => s.conversations);
  const loading = useMessageStore((s) => s.loadingConversations);
  const fetchConversations = useMessageStore((s) => s.fetchConversations);
  const fetchUnreadCount = useMessageStore((s) => s.fetchUnreadCount);

  useEffect(() => {
    fetchConversations();
    fetchUnreadCount();
  }, [fetchConversations, fetchUnreadCount]);

  return (
    <main className="sr-page">
      <section className="sr-section">
        <h1 className="sr-title">消息</h1>
        <div className="sr-card sr-msg-shell">
          {loading && conversations.length === 0 ? (
            <div className="sr-msg-empty">加载中…</div>
          ) : (
            <ConversationList
              conversations={conversations}
              onSelect={(id) => navigate(`/messages/${id}`)}
            />
          )}
        </div>
      </section>
    </main>
  );
}

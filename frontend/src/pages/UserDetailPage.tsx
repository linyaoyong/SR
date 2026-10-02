import { useEffect, useState } from 'react';
import { Spin } from 'antd';
import { Link, useParams } from 'react-router-dom';
import { rentalService } from '../services/rental';
import { userService } from '../services/user';
import type { RentalOrder, Review, UserPublicProfile } from '../types/api';

interface UserDetailPageProps {
  userId?: number;
}

function formatMinute(value?: string) {
  if (!value) return '';
  return value.replace('T', ' ').slice(0, 16);
}

// 公开用户主页：既可作为物品详情抽屉内容，也可挂载到 /users/:id 独立路由。
export default function UserDetailPage({ userId }: UserDetailPageProps) {
  const { id } = useParams<{ id: string }>();
  const routeUserId = id ? Number(id) : undefined;
  const resolvedUserId = userId ?? routeUserId;
  const standalone = userId === undefined;
  const [profile, setProfile] = useState<UserPublicProfile | null>(null);
  const [history, setHistory] = useState<RentalOrder[]>([]);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!resolvedUserId || Number.isNaN(resolvedUserId)) {
      setError('用户不存在');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    userService
      .publicProfile(resolvedUserId)
      .then(async (data) => {
        setProfile(data);
        if (data.showRentalHistory) {
          const [historyData, reviewData] = await Promise.all([
            rentalService.userHistory(resolvedUserId).catch(() => [] as RentalOrder[]),
            rentalService.userReviews(resolvedUserId).catch(() => [] as Review[]),
          ]);
          setHistory(historyData);
          setReviews(reviewData);
        } else {
          setHistory([]);
          setReviews([]);
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : '加载失败'))
      .finally(() => setLoading(false));
  }, [resolvedUserId]);

  const wrap = (content: React.ReactNode) =>
    standalone ? (
      <main className="sr-page">
        <section className="sr-section">
          <div className="sr-items-header">
            <h1 className="sr-title">用户主页</h1>
            <Link to="/items" className="sr-btn sr-btn-sm sr-btn-secondary">
              返回租赁
            </Link>
          </div>
          {content}
        </section>
      </main>
    ) : (
      content
    );

  if (loading) {
    return wrap(
      <div className="sr-detail-loading">
        <Spin />
      </div>,
    );
  }

  if (error || !profile) {
    return wrap(<div className="sr-empty">{error || '用户不存在'}</div>);
  }

  return wrap(
    <div className="sr-user-detail">
      <div className="sr-user-detail-header">
        {profile.avatarUrl ? (
          <img
            src={profile.avatarUrl}
            alt={profile.username}
            className="sr-user-detail-avatar"
          />
        ) : (
          <div className="sr-user-detail-avatar sr-user-detail-avatar--placeholder">
            {profile.username.slice(0, 1)}
          </div>
        )}
        <div className="sr-user-detail-meta">
          <h2 className="sr-user-detail-name">{profile.username}</h2>
          <p className="sr-detail-text">信用分：{profile.creditScore}</p>
        </div>
      </div>
      {profile.description && (
        <div className="sr-detail-section">
          <h3 className="sr-detail-heading">简介</h3>
          <p className="sr-detail-text">{profile.description}</p>
        </div>
      )}
      <div className="sr-detail-section">
        <h3 className="sr-detail-heading">租借历史</h3>
        {profile.showRentalHistory && history.length > 0 ? (
          <div className="sr-user-history-list">
            {history.map((order) => {
              const isOwner = order.ownerId === resolvedUserId;
              return (
                <Link className="sr-user-history-item" to={`/orders/${order.id}/snapshot`} key={order.id}>
                  <span className="sr-user-card-time">
                    {order.completedTime
                      ? `完成于 ${formatMinute(order.completedTime)} · ${isOwner ? '出租' : '租借'}`
                      : isOwner ? '出租' : '租借'}
                  </span>
                  <span>{order.itemSnapshotTitle || `物品 #${order.itemId}`}</span>
                </Link>
              );
            })}
          </div>
        ) : profile.showRentalHistory ? (
          <p className="sr-detail-text">该用户暂无租借记录</p>
        ) : (
          <p className="sr-detail-text">该用户未开放租借历史</p>
        )}
      </div>
      {profile.showRentalHistory && (
        <div className="sr-detail-section">
          <h3 className="sr-detail-heading">收到的评价</h3>
          {reviews.length > 0 ? (
            <div className="sr-user-review-list">
              {reviews.map((review) => (
                <div className="sr-user-review-item" key={review.id}>
                  <span className="sr-user-card-time">{formatMinute(review.createTime)}</span>
                  {review.reviewerName && (
                    <span className="sr-user-review-reviewer">评价者：{review.reviewerName}</span>
                  )}
                  <strong>{review.rating} 分</strong>
                  <span>{review.content || '暂无文字评价'}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="sr-detail-text">暂无评价</p>
          )}
        </div>
      )}
    </div>,
  );
}

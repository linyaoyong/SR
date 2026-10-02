import { useEffect, useMemo, useState } from 'react';
import { Button, Carousel, Drawer, Image, Spin, Tag, message } from 'antd';
import { ArrowLeft, Heart, Maximize2, MessageCircle } from 'lucide-react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import ApplyRentalModal from '../components/ApplyRentalModal';
import { authService } from '../services/auth';
import { itemService } from '../services/item';
import { messageService } from '../services/message';
import { rentalService } from '../services/rental';
import { userService } from '../services/user';
import { useAuthStore } from '../stores/authStore';
import { itemAuditText, itemStatusText } from '../types/status';
import type { ItemDetail, Review, UserPublicProfile } from '../types/api';
import UserDetailPage from './UserDetailPage';

function parseDetailTags(value?: string) {
  const raw = (value ?? '').trim();
  if (!raw) return [];
  if (raw.startsWith('[')) {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.map((tag) => String(tag).trim()).filter(Boolean);
      }
    } catch {
      // Fall through to delimiter parsing for malformed legacy values.
    }
  }
  return raw
    .split(/[,\uFF0C\s]+/)
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function parseStoredImageUrls(raw?: string): string[] {
  if (!raw || !raw.trim()) return [];
  const trimmed = raw.trim();
  if (trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed.map((url) => String(url).trim()).filter(Boolean);
      }
    } catch {
      // Fall back to delimiter parsing for legacy values.
    }
  }
  return trimmed
    .split(/[,，\s]+/)
    .map((url) => url.trim())
    .filter(Boolean);
}

function formatMinute(value?: string) {
  if (!value) return '';
  return value.replace('T', ' ').slice(0, 16);
}

export default function ItemDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const accessToken = useAuthStore((state) => state.accessToken);
  const currentUser = useAuthStore((state) => state.currentUser);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

  const [item, setItem] = useState<ItemDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isFavorite, setIsFavorite] = useState(false);
  const [userDrawerOpen, setUserDrawerOpen] = useState(false);
  const [openingConversation, setOpeningConversation] = useState(false);
  const [ownerProfile, setOwnerProfile] = useState<UserPublicProfile | null>(null);
  const [itemReviews, setItemReviews] = useState<Review[]>([]);

  // 申请相关状态。
  const [applyOpen, setApplyOpen] = useState(false);
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewIndex, setPreviewIndex] = useState(0);

  const itemId = Number(id);

  useEffect(() => {
    if (!itemId || Number.isNaN(itemId)) {
      setError('物品不存在');
      setLoading(false);
      return;
    }
    setLoading(true);
    itemService
      .detail(itemId)
      .then((data) => setItem(data))
      .catch((err) => setError(err instanceof Error ? err.message : '加载失败'))
      .finally(() => setLoading(false));
  }, [itemId]);

  useEffect(() => {
    if (!accessToken || !itemId) return;
    itemService
      .favorites()
      .then((favs) => setIsFavorite(favs.some((f) => f.itemId === itemId)))
      .catch(() => setIsFavorite(false));
  }, [accessToken, itemId]);

  useEffect(() => {
    if (accessToken && !currentUser) {
      authService
        .me()
        .then(setCurrentUser)
        .catch(() => undefined);
    }
  }, [accessToken, currentUser, setCurrentUser]);

  useEffect(() => {
    if (!item?.ownerId) {
      setOwnerProfile(null);
      return;
    }
    let cancelled = false;
    userService
      .publicProfile(item.ownerId)
      .then((profile) => {
        if (!cancelled) setOwnerProfile(profile);
      })
      .catch(() => {
        if (!cancelled) setOwnerProfile(null);
      });
    return () => {
      cancelled = true;
    };
  }, [item?.ownerId]);

  useEffect(() => {
    if (!itemId || Number.isNaN(itemId)) {
      setItemReviews([]);
      return;
    }
    let cancelled = false;
    rentalService
      .itemReviews(itemId)
      .then((reviews) => {
        if (!cancelled) setItemReviews(reviews);
      })
      .catch(() => {
        if (!cancelled) setItemReviews([]);
      });
    return () => {
      cancelled = true;
    };
  }, [itemId]);

  const handleToggleFavorite = async () => {
    if (!accessToken) {
      navigate('/login');
      return;
    }
    try {
      if (isFavorite) {
        await itemService.removeFavorite(itemId);
        setIsFavorite(false);
        message.success('已取消收藏');
      } else {
        await itemService.addFavorite(itemId);
        setIsFavorite(true);
        message.success('已收藏');
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    }
  };

  const handleApply = () => {
    if (!accessToken) {
      navigate('/login');
      return;
    }
    setApplyOpen(true);
  };

  useEffect(() => {
    const state = location.state as { openApply?: boolean } | null;
    if (!state?.openApply || !item || applyOpen) return;
    handleApply();
    navigate(location.pathname, { replace: true, state: {} });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applyOpen, item, location.pathname, location.state, navigate]);

  const handleOpenConversation = async () => {
    if (!item) return;
    if (!accessToken) {
      navigate('/login');
      return;
    }
    setOpeningConversation(true);
    try {
      const conversation = await messageService.openConversation({
        targetUserId: item.ownerId,
        itemId: item.id,
      });
      navigate(`/messages/${conversation.id}`);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '无法发起聊天');
    } finally {
      setOpeningConversation(false);
    }
  };

  const isOwner = !!currentUser && !!item && currentUser.id === item.ownerId;
  const creditLow = !!currentUser && !!item && currentUser.creditScore < item.minCreditScore;
  const availableQuantity = item ? Math.max(0, item.quantity - item.rentedCount) : 0;
  const outOfStock = !!item && availableQuantity <= 0;
  const detailTags = useMemo(() => parseDetailTags(item?.tags), [item?.tags]);

  if (loading) {
    return (
      <main className="sr-page">
        <section className="sr-section sr-detail-loading">
          <Spin />
        </section>
      </main>
    );
  }

  if (error || !item) {
    return (
      <main className="sr-page">
        <section className="sr-section">
          <div className="sr-empty">
            <p>{error || '物品不存在'}</p>
            <Button onClick={() => navigate('/items')}>返回列表</Button>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="sr-page">
      <section className="sr-section">
        <button
          type="button"
          className="sr-btn sr-btn-sm sr-btn-secondary sr-detail-back"
          onClick={() => navigate(-1)}
        >
          <ArrowLeft size={16} /> 返回
        </button>
        <div className="sr-detail">
          <div className="sr-detail-gallery">
            {item.images.length > 0 ? (
              <div className="sr-detail-carousel" role="region" aria-label="物品图片轮播">
                <Image.PreviewGroup
                  preview={{
                    visible: previewVisible,
                    onVisibleChange: (vis) => setPreviewVisible(vis),
                    current: previewIndex,
                  }}
                >
                  {item.images.map((image) => (
                    <Image key={image.id} src={image.url} alt={item.title} style={{ display: 'none' }} />
                  ))}
                </Image.PreviewGroup>
                <Carousel
                  dots
                  autoplay={item.images.length > 1}
                  autoplaySpeed={4000}
                  arrows={item.images.length > 1}
                  infinite={item.images.length > 1}
                >
                  {item.images.map((image, idx) => (
                    <div className="sr-detail-carousel-slide" key={image.id}>
                      <img
                        src={image.url}
                        alt={item.title}
                        className="sr-detail-carousel-img"
                      />
                      <button
                        type="button"
                        className="sr-detail-carousel-zoom"
                        aria-label="查看大图"
                        onClick={() => { setPreviewIndex(idx); setPreviewVisible(true); }}
                      >
                        <Maximize2 size={16} />
                      </button>
                    </div>
                  ))}
                </Carousel>
              </div>
            ) : (
              <div className="sr-empty">暂无图片</div>
            )}
            <section className="sr-detail-review-card sr-card" aria-label="物品历史评价">
              <div className="sr-detail-review-head">
                <h2 className="sr-detail-heading">历史评价</h2>
                <span>{itemReviews.length} 条</span>
              </div>
              {itemReviews.length > 0 ? (
                <div className="sr-detail-review-list">
                  {itemReviews.map((review) => {
                    const images = parseStoredImageUrls(review.imageUrls);
                    return (
                      <article className="sr-detail-review-item" key={review.id}>
                        <div className="sr-detail-review-meta">
                          <strong>{review.rating} 分</strong>
                          {review.createTime && <time>{formatMinute(review.createTime)}</time>}
                        </div>
                        <p>{review.content || '暂无文字评价'}</p>
                        {images.length > 0 && (
                          <div className="sr-detail-review-images">
                            {images.map((url) => (
                              <img src={url} alt="评价图片" key={url} />
                            ))}
                          </div>
                        )}
                      </article>
                    );
                  })}
                </div>
              ) : (
                <p className="sr-detail-text">暂无评价</p>
              )}
            </section>
          </div>
          <div className="sr-detail-info sr-card">
            <h1 className="sr-title">{item.title}</h1>
            <div className="sr-detail-meta">
              <Tag>{itemStatusText[item.status]}</Tag>
              <Tag>{itemAuditText[item.auditStatus]}</Tag>
              {item.auditReason && (
                <span className="sr-detail-audit-reason">审核原因：{item.auditReason}</span>
              )}
            </div>
            <div className="sr-detail-price">
              <span className="sr-detail-price-main">¥{item.dailyPrice}/天</span>
              <span>押金 ¥{item.depositAmount}</span>
              <span>起租 {item.minRentDays} 天</span>
              {item.freeRent > 0 && <span>免租 {item.freeRent} 天</span>}
            </div>
            <div className="sr-detail-section">
              <h2 className="sr-detail-heading">描述</h2>
              <p className="sr-detail-text">{item.description}</p>
            </div>
            {detailTags.length > 0 && (
              <div className="sr-detail-section">
                <h2 className="sr-detail-heading">标签</h2>
                <div className="sr-detail-tag-list" aria-label="物品标签">
                  {detailTags.map((tag) => (
                    <span className="sr-tag-chip" key={tag}>{tag}</span>
                  ))}
                </div>
            </div>
            )}
            <div className="sr-detail-section">
              <h2 className="sr-detail-heading">交付</h2>
              <p className="sr-detail-text">
                快递：{item.supportDelivery ? `支持（${item.deliveryCity || '不限城市'}）` : '不支持'}
              </p>
              <p className="sr-detail-text">
                面交：{item.supportMeetup ? `支持（${item.meetupLocation || '协商地点'}）` : '不支持'}
              </p>
              <p className="sr-detail-text">可租数量：{availableQuantity}</p>
            </div>
            <div className="sr-detail-section">
              <h2 className="sr-detail-heading">信用与押金</h2>
              <p className="sr-detail-text">免押最低信用分：{item.minCreditScore}</p>
              {item.creditDepositEnabled ? (
                <>
                  <p className="sr-detail-text">全免信用分阈值：{item.freeDepositScore}</p>
                  <p className="sr-detail-text">减押信用分阈值：{item.reducedDepositScore}</p>
                  <p className="sr-detail-text">减押后金额：¥{item.reducedDepositAmount}</p>
                </>
              ) : (
                <p className="sr-detail-text">未启用信用免押</p>
              )}
              {accessToken && creditLow && (
                <div className="sr-alert sr-alert--warning">
                  你的信用分（{currentUser?.creditScore}）低于免押最低信用分（{item.minCreditScore}），可能需要缴纳押金。
                </div>
              )}
            </div>
            <div className="sr-detail-section">
              <h2 className="sr-detail-heading">物主</h2>
              <button
                type="button"
                className="sr-owner-summary"
                onClick={() => setUserDrawerOpen(true)}
              >
                {ownerProfile?.avatarUrl ? (
                  <img src={ownerProfile.avatarUrl} alt="" className="sr-owner-summary-avatar" />
                ) : (
                  <span className="sr-owner-summary-avatar sr-owner-summary-avatar--placeholder">
                    {(ownerProfile?.username ?? '物').slice(0, 1)}
                  </span>
                )}
                <span className="sr-owner-summary-main">
                  <span className="sr-owner-summary-name">{ownerProfile?.username ?? `用户 #${item.ownerId}`}</span>
                  <span className="sr-owner-summary-score">
                    信用分 {ownerProfile?.creditScore ?? '-'}
                  </span>
                </span>
                <span className="sr-owner-summary-action">查看主页</span>
              </button>
            </div>
            <div className="sr-actions">
              <Button
                className={`sr-detail-fav${isFavorite ? ' active' : ''}`}
                icon={<Heart size={16} fill={isFavorite ? 'currentColor' : 'none'} />}
                onClick={handleToggleFavorite}
              >
                {isFavorite ? '已收藏' : '收藏'}
              </Button>
              {!isOwner && (
                <>
                  <Button
                    icon={<MessageCircle size={16} />}
                    loading={openingConversation}
                    onClick={handleOpenConversation}
                  >
                    联系物主
                  </Button>
                  <Button type="primary" onClick={handleApply} disabled={outOfStock}>
                    {outOfStock ? '库存不足' : '申请租借'}
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      </section>
      <Drawer
        title="物主主页"
        open={userDrawerOpen}
        onClose={() => setUserDrawerOpen(false)}
        width={420}
        destroyOnHidden
      >
        <UserDetailPage userId={item.ownerId} />
      </Drawer>
      <ApplyRentalModal
        itemId={item.id}
        open={applyOpen}
        currentUserId={currentUser?.id}
        onClose={() => setApplyOpen(false)}
      />
    </main>
  );
}

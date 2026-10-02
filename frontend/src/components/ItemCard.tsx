import { Heart } from 'lucide-react';
import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { itemAuditText } from '../types/status';

// 卡片展示的物品字段。ItemListItem 与由 FavoriteItem 映射而来的对象均满足该结构。
export interface ItemCardItem {
  id: number;
  title: string;
  dailyPrice: number;
  firstImageUrl?: string;
  categoryId?: number;
  minRentDays?: number;
  depositAmount?: number;
  auditStatus?: number;
}

interface ItemCardProps {
  item: ItemCardItem;
  categoryName?: string;
  favoriteState?: boolean;
  onToggleFavorite?: (item: ItemCardItem) => void;
  onRemoveFavorite?: (itemId: number) => void;
}

export default function ItemCard({
  item,
  categoryName,
  favoriteState,
  onToggleFavorite,
  onRemoveFavorite,
}: ItemCardProps) {
  const navigate = useNavigate();

  const goDetail = () => navigate(`/items/${item.id}`);

  const handleFavorite = (event: MouseEvent) => {
    event.stopPropagation();
    if (onRemoveFavorite) {
      onRemoveFavorite(item.id);
      return;
    }
    if (onToggleFavorite) {
      onToggleFavorite(item);
    } else {
      navigate('/login');
    }
  };

  const auditText = typeof item.auditStatus === 'number' ? itemAuditText[item.auditStatus] : undefined;
  const showDeposit = typeof item.depositAmount === 'number' && item.depositAmount > 0;
  const showMinRent = typeof item.minRentDays === 'number' && item.minRentDays > 0;

  return (
    <article
      className="sr-card sr-item-card"
      role="button"
      tabIndex={0}
      onClick={goDetail}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          goDetail();
        }
      }}
    >
      <div className="sr-item-card-image">
        {item.firstImageUrl ? (
          <img src={item.firstImageUrl} alt={item.title} loading="lazy" />
        ) : (
          <div className="sr-item-card-placeholder">暂无图片</div>
        )}
        {onRemoveFavorite ? (
          <button
            type="button"
            className="sr-item-card-fav active"
            onClick={handleFavorite}
            aria-label="取消收藏"
          >
            <Heart size={16} fill="currentColor" />
          </button>
        ) : (
          <button
            type="button"
            className={`sr-item-card-fav${favoriteState ? ' active' : ''}`}
            onClick={handleFavorite}
            aria-label={favoriteState ? '取消收藏' : '收藏'}
          >
            <Heart size={16} fill={favoriteState ? 'currentColor' : 'none'} />
          </button>
        )}
      </div>
      <div className="sr-item-card-body">
        <h3 className="sr-item-card-title">{item.title}</h3>
        {(categoryName || auditText) && (
          <div className="sr-item-card-meta">
            {categoryName && <span className="sr-item-card-tag">{categoryName}</span>}
            {auditText && <span className="sr-item-card-audit">{auditText}</span>}
          </div>
        )}
        <div className="sr-item-card-price">
          <span className="sr-item-card-price-main">¥{item.dailyPrice}/天</span>
        </div>
        {(showDeposit || showMinRent) && (
          <div className="sr-item-card-extra">
            {showDeposit && <span>押金 ¥{item.depositAmount}</span>}
            {showMinRent && <span>起租 {item.minRentDays} 天</span>}
          </div>
        )}
      </div>
    </article>
  );
}

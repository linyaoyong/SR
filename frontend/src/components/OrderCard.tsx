import { Tag } from 'antd';
import type { MouseEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { deliveryTypeText, orderStatusText } from '../types/status';
import type { ItemDetail, RentalOrder, UserPublicProfile } from '../types/api';

interface OrderCardProps {
  order: RentalOrder;
  // 可选：物主标题/首图，由调用方自行附加（RentalOrder 本身不含物品标题）。
  itemTitle?: string;
  itemImage?: string;
  item?: ItemDetail;
  counterparty?: UserPublicProfile;
  counterpartyLabel?: string;
  role?: 'renter' | 'owner';
}

// 订单卡片：展示订单号、状态、租金/押金、租期，点击跳转订单详情。
export default function OrderCard({
  order,
  itemTitle,
  itemImage,
  item,
  counterparty,
  counterpartyLabel,
  role,
}: OrderCardProps) {
  const navigate = useNavigate();

  const goDetail = () => navigate(`/orders/${order.id}`);
  const resolvedTitle = itemTitle ?? item?.title ?? `物品 #${order.itemId}`;
  const resolvedImage = itemImage ?? item?.images?.[0]?.url;

  const handleClick = (event: MouseEvent) => {
    event.stopPropagation();
  };

  return (
    <article
      className="sr-card sr-order-card"
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
      <div className="sr-order-card-header">
        <div className="sr-order-card-no">订单号：{order.orderNo}</div>
        <Tag color="blue">{orderStatusText[order.status]}</Tag>
        {role && <Tag>{role === 'renter' ? '租借者' : '物主'}</Tag>}
      </div>
      <div className="sr-order-card-body">
        {resolvedImage ? (
          <img
            src={resolvedImage}
            alt={resolvedTitle}
            className="sr-order-card-image"
            loading="lazy"
          />
        ) : (
          <div className="sr-order-card-image sr-order-card-placeholder">暂无图片</div>
        )}
        <div className="sr-order-card-info">
          <div className="sr-order-card-title">
            {resolvedTitle}
          </div>
          {counterparty && (
            <div className="sr-user-inline sr-order-card-user">
              {counterparty.avatarUrl ? (
                <img src={counterparty.avatarUrl} alt={`${counterparty.username}头像`} />
              ) : (
                <span>{counterparty.username.slice(0, 1)}</span>
              )}
              <strong>{counterparty.username}</strong>
              {counterpartyLabel && <span>{counterpartyLabel}</span>}
              <span>信用分 {counterparty.creditScore}</span>
            </div>
          )}
          <div className="sr-order-card-meta">
            <span>数量 {order.quantity}</span>
            <span>{deliveryTypeText[order.deliveryType]}</span>
            <span>日租金 ¥{order.dailyPrice}</span>
          </div>
          <div className="sr-order-card-meta">
            <span>租期：{order.rentStartTime} ~ {order.rentEndTime}</span>
          </div>
          <div className="sr-order-card-amounts">
            <span>租金 ¥{order.rentAmount}</span>
            <span>押金 ¥{order.depositAmount}</span>
          </div>
        </div>
      </div>
      <div className="sr-order-card-footer" onClick={handleClick}>
        <button
          type="button"
          className="sr-btn sr-btn-sm sr-btn-secondary"
          onClick={() => navigate(`/orders/${order.id}/snapshot`)}
        >
          查看快照
        </button>
      </div>
    </article>
  );
}

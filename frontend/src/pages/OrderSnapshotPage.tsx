import { useEffect, useState } from 'react';
import { Button, Spin, Tag, message } from 'antd';
import { ArrowLeft } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { rentalService } from '../services/rental';
import { deliveryTypeText } from '../types/status';
import type { RentalOrder } from '../types/api';

function parseSnapshotImageUrls(imageUrls?: string) {
  if (!imageUrls) return [];
  const trimmed = imageUrls.trim();
  if (!trimmed) return [];
  if (trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed);
      return Array.isArray(parsed)
        ? parsed.filter((url): url is string => typeof url === 'string' && url.trim().length > 0)
        : [];
    } catch {
      return [];
    }
  }
  return trimmed
    .split(',')
    .map((url) => url.trim())
    .filter(Boolean);
}

// 订单物品快照页：仅展示订单创建时保存的快照字段，无任何写操作。
// 字段对齐 docs/03-api-contract.md 第 9.2 节 RentalOrderResponse。
export default function OrderSnapshotPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const orderId = Number(id);

  const [order, setOrder] = useState<RentalOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderId || Number.isNaN(orderId)) {
      setError('订单不存在');
      setLoading(false);
      return;
    }
    let active = true;
    (async () => {
      setLoading(true);
      try {
        const data = await rentalService.orderDetail(orderId);
        if (!active) return;
        setOrder(data);
      } catch (err) {
        if (active) {
          setError(err instanceof Error ? err.message : '加载失败');
          message.error(err instanceof Error ? err.message : '加载失败');
        }
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [orderId]);

  if (loading) {
    return (
      <main className="sr-page">
        <section className="sr-section sr-detail-loading">
          <Spin />
        </section>
      </main>
    );
  }

  if (error || !order) {
    return (
      <main className="sr-page">
        <section className="sr-section">
          <div className="sr-empty">
            <p>{error || '订单不存在'}</p>
            <Button onClick={() => navigate(`/orders/${orderId}`)}>返回订单详情</Button>
          </div>
        </section>
      </main>
    );
  }

  const snapshotTitle = order.itemSnapshotTitle || `物品 #${order.itemId}`;
  const snapshotImages = parseSnapshotImageUrls(order.itemSnapshotImageUrls);

  return (
    <main className="sr-page">
      <section className="sr-section">
        <button
          type="button"
          className="sr-btn sr-btn-sm sr-btn-secondary sr-detail-back"
          onClick={() => navigate(`/orders/${orderId}`)}
        >
          <ArrowLeft size={16} /> 返回订单详情
        </button>
        <div className="sr-card sr-snapshot">
          <p className="sr-caption">订单物品快照</p>
          <h1 className="sr-title">订单 {order.orderNo} 物品快照</h1>
          <p className="sr-text">以下信息来自订单创建时保存的快照字段。</p>
          <section className="sr-snapshot-media" aria-label="物品快照图片">
            <div className="sr-snapshot-media-main">
              <h2>{snapshotTitle}</h2>
              {order.itemSnapshotCategoryName && <Tag>{order.itemSnapshotCategoryName}</Tag>}
              {order.itemSnapshotDescription && (
                <p className="sr-text">{order.itemSnapshotDescription}</p>
              )}
            </div>
            {snapshotImages.length > 0 ? (
              <div className="sr-snapshot-images">
                {snapshotImages.map((url, index) => (
                  <img
                    key={`${url}-${index}`}
                    src={url}
                    alt={`${snapshotTitle} 图片 ${index + 1}`}
                  />
                ))}
              </div>
            ) : (
              <div className="sr-snapshot-no-image">暂无快照图片</div>
            )}
          </section>
          <dl className="sr-snapshot-grid">
            <div>
              <dt>订单号</dt>
              <dd>{order.orderNo}</dd>
            </div>
            <div>
              <dt>物品 ID</dt>
              <dd>{order.itemId}</dd>
            </div>
            <div>
              <dt>物品快照 ID</dt>
              <dd>{order.itemSnapshotId}</dd>
            </div>
            <div>
              <dt>数量</dt>
              <dd>{order.quantity}</dd>
            </div>
            <div>
              <dt>交付方式</dt>
              <dd>
                <Tag>{deliveryTypeText[order.deliveryType]}</Tag>
              </dd>
            </div>
            <div>
              <dt>日租金</dt>
              <dd>¥{order.dailyPrice}</dd>
            </div>
            <div>
              <dt>租金总额</dt>
              <dd>¥{order.rentAmount}</dd>
            </div>
            <div>
              <dt>押金金额</dt>
              <dd>¥{order.depositAmount}</dd>
            </div>
            <div>
              <dt>租期开始</dt>
              <dd>{order.rentStartTime}</dd>
            </div>
            <div>
              <dt>租期结束</dt>
              <dd>{order.rentEndTime}</dd>
            </div>
          </dl>
          <div className="sr-actions">
            <Link to={`/orders/${orderId}`} className="sr-btn sr-btn-md sr-btn-secondary">
              返回订单详情
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
}

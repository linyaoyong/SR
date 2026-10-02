import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Segmented, Spin, message } from 'antd';
import { RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';
import OrderCard from '../components/OrderCard';
import RentalApplicationCard from '../components/RentalApplicationCard';
import { itemService } from '../services/item';
import { rentalService } from '../services/rental';
import { userService } from '../services/user';
import { useAuthStore } from '../stores/authStore';
import { orderStatusText } from '../types/status';
import type { ItemDetail, RentalApplication, RentalOrder, UserPublicProfile } from '../types/api';

// 订单状态筛选：全部 + 主流程状态。docs/03-api-contract.md 第 9 节 OrderStatusEnum。
type FilterKey = 'all' | 'active' | 'applications' | `status-${number}`;
type OrderRole = 'renter' | 'owner';
const FILTER_OPTIONS: { label: string; value: FilterKey }[] = [
  { label: '全部', value: 'all' },
  { label: '进行中', value: 'active' },
  { label: '申请', value: 'applications' },
  { label: orderStatusText[0], value: 'status-0' },
  { label: orderStatusText[1], value: 'status-1' },
  { label: orderStatusText[2], value: 'status-2' },
  { label: orderStatusText[3], value: 'status-3' },
  { label: orderStatusText[4], value: 'status-4' },
  { label: orderStatusText[5], value: 'status-5' },
  { label: orderStatusText[6], value: 'status-6' },
];

export default function OrdersPage() {
  const currentUser = useAuthStore((state) => state.currentUser);
  const [orders, setOrders] = useState<RentalOrder[]>([]);
  const [applications, setApplications] = useState<RentalApplication[]>([]);
  const [itemsById, setItemsById] = useState<Record<number, ItemDetail>>({});
  const [profilesById, setProfilesById] = useState<Record<number, UserPublicProfile>>({});
  const [loading, setLoading] = useState(true);
  const [activeRole, setActiveRole] = useState<OrderRole>('renter');
  const [filter, setFilter] = useState<FilterKey>('active');

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [orderData, appData] = await Promise.all([
        rentalService.orders(),
        rentalService.applications().catch(() => [] as RentalApplication[]),
      ]);
      setOrders(orderData);
      setApplications(appData);
      const itemIds = Array.from(new Set([
        ...orderData.map((order) => order.itemId),
        ...appData.map((application) => application.itemId),
      ].filter((id): id is number => typeof id === 'number' && id > 0)));
      const userIds = Array.from(new Set([
        ...orderData.flatMap((order) => [order.ownerId, order.renterId]),
        ...appData.flatMap((application) => [application.ownerId, application.renterId]),
      ].filter((id): id is number => typeof id === 'number' && id > 0)));
      const [itemEntries, profileEntries] = await Promise.all([
        Promise.all(itemIds.map(async (id) => {
          try {
            return [id, await itemService.detail(id)] as const;
          } catch {
            return null;
          }
        })),
        Promise.all(userIds.map(async (id) => {
          try {
            return [id, await userService.publicProfile(id)] as const;
          } catch {
            return null;
          }
        })),
      ]);
      setItemsById(Object.fromEntries(itemEntries.filter((entry): entry is readonly [number, ItemDetail] => !!entry)));
      setProfilesById(Object.fromEntries(profileEntries.filter((entry): entry is readonly [number, UserPublicProfile] => !!entry)));
    } catch (err) {
      message.error({
        key: 'orders-load-error',
        content: err instanceof Error ? err.message : '加载失败',
      });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const visibleApplications = useMemo(
    () => applications.filter((application) => application.status !== 2),
    [applications],
  );

  const rentInOrders = useMemo(
    () => orders.filter((order) => currentUser?.id === order.renterId),
    [orders, currentUser?.id],
  );

  const rentOutOrders = useMemo(
    () => orders.filter((order) => currentUser?.id === order.ownerId),
    [orders, currentUser?.id],
  );

  const rentInApplications = useMemo(
    () => visibleApplications.filter((application) => currentUser?.id === application.renterId),
    [visibleApplications, currentUser?.id],
  );

  const rentOutApplications = useMemo(
    () => visibleApplications.filter((application) => currentUser?.id === application.ownerId),
    [visibleApplications, currentUser?.id],
  );

  const activeOrders = activeRole === 'renter' ? rentInOrders : rentOutOrders;
  const activeApplications = activeRole === 'renter' ? rentInApplications : rentOutApplications;
  const activeOrderRole = activeRole === 'renter' ? 'renter' : 'owner';

  const renderOrderContent = () => {
    const filteredOrders = filter === 'all'
      ? activeOrders
      : filter === 'applications'
        ? []
        : filter === 'active'
          ? activeOrders.filter((order) => order.status >= 0 && order.status <= 4)
          : activeOrders.filter((order) => order.status === Number(filter.replace('status-', '')));
    const filteredApplications = filter === 'all' || filter === 'applications'
      ? activeApplications
      : filter === 'active'
        ? activeApplications.filter((application) => application.status === 0 || application.status === 1)
        : [];
    const total = filteredOrders.length + filteredApplications.length;

    return (
      <section className="sr-orders-panel" aria-label="订单内容">
        <div className="sr-orders-group-header">
          <span>{total} 项</span>
        </div>
        <div className="sr-orders-filter">
          <Segmented
            value={filter}
            onChange={(value) => setFilter(value as FilterKey)}
            options={FILTER_OPTIONS}
          />
        </div>
        {total === 0 ? (
          <div className="sr-orders-group-empty">暂无订单</div>
        ) : (
          <div className="sr-orders-list">
            {filteredApplications.map((application) => (
              <RentalApplicationCard
                key={`application-${application.id}`}
                application={application}
                currentUserId={currentUser?.id}
                item={itemsById[application.itemId]}
                counterparty={profilesById[activeRole === 'renter' ? application.ownerId : application.renterId]}
                counterpartyLabel={activeRole === 'renter' ? '物主' : '租借者'}
                onChanged={loadAll}
              />
            ))}
            {filteredOrders.map((order) => (
              <OrderCard
                key={order.id}
                order={order}
                role={activeOrderRole}
                item={itemsById[order.itemId]}
                counterparty={profilesById[activeRole === 'renter' ? order.ownerId : order.renterId]}
                counterpartyLabel={activeRole === 'renter' ? '物主' : '租借者'}
              />
            ))}
          </div>
        )}
      </section>
    );
  };

  if (loading) {
    return (
      <main className="sr-page">
        <section className="sr-section sr-detail-loading">
          <Spin />
        </section>
      </main>
    );
  }

  return (
    <main className="sr-page">
      <section className="sr-section">
        <div className="sr-page-title-row">
          <h1 className="sr-title">我的订单</h1>
          <Button
            aria-label="刷新订单"
            icon={<RefreshCw size={16} />}
            onClick={loadAll}
          >
            刷新
          </Button>
        </div>
        {orders.length === 0 && visibleApplications.length === 0 ? (
          <div className="sr-empty">
            <p>还没有订单</p>
            <Link to="/items" className="sr-btn sr-btn-md sr-btn-primary">
              去发现物品
            </Link>
          </div>
        ) : (
          <div className="sr-orders-groups">
            <div className="sr-orders-role-switch" aria-label="订单角色切换">
              <button
                type="button"
                className={`sr-orders-role-button${activeRole === 'renter' ? ' active' : ''}`}
                onClick={() => setActiveRole('renter')}
              >
                我租借的
              </button>
              <button
                type="button"
                className={`sr-orders-role-button${activeRole === 'owner' ? ' active' : ''}`}
                onClick={() => setActiveRole('owner')}
              >
                我借出的
              </button>
            </div>
            {renderOrderContent()}
          </div>
        )}
      </section>
    </main>
  );
}

import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Drawer,
  Form,
  Input,
  Popconfirm,
  Spin,
  Tag,
  message,
} from 'antd';
import { ArrowLeft } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import OverdueFeePanel from '../components/OverdueFeePanel';
import OrderTimeline from '../components/OrderTimeline';
import PaymentPanel from '../components/PaymentPanel';
import ReviewDialog from '../components/ReviewDialog';
import { rentalService } from '../services/rental';
import { useAuthStore } from '../stores/authStore';
import { deliveryTypeText, orderStatusText } from '../types/status';
import type { RentalOrder } from '../types/api';

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);

  const orderId = Number(id);
  const [order, setOrder] = useState<RentalOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actioning, setActioning] = useState<string | null>(null);
  const [shipOpen, setShipOpen] = useState(false);
  const [returnOpen, setReturnOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [reviewOpen, setReviewOpen] = useState(false);
  const [shipForm] = Form.useForm<{ shipCompany: string; shipTrackingNo: string }>();
  const [returnForm] = Form.useForm<{ returnCompany?: string; returnTrackingNo?: string }>();
  const [cancelForm] = Form.useForm<{ cancelReason: string }>();

  const loadOrder = useCallback(async () => {
    if (!orderId || Number.isNaN(orderId)) {
      setError('订单不存在');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await rentalService.orderDetail(orderId);
      setOrder(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

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
            <Button onClick={() => navigate('/orders')}>返回订单列表</Button>
          </div>
        </section>
      </main>
    );
  }

  const isRenter = !!currentUser && currentUser.id === order.renterId;
  const isOwner = !!currentUser && currentUser.id === order.ownerId;
  const status = order.status;
  const canCancel = status === 0 || status === 1;

  const handleReceive = async () => {
    setActioning('receive');
    try {
      await rentalService.receive(order.id);
      message.success('已确认收到');
      await loadOrder();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioning(null);
    }
  };

  const handleComplete = async () => {
    setActioning('complete');
    try {
      await rentalService.complete(order.id);
      message.success('订单已完成');
      await loadOrder();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioning(null);
    }
  };

  const handleShip = async (values: { shipCompany: string; shipTrackingNo: string }) => {
    setActioning('ship');
    try {
      await rentalService.ship(order.id, values);
      message.success('已发货');
      setShipOpen(false);
      shipForm.resetFields();
      await loadOrder();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioning(null);
    }
  };

  const handleReturn = async (values?: { returnCompany?: string; returnTrackingNo?: string }) => {
    setActioning('return');
    try {
      await rentalService.returnOrder(order.id, order.deliveryType === 0 ? undefined : values);
      message.success('已提交归还');
      setReturnOpen(false);
      returnForm.resetFields();
      await loadOrder();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioning(null);
    }
  };

  const handleCancel = async (values: { cancelReason?: string }) => {
    setActioning('cancel');
    try {
      await rentalService.cancelOrder(order.id, values.cancelReason);
      message.success('订单已取消');
      setCancelOpen(false);
      cancelForm.resetFields();
      await loadOrder();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setActioning(null);
    }
  };

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
        <div className="sr-card sr-order-detail">
          <div className="sr-order-detail-header">
            <div>
              <h1 className="sr-title">{order.orderNo}</h1>
              <div className="sr-order-detail-meta">
                <Tag color="blue">{orderStatusText[status]}</Tag>
                <Tag>{deliveryTypeText[order.deliveryType]}</Tag>
                {isRenter && <Tag>租借者视角</Tag>}
                {isOwner && <Tag>物主视角</Tag>}
              </div>
            </div>
            <Link
              to={`/orders/${order.id}/snapshot`}
              className="sr-btn sr-btn-sm sr-btn-secondary"
            >
              查看物品快照
            </Link>
          </div>

          <div className="sr-order-detail-grid">
            <dl className="sr-order-detail-info">
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
                <dt>已付租金</dt>
                <dd>¥{order.paidRentAmount}</dd>
              </div>
              <div>
                <dt>已冻结押金</dt>
                <dd>¥{order.frozenDepositAmount}</dd>
              </div>
              <div>
                <dt>交付方式</dt>
                <dd>{deliveryTypeText[order.deliveryType]}</dd>
              </div>
              <div>
                <dt>租期</dt>
                <dd>
                  {order.rentStartTime} ~ {order.rentEndTime}
                </dd>
              </div>
              {order.shipCompany && (
                <div>
                  <dt>发货快递</dt>
                  <dd>
                    {order.shipCompany} / {order.shipTrackingNo}
                  </dd>
                </div>
              )}
              {order.returnCompany && (
                <div>
                  <dt>归还快递</dt>
                  <dd>
                    {order.returnCompany} / {order.returnTrackingNo}
                  </dd>
                </div>
              )}
              {order.cancelReason && (
                <div>
                  <dt>取消原因</dt>
                  <dd>{order.cancelReason}</dd>
                </div>
              )}
            </dl>

            <OrderTimeline
              status={order.status}
              createTime={order.createTime}
              receivedTime={order.receivedTime}
              returnedTime={order.returnedTime}
              completedTime={order.completedTime}
            />
          </div>

          {status === 0 && (
            <PaymentPanel
              orderId={order.id}
              rentAmount={order.rentAmount}
              paidRentAmount={order.paidRentAmount}
              depositAmount={order.depositAmount}
              frozenDepositAmount={order.frozenDepositAmount}
              canOperate={isRenter}
              onUpdated={(updated) => {
                if (updated && typeof updated === 'object') {
                  setOrder((prev) =>
                    prev ? { ...prev, ...(updated as Partial<RentalOrder>) } : prev,
                  );
                }
                loadOrder();
              }}
            />
          )}

          <OverdueFeePanel
            overdueMinutes={order.overdueMinutes}
            overdueFeeAmount={order.overdueFeeAmount}
            overdueSettled={order.overdueSettled}
          />

          <div className="sr-actions sr-order-detail-actions">
            {/* 状态 0：付款/冻结押金已由 PaymentPanel 提供 */}
            {/* 状态 1 + 快递 + 物主：发货 */}
            {status === 1 && order.deliveryType === 1 && isOwner && (
              <Button type="primary" onClick={() => setShipOpen(true)}>
                发货
              </Button>
            )}
            {/* 状态 1 + 面交 + 租借者：确认收到 */}
            {status === 1 && order.deliveryType === 0 && isRenter && (
              <Button type="primary" loading={actioning === 'receive'} onClick={handleReceive}>
                确认收到
              </Button>
            )}
            {/* 状态 2 + 租借者：确认收到 */}
            {status === 2 && isRenter && (
              <Button type="primary" loading={actioning === 'receive'} onClick={handleReceive}>
                确认收到
              </Button>
            )}
            {/* 状态 3 + 租借者：提交归还 */}
            {status === 3 && isRenter && (
              <Button type="primary" onClick={() => setReturnOpen(true)}>
                提交归还
              </Button>
            )}
            {/* 状态 4 + 物主：确认完成 */}
            {status === 4 && isOwner && (
              <Popconfirm
                title="确认完成该订单？"
                onConfirm={handleComplete}
                okText="确认"
                cancelText="取消"
              >
                <Button type="primary" loading={actioning === 'complete'}>
                  确认完成
                </Button>
              </Popconfirm>
            )}
            {/* 状态 5：评价 + 异议。订单参与方（租借者或物主）均可评价，每方仅一次。 */}
            {status === 5 && (isRenter || isOwner) && (
              <>
                <Link
                  to={`/items/${order.itemId}`}
                  className="sr-btn sr-btn-md sr-btn-secondary"
                >
                  查看物品
                </Link>
                <Link
                  to={`/users/${order.ownerId}`}
                  className="sr-btn sr-btn-md sr-btn-secondary"
                >
                  查看物主主页
                </Link>
                <Button type="primary" onClick={() => setReviewOpen(true)}>
                  评价订单
                </Button>
                <Link
                  to={`/orders/${order.id}/dispute`}
                  className="sr-btn sr-btn-md sr-btn-secondary"
                >
                  发起异议
                </Link>
              </>
            )}
            {/* 状态 0/1：取消订单 */}
            {canCancel && (isRenter || isOwner) && (
              <Button danger onClick={() => setCancelOpen(true)}>
                取消订单
              </Button>
            )}
          </div>
        </div>
      </section>

      <Drawer
        title="快递发货"
        open={shipOpen}
        onClose={() => setShipOpen(false)}
        width={420}
        destroyOnClose
      >
        <Form form={shipForm} layout="vertical" onFinish={handleShip}>
          <Form.Item
            name="shipCompany"
            label="快递公司"
            rules={[{ required: true, message: '请输入快递公司' }]}
          >
            <Input placeholder="如：顺丰" maxLength={64} />
          </Form.Item>
          <Form.Item
            name="shipTrackingNo"
            label="快递单号"
            rules={[{ required: true, message: '请输入快递单号' }]}
          >
            <Input placeholder="快递单号" maxLength={64} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={actioning === 'ship'}>
              确认发货
            </Button>
          </Form.Item>
        </Form>
      </Drawer>

      <Drawer
        title="提交归还"
        open={returnOpen}
        onClose={() => setReturnOpen(false)}
        width={420}
        destroyOnClose
      >
        {order.deliveryType === 0 ? (
          <div className="sr-return-meetup">
            <p>面交订单无需填写快递单号，提交后等待物主确认归还。</p>
            <Button type="primary" loading={actioning === 'return'} onClick={() => handleReturn()}>
              确认提交归还
            </Button>
          </div>
        ) : (
          <Form form={returnForm} layout="vertical" onFinish={handleReturn}>
            <Form.Item
              name="returnCompany"
              label="归还快递公司"
              rules={[{ required: true, message: '请输入快递公司' }]}
            >
              <Input placeholder="如：顺丰" maxLength={255} />
            </Form.Item>
            <Form.Item
              name="returnTrackingNo"
              label="归还快递单号"
              rules={[{ required: true, message: '请输入快递单号' }]}
            >
              <Input placeholder="快递单号" maxLength={255} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={actioning === 'return'}>
                提交归还
              </Button>
            </Form.Item>
          </Form>
        )}
      </Drawer>

      <Drawer
        title="取消订单"
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        width={420}
        destroyOnClose
      >
        <Form
          form={cancelForm}
          layout="vertical"
          initialValues={{ cancelReason: '' }}
          onFinish={handleCancel}
        >
          <Form.Item name="cancelReason" label="取消原因（可选）">
            <Input.TextArea placeholder="可不填" maxLength={255} autoSize={{ minRows: 3 }} />
          </Form.Item>
          <Form.Item>
            <Button danger htmlType="submit" loading={actioning === 'cancel'}>
              确认取消订单
            </Button>
          </Form.Item>
        </Form>
      </Drawer>

      <ReviewDialog
        open={reviewOpen}
        orderId={order.id}
        currentUserId={currentUser?.id}
        onClose={() => setReviewOpen(false)}
        onReviewed={loadOrder}
      />
    </main>
  );
}

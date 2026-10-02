import { Timeline } from 'antd';
import { orderStatusText } from '../types/status';

interface OrderTimelineProps {
  status: number; // 当前订单状态 OrderStatusEnum
  createTime?: string;
  receivedTime?: string;
  returnedTime?: string;
  completedTime?: string;
}

// 主流程状态顺序（docs/03-api-contract.md 第 9 节 OrderStatusEnum）：
// 0 待付款 → 1 已付款待交付 → 2 已发货 → 3 租借中 → 4 待归还确认 → 5 已完成
const MAIN_FLOW = [0, 1, 2, 3, 4, 5];

// 订单状态时间线。RentalOrderResponse 暂未提供完整状态历史，使用当前状态 + 主流程渲染。
export default function OrderTimeline({
  status,
  createTime,
  receivedTime,
  returnedTime,
  completedTime,
}: OrderTimelineProps) {
  const currentIndex = MAIN_FLOW.indexOf(status);
  const isMainFlow = currentIndex >= 0;

  const timeLabel: Record<number, string | undefined> = {
    0: createTime,
    1: createTime,
    2: undefined,
    3: receivedTime,
    4: returnedTime,
    5: completedTime,
  };

  const items = MAIN_FLOW.map((s) => {
    const label = orderStatusText[s];
    const active = s === status;
    const done = isMainFlow && currentIndex >= 0 && MAIN_FLOW.indexOf(s) < currentIndex;
    let color: string = 'gray';
    if (active) color = 'blue';
    else if (done) color = 'green';
    const time = timeLabel[s];
    return {
      color,
      children: (
        <div>
          <div>{label}</div>
          {time && <div className="sr-order-timeline-time">{time}</div>}
        </div>
      ),
    };
  });

  // 终止性状态（取消 / 异议 / 关闭）单独作为当前节点高亮。
  if (!isMainFlow) {
    items.push({
      color: 'red',
      children: (
        <div>
          <div>{orderStatusText[status]}</div>
        </div>
      ),
    });
  }

  return (
    <section className="sr-card sr-order-timeline" aria-label="订单状态时间线">
      <h3 className="sr-order-timeline-title">订单状态</h3>
      <Timeline items={items} />
    </section>
  );
}

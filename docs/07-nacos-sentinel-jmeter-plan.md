# Nacos、Sentinel 与 JMeter 演示设计

## 1. 设计原则

旧项目 SDS 的问题是分布式演示偏后补。SR 必须从业务设计阶段固定演示点：

- 热门物品列表需要限流。
- 提交租借申请需要防恶意重复请求。
- 租借服务依赖物品服务和钱包服务，需要熔断降级。
- 时间段库存是租借业务核心，必须有并发一致性演示。

## 2. Nacos Config

建议 Data ID：

```text
item-service.yaml
rental-service.yaml
wallet-service.yaml
message-service.yaml
gateway.yaml
```

建议脚本：

```text
scripts/nacos-config.sh apply
scripts/nacos-config.sh status
scripts/nacos-config.sh delete
```

脚本必须：

- 使用 `NACOS_ADDR` 环境变量，默认 `http://127.0.0.1:8848`。
- 带托管标记 `# share-rental-demo-managed: true`。
- 默认拒绝覆盖没有托管标记的配置。
- 支持 `--force` 明确覆盖。

## 3. 配置项

### item-service.yaml

```yaml
# share-rental-demo-managed: true
item:
  list:
    max-page-size: 100
    default-sort: createTime
sentinel:
  item-list:
    qps: 20
resilience:
  demo:
    item-delay-ms: 0
```

### rental-service.yaml

```yaml
# share-rental-demo-managed: true
rental:
  application:
    qps: 5
  payment:
    timeout-minutes: 30
    demo-timeout-seconds:
    timeout-scan-ms: 60000
resilience:
  item:
    slow-call-rt-ms: 1000
    slow-ratio-threshold: 0.5
    min-request-amount: 5
    stat-interval-ms: 60000
    time-window-seconds: 10
  wallet:
    slow-call-rt-ms: 1000
    slow-ratio-threshold: 0.5
    min-request-amount: 5
    stat-interval-ms: 60000
    time-window-seconds: 10
```

### wallet-service.yaml

```yaml
# share-rental-demo-managed: true
wallet:
  recharge:
    max-amount: 99999
resilience:
  demo:
    wallet-delay-ms: 0
```

## 4. Sentinel 资源

| 资源 | 类型 | 阈值来源 | 演示 |
|---|---|---|---|
| `/api/items` | QPS 限流 | `sentinel.item-list.qps` | 物品列表高频访问返回 429 |
| `/api/rentals/applications` | QPS 限流 | `rental.application.qps` | 重复提交申请被限流 |
| `rental:item-access` | 慢调用熔断 | `resilience.item.*` | item-service 延迟后 rental 降级 |
| `rental:wallet-access` | 慢调用熔断 | `resilience.wallet.*` | wallet-service 延迟后 rental 降级 |

限流响应建议：

```json
{
  "code": 42900,
  "message": "请求过于频繁",
  "data": null
}
```

熔断响应建议：

```json
{
  "code": 42901,
  "message": "服务繁忙",
  "data": null
}
```

## 4.1 Redis 与 RabbitMQ 演示点

Redis key：

```text
sr:auth:refresh:{userId}:{tokenId}
sr:rental:application:duplicate:{renterId}:{itemId}
sr:rental:time-lock:{itemId}:{start}:{end}
sr:message:unread:{userId}
sr:message:online:{userId}
sr:mq:idempotent:{eventId}
sr:scheduler:lock:payment-timeout
```

RabbitMQ 拓扑：

```text
exchange: sr.rental.exchange
wait queue: sr.rental.payment.timeout.wait
dead-letter queue: sr.rental.payment.timeout.dlq
wait routing key: rental.payment.timeout.wait
dead routing key: rental.payment.timeout.dead
```

待付款订单创建后发布 TTL 消息；DLQ 消费端只取消仍为 `PENDING_PAYMENT` 的订单，并使用 `sr:mq:idempotent:{eventId}` 防重复。兜底任务使用 `sr:scheduler:lock:payment-timeout` 防多实例重复扫描。

## 5. JMeter 场景

本项目交付的 JMeter 脚本全部使用 GUI。每个 `.jmx` 配套 README 都必须写清：打开 GUI、选择 JMX、确认变量、启动线程组、查看 View Results Tree、查看 Aggregate Report、截图哪些证据。

### 5.1 物品列表限流

文件：

```text
docs/tests/item-list-stress.jmx
```

步骤：

1. Nacos 将 `sentinel.item-list.qps` 调低到 `2`。
2. JMeter 并发请求 `GET /api/items`。
3. 聚合报告中同时出现 `200` 和 `429`。
4. Sentinel Dashboard 展示 `/api/items` Block QPS。
5. 恢复 QPS 到 `20`。

### 5.2 提交申请限流

文件：

```text
docs/tests/application-submit-limit.jmx
```

步骤：

1. 初始化一个未审核但公开可见、库存足够的物品。
2. 多线程使用不同用户提交同一物品申请。
3. 部分请求成功，部分返回 `42900`。
4. 后端日志能看到限流记录。

### 5.3 时间段库存一致性

文件：

```text
docs/tests/rental-time-lock-consistency.jmx
```

目标：

```text
一件 quantity = 5 的物品，10 个用户并发申请并确认同一时间段。
最终有效 rental_time_locks 占用数量不能超过 5。
```

校验脚本建议：

```text
scripts/verify-rental-time-lock-demo.sh
```

输出必须包含：

```text
初始数量: 5
成功占用数量: <= 5
剩余可租数量: >= 0
时间段库存未超租: PASS
库存守恒检查: PASS
```

### 5.4 钱包慢调用熔断

文件：

```text
docs/tests/wallet-slow-call-circuit-breaker.jmx
```

步骤：

1. Nacos 设置 `resilience.demo.wallet-delay-ms: 1500`。
2. rental-service 熔断阈值保持 `slow-call-rt-ms: 1000`。
3. 多次发起订单租金支付、订单补缴或冻结押金请求。
4. 前几次响应变慢，达到最小请求数后快速返回降级。
5. 恢复 `resilience.demo.wallet-delay-ms: 0`。

### 5.5 物品服务停止降级

文件：

```text
docs/tests/item-service-down-degrade.jmx
```

步骤：

1. 停止 item-service。
2. 发起创建申请或确认 proposal。
3. rental-service 返回稳定降级响应，不出现 500 堆栈泄露。
4. 重启 item-service 后恢复。

## 6. 演示截图清单

- Nacos 服务列表。
- Nacos 配置项发布页面。
- Sentinel 资源实时监控。
- Sentinel Block QPS。
- JMeter Aggregate Report。
- JMeter View Results Tree 中的 429/降级响应。
- 数据库 `rental_time_locks` 校验结果。
- 后端日志中的限流、熔断、降级记录。

## 7. 阶段 8 命令清单

```bash
scripts/nacos-config.sh apply
scripts/nacos-config.sh status
scripts/check-nacos-config.sh
scripts/verify-rental-time-lock-demo.sh
scripts/check-stage8-demo.sh
```

JMeter 文件：

```text
docs/tests/item-list-stress.jmx
docs/tests/application-submit-limit.jmx
docs/tests/rental-time-lock-consistency.jmx
docs/tests/wallet-slow-call-circuit-breaker.jmx
docs/tests/item-service-down-degrade.jmx
docs/tests/README-jmeter.md
```

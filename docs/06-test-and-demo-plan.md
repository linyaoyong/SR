# 测试资产与演示计划

## 1. 测试目标

项目完成时必须能证明：

- 所有服务能启动并注册到 Nacos。
- 前端统一访问 Gateway。
- 核心业务闭环可完整演示。
- API 有 Postman/Apifox 集合可复验，且尽量完整覆盖 `03-api-contract.md` 的全部接口。
- JMeter 能通过 GUI 演示高并发、限流、熔断或降级。
- 异常场景有稳定错误响应和日志。
- 数据库状态与业务流程一致。

## 2. Postman / Apifox 集合

建议文件：

```text
docs/tests/Share Rental API - 双用户完整流程.postman_collection.json
docs/tests/Share Rental API - environment.local.postman_environment.json
```

变量：

```text
baseUrl
userAUsername
userAPassword
userAToken
userBUsername
userBPassword
userBToken
adminUsername
adminPassword
adminToken
itemId
applicationId
proposalId
orderId
conversationId
```

集合分组：

```text
01 Auth
02 User Profile
03 Item
04 Admin Audit
05 Favorite
06 Rental Application
07 Proposal Negotiation
08 Order
09 Wallet
10 Message
11 Review
12 Dispute
13 Error Cases
14 Sentinel Degrade
```

每个核心接口至少准备：

- 正常请求。
- 参数错误。
- 未登录。
- 无权限。
- 业务异常。
- 限流或降级响应，适用于 Sentinel 演示接口。

集合覆盖要求：

- 对外接口按服务完整覆盖：auth、item、rental、wallet、message、admin。
- 内部 Feign 接口单独分组，用于验证 Gateway 禁止外部访问 `/internal/**`，以及带内部 token 时服务间调用可用。
- 双用户完整流程必须包含钱包结算校验：租借者支付租金和冻结押金，订单完成后出借者收到租金，租借者押金释放。

## 3. JMeter 测试资产

所有提供给课程演示和答辩使用的 JMeter 脚本都按 GUI 方式准备和说明。README 中只写 GUI 打开、配置、运行、截图步骤。

建议目录：

```text
docs/tests/
  item-list-stress.jmx
  application-submit-limit.jmx
  rental-time-lock-consistency.jmx
  wallet-slow-call-circuit-breaker.jmx
  item-service-down-degrade.jmx
  README-jmeter.md
  output/               # 不提交
```

最少测试：

| 文件 | 目标 | 成功证据 |
|---|---|---|
| `item-list-stress.jmx` | 物品列表高并发查询 | 聚合报告、响应时间、无 500 |
| `application-submit-limit.jmx` | 提交申请限流 | 同时出现成功和 429 |
| `rental-time-lock-consistency.jmx` | 时间段库存防超租 | 数据库校验库存占用不超过总数量 |
| `wallet-slow-call-circuit-breaker.jmx` | 钱包服务慢调用熔断 | 后续请求快速返回服务繁忙 |
| `item-service-down-degrade.jmx` | 物品服务停止后的降级 | rental-service 返回稳定降级响应 |

## 4. 功能测试主流程

| 编号 | 流程 | 预期 |
|---|---|---|
| TC-001 | 用户 A 注册登录 | 返回 token，可访问个人资料 |
| TC-002 | 用户 A 发布物品 | 物品进入未审核但公开可见 |
| TC-003 | 管理员要求整改物品 | 普通列表隐藏该物品，用户收到整改通知 |
| TC-004 | 用户 B 收藏并申请物品 | 创建申请和 proposal；申请阶段不占用库存，订单创建后再支付租金和冻结押金 |
| TC-005 | 出租者修改协商字段 | 生成新 proposal，另一方需确认 |
| TC-006 | 双方确认 proposal | 创建订单，占用时间段库存；租借者支付租金和冻结押金后进入已付款待交付 |
| TC-007 | 用户 B 充值并补缴租金/冻结押金 | 金额补足后订单进入已付款待交付 |
| TC-008 | 快递发货或面交确认收到 | 订单进入租借中 |
| TC-009 | 用户 B 超时提交归还，用户 A 确认收回 | 订单完成，租金和逾期费用入账用户 A 钱包，剩余押金释放回用户 B 钱包 |
| TC-010 | 双方评价 | 写入评价和信用分记录 |

## 5. 异常测试

| 场景 | 操作 | 预期 |
|---|---|---|
| 未登录 | 请求受保护接口 | 401 或统一未登录错误 |
| 无权限 | B 编辑 A 的物品 | 权限错误 |
| 信用分不足 | 低信用用户申请高门槛物品 | 返回信用分不足 |
| 时间段库存不足 | 多人申请同一时间段超过数量 | 返回库存不足 |
| 余额不足 | 支付租金或冻结押金 | 返回余额不足 |
| 钱包欠费 | 逾期费用扣至钱包余额小于等于 0 后发布、申请或发消息 | 返回钱包欠费，提示先充值 |
| 逾期费用 | 归还时间晚于租借结束时间 | 不足 24 小时按 1.5 倍日租金计费，超过后按 2 倍日租金/天持续计费 |
| 被拉黑 | 被拉黑用户发消息或申请 | 返回被拉黑提示 |
| 用户封禁 | 封禁用户发布/申请/发消息 | 后端拒绝 |
| 服务慢调用 | wallet-service 人为延迟 | Sentinel 熔断 |
| 服务停止 | 停止 item-service 后创建订单 | 返回降级响应 |

## 6. 演示视频证据

视频需覆盖：

```text
启动 MySQL、Redis、Nacos、Sentinel
启动 Gateway 和业务服务
Nacos 服务注册截图
Nacos 配置中心截图
普通用户完整租借流程
管理员审核和封禁流程
WebSocket 实时消息
Postman 集合运行
JMeter 聚合报告
JMeter GUI 操作画面
Sentinel Dashboard 限流/熔断截图
数据库关键表状态
后端日志
API 文档聚合页
```

### 6.1 阶段 8 分布式能力截图清单

- Nacos：`scripts/nacos-config.sh apply/status/delete` 终端输出与配置中心页面。
- Sentinel：`/api/items`、`/api/rentals/applications` 的 429 响应截图，以及 `rental:item-access`、`rental:wallet-access` 降级截图。
- RabbitMQ：`sr.rental.payment.timeout.wait`、`sr.rental.payment.timeout.dlq` 队列页面，待付款订单超时自动取消前后订单状态截图。
- Redis：`sr:auth:refresh:{userId}:{tokenId}`、`sr:rental:application:duplicate:{renterId}:{itemId}`、`sr:rental:time-lock:{itemId}:{start}:{end}`、`sr:message:unread:{userId}`、`sr:message:online:{userId}`、`sr:mq:idempotent:{eventId}` key 截图。
- JMeter：五个 GUI 脚本的 View Results Tree 与 Aggregate Report 截图，脚本位于 `docs/tests/`。
- 烟测：`scripts/check-stage8-demo.sh` 输出 `阶段8演示环境检查: PASS`。

## 7. 最终交付材料

按课程要求最终压缩包包含：

```text
项目源代码/
小组评分手册.docx
答辩PPT.pptx
项目需求分析与设计文档.docx 或 .pdf
项目演示视频.mp4
```

项目需求分析与设计文档不少于 4000 字，必须包含需求分析、架构设计、模块划分、UML 图、ER 图、数据库设计、核心代码说明、测试结果、项目总结和难点分析。

## 8. 阶段 5 双方确认流程演示

本章节用于演示阶段 5 租借申请双方协商、确认、转订单、时间段库存与时间锁的完整闭环。前置条件：用户 A、用户 B 已注册登录并取得 token，用户 A 钱包可用，用户 B 钱包可用且余额充足（覆盖租金 + 押金），数据库已就绪。

### 8.1 主流程

| 步骤 | 操作方 | 接口 | 预期结果 |
|---|---|---|---|
| 1 | 用户 A | `POST /api/items` | 发布物品 `quantity=1`，记录 `itemId`，物品进入待审核但公开可见 |
| 2 | 用户 B | `POST /api/rentals/applications` | 创建租借申请，申请 `status=0`（NEGOTIATING），生成首版 proposal `versionNo=1`，记录 `applicationId`；不占用时间段库存、不写入时间锁（在订单创建时处理） |
| 3 | 用户 A | `PUT /api/rentals/applications/{id}/proposal` | 物主修改 proposal（例如调整 `rentAmount`），生成 `versionNo=2`，重置确认标记（操作方置 1、反方置 0：物主操作则 `ownerConfirmed=1`、`renterConfirmed=0`） |
| 4 | 用户 B | `PUT /api/rentals/applications/{id}/confirm` | 租借者确认，`renterConfirmed=1`，申请 `status` 仍为 0 |
| 5 | 用户 A | `PUT /api/rentals/applications/{id}/confirm` | 物主确认，`ownerConfirmed=1`；双方确认后申请 `status=1`（CONFIRMED），随即转换为 `status=2`（CONVERTED），创建订单 `RentalOrder`（`PENDING_PAYMENT`），写入 `rental_time_lock`，记录 `orderId`，并发布 RabbitMQ 待付款超时消息 |
| 6 | 系统 | 触发库存与超时能力 | 订单创建后调用 item-service 扣减可租库存，写入时间段锁，并发送 RabbitMQ 待付款超时消息；租借者后续通过 `POST /api/orders/{orderId}/pay` 和 `POST /api/orders/{orderId}/freeze-deposit` 完成租金支付与押金冻结 |

### 8.2 时间段库存与时间锁验证

| 场景 | 操作 | 预期 |
|---|---|---|
| 第二个重叠时间段转单被拒绝 | 用户 C 对同一物品发起与用户 B 时间段重叠的申请并双方确认转单 | 申请可创建（申请阶段不校验重叠、不写时间锁）；转单（订单创建）时返回 `RENTAL_TIME_STOCK_NOT_ENOUGH`，订单未创建 |
| 相邻非重叠时间段可转单 | 用户 C 对同一物品发起紧邻用户 B 时间段（不重叠）的申请并双方确认转单 | 申请与订单均创建成功，两个 `rental_time_lock` 时间段独立不冲突 |
| 取消申请（无锁可释放） | 用户 B 调用 `POST /api/rentals/applications/{id}/cancel` | 申请 `status=3`（CANCELLED）；申请阶段未写时间锁、未占用库存，无锁可释放 |
| 取消订单释放时间锁 | 物主或租借者在订单 `PENDING_PAYMENT`/`PAID_PENDING_DELIVERY` 阶段调用 `POST /api/orders/{id}/cancel` | 订单 `status=6`（CANCELLED），释放时间段库存与时间锁（`rental_time_lock`）；如已有支付或押金冻结，调用钱包取消支付 |
| 完成订单释放时间锁 | 出租者调用 `POST /api/orders/{id}/complete` 完成订单 | 订单 `status=5`（COMPLETED），释放时间段库存与时间锁（`rental_time_lock`）；调用钱包结算租金、押金和逾期费用 |

### 8.3 数据库状态校验

演示完成后需在数据库中校验以下状态：

- `rental_application.status` 与流程一致（主流程最终为 2 CONVERTED）。
- `rental_proposal` 存在 `versionNo=1` 和 `versionNo=2` 两条记录，`changedFields` 准确反映步骤 3 的变更字段。
- `rental_order` 存在一条记录，`applicationId` 关联申请，`status` 与流程阶段一致。
- `rental_time_lock` 在订单创建时插入（申请阶段不写），在取消订单或完成订单时被释放（`released=1` 或记录被删除，视实现而定）。
- 钱包流水表 `wallet_transaction` 可用于校验订单支付、押金冻结、取消退款和完成结算流水。

### 8.4 演示视频证据补充

阶段 5 演示视频需额外覆盖：

```text
用户 B 创建申请截图与响应
用户 A 修改 proposal 截图与响应（含 changedFields）
用户 B、用户 A 顺序确认截图与响应
订单创建后数据库 rental_order 表截图
第二个重叠时间段转单被拒绝（RENTAL_TIME_STOCK_NOT_ENOUGH）的错误响应截图
相邻非重叠时间段转单成功的响应截图
取消/完成订单后 rental_time_lock 表状态截图
钱包流水表截图（阶段 6/8 已接入订单支付、押金冻结、取消退款和完成结算）
```

## 8. 阶段 6 支付结算流程

1. User B 充值 500 元。
2. User A 发布物品（quantity=1, dailyPrice=10, depositAmount=50）。
3. User B 创建申请，双方确认，订单创建，状态 PENDING_PAYMENT。
4. User B 调 POST /api/orders/{orderId}/pay（amount=100），验证钱包扣 100，order.paidRentAmount=100。
5. User B 调 POST /api/orders/{orderId}/freeze-deposit（amount=50），验证钱包冻结 50，order.frozenDepositAmount=50。
6. 订单状态自动推进到 PAID_PENDING_DELIVERY。
7. User A ship → User B receive → User B return → User A complete。
8. 验证 owner 余额 += 100（租金收入），renter 押金释放回 50。
9. 验证 wallet_transactions 有对应流水。
10. 验证 order_settlements 有结算记录。
11. 重复流程但让 rentEndTime 已过，complete 时验证逾期费用计算。
12. 测试取消：创建新订单，pay 后 cancel，验证退款到 renter。

## 9. 阶段 7 即时通讯、评价与异议流程

本章节覆盖阶段 7 新增的 WebSocket 即时通讯、评价与异议三条主流程。接口字段以 `docs/03-api-contract.md` 第 11 章为准。

### 9.1 双用户发消息与 WebSocket 推送

1. User A 登录，User B 登录，分别获取 `userAToken`、`userBToken`。
2. User A 与 User B 通过物品详情页发起一次会话（或由后端在创建租借申请时自动建立会话），记录 `conversationId`。
3. 两个用户分别用 `ws://127.0.0.1:8080/ws/chat?token={jwt}` 建立 WebSocket 连接，验证连接成功（无 `POLICY_VIOLATION` 关闭）。
4. User A 调 `POST /api/messages/conversations/{conversationId}/messages`（`messageType=1`、`content=你好`），验证：
   - 接口返回 `MessageResponse`，`senderId=A`、`receiverId=B`、`isRead=0`。
   - User B 的 WebSocket 连接收到一条 JSON 推送，内容与 `MessageResponse` 一致。
   - User B 调 `GET /api/messages/unread-count`，`totalUnread=1`。
5. User B 调 `PUT /api/messages/conversations/{conversationId}/read`，验证 `unread-count` 归零；User A 再次发消息，验证 User B 未在线时消息只落库不推送（关闭 User B 的 WebSocket 后测试）。
6. User A 发送 `messageType=2` 图片消息（`imageUrls=["http://cdn/a.png"]`），验证会话 `lastMessageContent` 显示为 `[图片]`。
7. 心跳验证：User A WebSocket 连接建立后等待 60 秒以上，发送文本 `ping`，验证收到 `pong`，并检查 Redis `sr:message:online:{A}` key 仍存在（TTL 被刷新）。
8. 鉴权异常：不带 `token` 或 `token` 无效连接 `ws://127.0.0.1:8080/ws/chat`，验证连接被以 `POLICY_VIOLATION` 关闭。

### 9.2 订单完成后的评价流程

1. 沿用阶段 6 流程跑通一个完整订单：User B 支付租金 → 冻结押金 → User A ship → User B receive → User B return → User A complete，订单状态 `COMPLETED`。
2. 验证订单完成时双方都收到系统通知：双方 `GET /api/messages/conversations` 出现 `itemId=0`、`userAId=0` 的系统会话，`lastMessageContent=订单已完成，请评价对方`；若双方 WebSocket 在线则实时收到推送。
3. User B 调 `POST /api/orders/{orderId}/reviews`（`rating=5`、`content=物品不错`），验证返回 `ReviewResponse`，`reviewerId=B`、`revieweeId=A`。
4. User A 调 `POST /api/orders/{orderId}/reviews`（`rating=4`、`content=租客爱护物品`），验证成功。
5. 重复提交：User B 再次调创建评价接口，验证返回 `REVIEW_ALREADY_EXISTS(40310)`。
6. 权限校验：未参与订单的用户调创建评价接口，验证返回 `REVIEW_PERMISSION_DENIED(40312)`。
7. 状态校验：在订单 `RENTING` 状态下尝试创建评价，验证返回 `REVIEW_ORDER_NOT_COMPLETED(40313)`。
8. 查询：User A/B 调 `GET /api/orders/{orderId}/reviews`，验证返回两条记录，按 `createTime` 排序。

### 9.3 异议提交与管理端裁定

1. 创建新订单并推进到 `COMPLETED` 状态（或 `RENTING` 状态，只要订单的 renter/owner 之一即可提交异议）。
2. User B 调 `POST /api/orders/{orderId}/disputes`（`reason=物品损坏`、`description=归还时有划痕`、`expectedDepositDeduction=20`、`imageUrls=["http://cdn/d.png"]`），验证返回 `DisputeResponse`，`status=0`、`applicantId=B`、`adminId=null`。
3. 重复提交：User B 再次调创建异议接口，验证返回 `DISPUTE_ALREADY_EXISTS(40314)`。
4. 权限校验：未参与订单的用户调创建异议接口，验证返回 `DISPUTE_PERMISSION_DENIED(40316)`。
5. 验证订单主状态未因异议改变（异议不进入 `DISPUTING` 状态，沿用原订单状态）。
6. 查询：User A/B 调 `GET /api/orders/{orderId}/disputes`，验证返回一条记录。
7. 管理员登录获取 `adminToken`，调 `GET /api/admin/disputes?status=0`，验证返回包含上一步的异议。
8. 管理员调 `PUT /api/admin/disputes/{id}/resolve`（请求头使用 `adminToken`，请求体 `adminRemark=扣除 20 元押金`，不传 `adminId`），验证返回成功。
9. 再次查询异议：User B 调 `GET /api/orders/{orderId}/disputes`，验证 `status=2`、`adminId` 与 `adminRemark` 已写入、`updateTime` 已刷新。
10. 重复裁定：管理员再次调 resolve 接口，验证返回 `DISPUTE_STATUS_INVALID(40317)`。
11. 内部接口保护：直接通过浏览器或 Postman 访问 `GET /internal/rental/disputes`，验证 Gateway 返回 403（外部禁止访问内部接口）。

# 前端页面路由与组件设计

## 1. 前端项目

```text
frontend/        普通用户端
admin-frontend/  管理员后台
```

两个前端独立构建、独立登录页、独立路由。它们都只请求 Gateway。

技术建议：

```text
React + TypeScript + Vite
React Router
Zustand
Axios
Ant Design 5
lucide-react
```

说明：

- 普通端和管理端都可以使用 Ant Design 5 加速表单、表格、弹窗、上传等基础组件开发。
- 普通端需要通过自定义样式降低后台感，管理端优先使用 Ant Design 的 Table、Form、Modal、Tabs 等高效率组件。
- 图标优先使用 lucide-react，组件库图标只在 Ant Design 内置控件需要时使用。

## 2. 普通用户端路由

一级页面：

| 路由 | 页面 | 权限 | 说明 |
|---|---|---|---|
| `/items` | 租赁页 | 公开/可选登录 | 物品列表、搜索、分类、筛选 |
| `/my-items` | 我的物品 | 登录 | 我发布的物品、审核状态、上下架 |
| `/messages` | 消息页 | 登录 | 会话列表、未读 |
| `/orders` | 订单页 | 登录 | 我租入、我出租、状态筛选 |
| `/favorites` | 收藏页 | 登录 | 收藏物品 |
| `/profile` | 我的页 | 登录 | 资料、钱包、黑名单、设置 |

二级页面：

| 路由 | 页面 | 权限 |
|---|---|---|
| `/items/:id` | 物品详情页 | 公开/可选登录 |
| `/items/create` | 发布物品页 | 登录 |
| `/items/:id/edit` | 编辑物品页 | 物品拥有者 |
| `/messages/:conversationId` | 对话页 | 会话参与者 |
| `/orders/:id` | 订单详情页 | 订单参与者 |
| `/orders/:id/snapshot` | 订单物品快照页 | 订单参与者 |
| `/users/:id` | 用户详情页 | 公开/可选登录 |
| `/wallet` | 钱包页 | 登录 |
| `/blacklist` | 黑名单管理页 | 登录 |
| `/orders/:id/dispute` | 异议提交页 | 订单参与者 |

## 3. 管理员后台路由

| 路由 | 页面 |
|---|---|
| `/login` | 管理员登录页 |
| `/` | 后台首页 |
| `/users/audits` | 用户审核列表 |
| `/users/audits/:id` | 用户审核详情 |
| `/items/audits` | 物品审核列表 |
| `/items/audits/:id` | 物品审核详情 |
| `/bans` | 封禁管理 |
| `/disputes` | 异议查看 |
| `/logs` | 管理员操作日志 |

## 4. 普通端核心组件

| 组件 | 用途 |
|---|---|
| `AppLayout` | 顶栏、一级导航、移动端底栏、登录状态 |
| `ItemCard` | 物品列表卡片 |
| `ItemForm` | 发布/编辑物品表单 |
| `ImageUploader` | 图片上传、排序、删除、预览 |
| `RentalApplicationCard` | 申请卡片 |
| `ProposalEditor` | 协商字段编辑 |
| `OrderCard` | 订单列表卡片 |
| `OrderTimeline` | 订单状态时间线 |
| `WalletSummary` | 钱包余额与冻结金额 |
| `TransactionList` | 钱包流水 |
| `PaymentPanel` | 订单租金支付、押金冻结和补缴 |
| `OverdueFeePanel` | 订单详情中的逾期费用、押金扣除和欠费提示 |
| `ConversationList` | 会话列表 |
| `ChatPanel` | 对话消息面板 |
| `MessageComposer` | 文本/图片/卡片发送 |
| `ReviewDialog` | 评价弹窗 |
| `ImagePreviewDialog` | 图片预览 |

## 5. 管理端核心组件

| 组件 | 用途 |
|---|---|
| `AdminLayout` | 侧边栏、顶部账号、退出 |
| `AuditStatusBadge` | 审核状态标签 |
| `UserAuditPanel` | 用户资料审核详情 |
| `ItemAuditPanel` | 物品审核详情 |
| `BanUserDialog` | 封禁操作 |
| `DisputeDetailPanel` | 异议详情 |
| `OperationLogTable` | 操作日志列表 |

## 6. 状态与 API 分层

建议目录：

```text
src/
  components/
  components/ui/
  pages/
  services/
  stores/
  types/
  utils/
```

API 文件按服务拆分：

```text
services/auth.ts
services/item.ts
services/rental.ts
services/wallet.ts
services/message.ts
services/admin.ts
services/websocket.ts
```

共享类型放：

```text
types/api.ts
types/status.ts
```

## 7. UI 约束

- 不使用 emoji 作为主要 UI 图标，优先使用 lucide-react。
- 普通端偏生活化但保持清晰，不做营销式 landing page。
- 管理端偏工作台风格，信息密度高、筛选明确、操作可追踪。
- 表单必须给出字段级错误提示。
- 钱包余额小于等于 0 时，普通端必须在顶栏、我的页和钱包页展示欠费提示，并提供直接模拟充值入口。
- 移动端按钮和底栏不能遮挡内容。
- 页面实现后必须做桌面和移动端截图检查。

## 8. 关键前端验收流程

普通用户双人流程：

```text
用户 A 注册登录 -> 发布物品 -> 物品未审核但公开可见 -> 用户 B 注册登录
-> 浏览物品 -> 发起申请 -> 双方协商 -> 创建订单
-> B 支付租金和冻结押金 -> 金额满足后进入已付款待交付，否则 B 继续补缴
-> A 发货或面交 -> B 确认收到 -> B 归还 -> A 完成订单
-> 租金入账 A 钱包、逾期费用按规则结算、剩余押金退回 B 钱包 -> 双方评价
```

管理员流程：

```text
管理员登录 -> 审核用户资料 -> 审核物品 -> 要求整改 -> 普通端收到通知
-> 封禁用户 -> 普通端限制发布/申请/发消息 -> 查看异议和操作日志
```

# 数据库设计草案

## 1. 数据库拆分

| 数据库 | 服务 | 说明 |
|---|---|---|
| `sr_auth` | auth-service | 用户、资料审核字段、黑名单、信用分 |
| `sr_item` | item-service | 物品、分类、图片、收藏、审核、快照 |
| `sr_rental` | rental-service | 申请、协商版本、订单、时间段锁、评价、异议 |
| `sr_wallet` | wallet-service | 钱包账户、流水、押金冻结、支付记录 |
| `sr_message` | message-service | 会话、消息、未读、通知 |
| `sr_admin` | admin-service | 管理员操作日志、审核记录聚合、后台审计 |

数据库采用服务自治，不做跨库外键。跨服务引用字段保留业务 ID，并通过 Feign 查询或快照保存必要展示信息。

## 2. 通用字段

核心表默认包含：

```text
id BIGINT PRIMARY KEY AUTO_INCREMENT
create_time DATETIME
update_time DATETIME
deleted TINYINT DEFAULT 0
version INT DEFAULT 0
```

金额字段统一使用 `DECIMAL(10,2)`。状态字段使用 `TINYINT`，并在文档和枚举中同步说明。

## 3. sr_auth

### users

用途：普通用户与管理员账号。

核心字段：

```text
id
username
password
avatar_url
description
credit_score
role
status
username_audit_status
avatar_audit_status
description_audit_status
show_rental_history
last_login_time
create_time
update_time
deleted
version
```

约束与索引：

- `uk_username_deleted(username, deleted)`
- `idx_status(status)`
- `idx_role(role)`

### user_blacklist

用途：用户拉黑关系。

核心字段：

```text
id
user_id
target_user_id
reason
create_time
deleted
```

约束：

- `uk_user_target(user_id, target_user_id)`

### credit_score_records

用途：信用分变更记录。

核心字段：

```text
id
user_id
order_id
change_value
before_score
after_score
reason_type
reason
create_time
```

## 4. sr_item

### categories

用途：物品分类，P0 使用初始化数据，不做后台维护。

核心字段：

```text
id
name
sort_order
status
create_time
update_time
```

初始化分类：

```text
数码设备
摄影器材
户外露营
生活工具
图书教材
服饰道具
运动器材
其他
```

### items

用途：物品主表。

核心字段：

```text
id
owner_id
title
description
category_id
tags
quantity
rented_count
support_delivery
delivery_city
support_meetup
meetup_location
price_type
daily_price
min_rent_days
free_rent
deposit_enabled
deposit_amount
credit_deposit_enabled
min_credit_score
free_deposit_score
reduced_deposit_score
reduced_deposit_amount
status
audit_status
audit_reason
audit_time
audit_admin_id
version
create_time
update_time
deleted
```

索引：

- `idx_owner_id(owner_id)`
- `idx_category_status(category_id, status)`
- `idx_audit_status(audit_status)`
- `idx_create_time(create_time)`

### item_images

用途：物品图片。

核心字段：

```text
id
item_id
url
sort_order
create_time
deleted
```

### favorites

用途：收藏记录。

核心字段：

```text
id
user_id
item_id
status
create_time
update_time
```

约束：

- `uk_user_item(user_id, item_id)`

### item_snapshots

用途：订单创建时保存物品快照，避免物品后续编辑影响订单展示。

核心字段：

```text
id
item_id
owner_id
title
description
category_name
image_urls
price_type
daily_price
deposit_amount
support_delivery
support_meetup
snapshot_json
create_time
```

## 5. sr_rental

### rental_applications

用途：租借申请主表。

核心字段：

```text
id
item_id
owner_id
renter_id
conversation_id
status
current_proposal_id
owner_confirmed
renter_confirmed
prepaid_rent_amount
pre_frozen_deposit_amount
create_time
update_time
deleted
version
```

### rental_proposals

用途：每次申请或协商修改生成一条版本记录。

核心字段：

```text
id
application_id
version_no
operator_id
quantity
delivery_type
rent_start_time
rent_end_time
meetup_time
meetup_location
receiver_name
receiver_phone
receiver_address
rent_amount
deposit_amount
changed_fields
remark
create_time
```

约束：

- `uk_application_version(application_id, version_no)`

### rental_orders

用途：正式订单。

核心字段：

```text
id
order_no
application_id
proposal_id
item_id
item_snapshot_id
owner_id
renter_id
quantity
delivery_type
rent_start_time
rent_end_time
daily_price
rent_amount
deposit_amount
paid_rent_amount
frozen_deposit_amount
status
ship_company
ship_tracking_no
return_company
return_tracking_no
received_time
returned_time
completed_time
overdue_minutes
overdue_fee_amount
overdue_settled
cancel_reason
create_time
update_time
deleted
version
```

约束与索引：

- `uk_order_no(order_no)`
- `idx_owner_status(owner_id, status)`
- `idx_renter_status(renter_id, status)`
- `idx_item_time(item_id, rent_start_time, rent_end_time)`

### rental_time_locks

用途：时间段库存占用，作为防超租的最终依据。该表归 `rental-service` 管理，`item-service` 不写入时间段库存。

核心字段：

```text
id
order_id
item_id
quantity
rent_start_time
rent_end_time
status
create_time
update_time
```

索引：

- `idx_item_time_status(item_id, rent_start_time, rent_end_time, status)`

规则：

- 时间段使用半开区间 `[rent_start_time, rent_end_time)`。
- 有效占用状态对应订单的待付款、已付款待交付、已发货、租借中、待归还确认。
- 订单完成、取消或关闭后释放占用。

### order_status_histories

用途：订单状态历史。

核心字段：

```text
id
order_id
old_status
new_status
operator_id
remark
create_time
```

### reviews

用途：订单完成后的双方评价。

核心字段：

```text
id
order_id
item_id
reviewer_id
reviewee_id
rating
content
image_urls
status
create_time
update_time
```

约束：

- `uk_order_reviewer(order_id, reviewer_id)`

### disputes

用途：P0 异议入口。

核心字段：

```text
id
order_id
applicant_id
reason
description
expected_deposit_deduction
image_urls
status
admin_id
admin_remark
create_time
update_time
```

## 6. sr_wallet

### 钱包资金流

P0 采用模拟钱包，但资金流必须闭环：

1. 租借者模拟充值后，`wallet_accounts.balance` 增加，并写入一条“模拟充值”流水。
2. 租借者在订单创建后支付租金和冻结押金，资金绑定 `order_id`，申请阶段只记录协商方案和确认状态。
3. 双方确认 proposal 并创建订单后，租借者按 `order_id` 支付租金和冻结押金；金额已满足时订单进入已付款待交付，否则继续补缴。
4. 租借者可以多次支付租金和多次补冻结押金。
5. 租金先进入订单待结算记录，不立即进入出借者可用余额。
6. 订单完成时，租金从平台待结算金额转入出借者可用余额，写入出借者“租金收入”流水。
7. 订单完成时如发生逾期，逾期费用先从冻结押金扣除，押金不足部分继续扣租借者钱包余额；钱包余额允许扣成负数。
8. 逾期费用转入出借者钱包余额；剩余押金释放回租借者钱包余额。
9. 订单取消时，已支付租金退回租借者；已冻结押金取消或释放回租借者；库存占用同步释放。
10. 异议只记录入口，完整人工仲裁、押金赔偿分配放到 P1/P2。

### wallet_accounts

用途：用户钱包账户。

核心字段：

```text
id
user_id
balance
frozen_amount
status
create_time
update_time
version
```

约束：

- `uk_user_id(user_id)`

### wallet_transactions

用途：钱包流水。

核心字段：

```text
id
transaction_no
user_id
counterparty_user_id
application_id
proposal_id
order_id
type
direction
amount
balance_before
balance_after
frozen_before
frozen_after
remark
create_time
```

流水类型建议：

| type | 含义 | 方向 |
|---:|---|---|
| 1 | 模拟充值 | 收入 |
| 2 | 支付租金 | 支出 |
| 3 | 冻结押金 | 冻结 |
| 4 | 释放押金 | 解冻 |
| 5 | 租金收入 | 收入 |
| 6 | 取消退款 | 收入 |
| 7 | 逾期费用支出 | 支出 |
| 8 | 逾期费用收入 | 收入 |
| 9 | 押金扣除 | 支出 |

### deposit_freezes

用途：押金冻结记录。

核心字段：

```text
id
application_id
proposal_id
order_id
user_id
amount
deducted_amount
status
freeze_time
release_time
cancel_time
create_time
update_time
```

押金状态：

| status | 含义 |
|---:|---|
| 0 | 已冻结 |
| 1 | 已释放 |
| 2 | 已扣除 |
| 3 | 已取消 |
| 4 | 部分扣除后已释放 |

### order_settlements

用途：订单资金结算记录，保存已支付但未入账给出借者的租金，确保订单完成时租金入账和押金释放具有幂等依据。

核心字段：

```text
id
order_id
renter_id
owner_id
rent_amount
deposit_amount
overdue_fee_amount
deposit_deducted_amount
rent_settled
deposit_released
overdue_fee_settled
status
settle_time
create_time
update_time
```

约束：

- `uk_order_id(order_id)`

## 7. sr_message

### conversations

用途：会话摘要。

核心字段：

```text
id
item_id
user_a_id
user_b_id
last_message_content
last_message_time
user_a_unread_count
user_b_unread_count
create_time
update_time
```

约束：

- `uk_item_users(item_id, user_a_id, user_b_id)`

### messages

用途：消息明细。

核心字段：

```text
id
conversation_id
sender_id
receiver_id
message_type
card_type
content
image_urls
related_application_id
related_order_id
is_read
read_time
create_time
deleted
```

## 8. sr_admin

### admin_operation_logs

用途：后台操作日志。

核心字段：

```text
id
admin_id
operation_type
target_type
target_id
before_json
after_json
remark
ip
create_time
```

### audit_records

用途：审核记录。

核心字段：

```text
id
admin_id
target_type
target_id
field_name
old_status
new_status
reason
create_time
```

## 9. 后续落地要求

- 真实建表 SQL 放入 `backend/scripts/init.sql`。
- 初始化管理员账号必须使用 BCrypt 密文。
- 每个状态字段必须在 Java enum、API 文档、数据库注释中保持一致。
- 时间段库存并发测试必须以 `rental_time_locks` 和订单状态作为最终校验依据。

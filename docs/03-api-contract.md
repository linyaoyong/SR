# API 契约、状态码与错误码

## 1. 基础约定

Base URL：

```text
本机浏览器: http://127.0.0.1:8080
局域网设备: http://<开发机局域网IP>:8080
```

所有前端请求必须通过 Gateway。

认证头：

```text
Authorization: Bearer <accessToken>
```

内部调用头：

```text
X-Internal-Token: <internal-token>
```

Gateway 注入下游请求头：

```text
X-User-Id: <userId>
X-User-Role: <role>
```

## 2. 统一响应

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

分页：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "size": 20,
    "pages": 5
  }
}
```

失败：

```json
{
  "code": 40301,
  "message": "订单状态不允许执行该操作",
  "data": null
}
```

分页从 `page = 1` 开始，默认 `size = 20`，最大 `size = 100`。

## 3. HTTP 状态

| HTTP | 场景 |
|---:|---|
| 200 | 请求已被业务层处理，业务成功或业务失败都可使用统一响应 |
| 400 | 请求体格式错误、参数校验失败 |
| 401 | 未登录、Token 无效或过期 |
| 403 | 无权限、外部访问内部接口 |
| 404 | 路由不存在 |
| 429 | Sentinel 限流 |
| 500 | 系统异常或服务不可用 |

## 4. 错误码区间

| 区间 | 服务 | 示例 |
|---:|---|---|
| 40000-40099 | 通用错误 | 参数错误、未登录、无权限、内部接口禁止访问 |
| 40100-40199 | auth-service | 用户不存在、密码错误、账户已封禁 |
| 40200-40299 | item-service | 物品不存在、物品下架、审核未通过、图片过多 |
| 40300-40399 | rental-service | 申请不存在、信用分不足、时间段库存不足、订单状态不允许 |
| 40400-40499 | wallet-service | 余额不足、钱包欠费、押金冻结失败、支付金额不足 |
| 40500-40599 | message-service | 会话不存在、被拉黑、对方已封禁 |
| 40600-40699 | admin-service | 管理员权限不足、审核对象不存在 |
| 42900-42999 | Sentinel | 请求过于频繁、服务繁忙 |
| 50000-50099 | 系统错误 | 服务异常、远程调用失败、MQ 发送失败 |

## 5. 对外 API 清单

### auth-service

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| POST | `/api/auth/register` | 公开 | 普通用户注册 |
| POST | `/api/auth/login` | 公开 | 普通用户登录 |
| POST | `/api/auth/admin/login` | 公开 | 管理员登录 |
| POST | `/api/auth/refresh` | 公开 | 刷新 Token |
| GET | `/api/users/me` | 用户 | 当前用户资料 |
| PUT | `/api/users/me` | 用户 | 修改当前用户资料 |
| PUT | `/api/users/me/password` | 用户 | 修改密码 |
| GET | `/api/users/{id}` | 公开/可选登录 | 用户公开主页信息 |

认证响应示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "user_a",
    "role": "USER",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.demo.access",
    "refreshToken": "rt_user_1_20260627_demo_token",
    "expiresIn": 7200
  }
}
```

刷新令牌规则：登录写入 Redis `sr:auth:refresh:{userId}:{tokenId}`，TTL 30 天；`POST /api/auth/refresh` 使用 `Authorization: Bearer <refreshToken>`，使用 Redis 原子 GETDEL 消费旧 key，同一令牌最多使用一次；随后读取最新用户状态，封禁账户返回 `40102`，已删除账户返回 `40100`，不签发后继令牌。新 `accessToken` 与 `refreshToken` 使用最新用户名和角色，不能沿用 Redis 缓存中的旧角色。
| POST | `/api/users/blacklist/{targetUserId}` | 用户 | 拉黑用户 |
| DELETE | `/api/users/blacklist/{targetUserId}` | 用户 | 解除拉黑 |
| GET | `/api/users/blacklist` | 用户 | 黑名单列表 |
| POST | `/api/files/avatars` | 用户 | 上传头像 |

### item-service

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| GET | `/api/categories` | 公开 | 分类列表 |
| GET | `/api/items` | 公开/可选登录 | 物品列表 |
| GET | `/api/items/{id}` | 公开/可选登录 | 物品详情 |
| POST | `/api/items` | 用户 | 发布物品 |
| PUT | `/api/items/{id}` | 物品拥有者 | 编辑物品 |
| PUT | `/api/items/{id}/off-shelf` | 物品拥有者 | 下架 |
| PUT | `/api/items/{id}/re-list` | 物品拥有者 | 重新上架 |
| DELETE | `/api/items/{id}` | 物品拥有者 | 删除 |
| POST | `/api/items/{id}/images` | 物品拥有者 | 上传或追加图片 |
| GET | `/api/favorites` | 用户 | 我的收藏 |
| POST | `/api/favorites/{itemId}` | 用户 | 收藏 |
| DELETE | `/api/favorites/{itemId}` | 用户 | 取消收藏 |

### rental-service

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| POST | `/api/rentals/applications` | 用户 | 创建租借申请 |
| GET | `/api/rentals/applications` | 用户 | 我的申请列表 |
| GET | `/api/rentals/applications/{id}` | 申请参与者 | 申请详情 |
| PUT | `/api/rentals/applications/{id}/proposal` | 申请参与者 | 修改协商 proposal |
| PUT | `/api/rentals/applications/{id}/confirm` | 申请参与者 | 确认当前 proposal |
| POST | `/api/rentals/applications/{id}/cancel` | 申请参与者 | 取消申请 |
| GET | `/api/orders` | 用户 | 我的订单 |
| GET | `/api/orders/{id}` | 订单参与者 | 订单详情 |
| POST | `/api/orders/{id}/pay` | 租借者 | 支付租金 |
| POST | `/api/orders/{id}/freeze-deposit` | 租借者 | 冻结押金 |
| POST | `/api/orders/{id}/ship` | 出租者 | 快递发货 |
| POST | `/api/orders/{id}/receive` | 租借者 | 确认收到 |
| POST | `/api/orders/{id}/return` | 租借者 | 提交归还 |
| POST | `/api/orders/{id}/complete` | 出租者 | 确认收回并完成 |
| POST | `/api/orders/{id}/cancel` | 订单参与者 | 取消订单 |
| POST | `/api/orders/{id}/reviews` | 订单参与者 | 评价 |
| GET | `/api/orders/{id}/reviews` | 订单参与者 | 查看订单评价 |
| POST | `/api/orders/{id}/reviews/images` | 订单参与者 | 上传评价图片 |
| POST | `/api/orders/{id}/disputes` | 订单参与者 | 发起异议 |
| GET | `/api/orders/{id}/disputes` | 订单参与者 | 查看订单异议 |
| GET | `/api/rentals/items/{itemId}/reviews` | 公开 | 查看物品历史评价 |
| GET | `/api/rentals/users/{userId}/history` | 公开 | 查看用户公开租借历史 |
| GET | `/api/rentals/users/{userId}/reviews` | 公开 | 查看用户公开评价 |

### wallet-service

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| GET | `/api/wallet/me` | 用户 | 钱包首页 |
| POST | `/api/wallet/recharge` | 用户 | 模拟充值 |
| GET | `/api/wallet/transactions` | 用户 | 钱包流水 |

### message-service

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| GET | `/api/messages/conversations` | 用户 | 会话列表 |
| POST | `/api/messages/conversations/open` | 用户 | 按物品和对方用户创建或复用会话 |
| GET | `/api/messages/conversations/{conversationId}/messages` | 会话参与者 | 消息历史 |
| POST | `/api/messages/conversations/{conversationId}/messages` | 会话参与者 | 发送文字、图片或卡片消息 |
| POST | `/api/messages/images` | 用户 | 上传聊天图片，返回可嵌入消息的 URL |
| PUT | `/api/messages/conversations/{conversationId}/read` | 会话参与者 | 标记已读 |
| GET | `/api/messages/unread-count` | 用户 | 未读总数 |
| WS | `/ws/chat?token={jwt}` | 用户 | WebSocket 接收推送 |

### admin-service

| 方法 | 路径 | 权限 | 功能 |
|---|---|---|---|
| GET | `/api/admin/dashboard` | 管理员 | 后台首页统计 |
| GET | `/api/admin/users/audits` | 管理员 | 用户资料审核列表 |
| POST | `/api/admin/users/{id}/audit` | 管理员 | 用户资料审核 |
| POST | `/api/admin/users/{id}/ban` | 管理员 | 封禁用户 |
| POST | `/api/admin/users/{id}/unban` | 管理员 | 解封用户 |
| GET | `/api/admin/items/audits` | 管理员 | 物品审核列表 |
| POST | `/api/admin/items/{id}/audit` | 管理员 | 物品审核 |
| POST | `/api/admin/items/{id}/force-off-shelf` | 管理员 | 强制下架 |
| GET | `/api/admin/disputes` | 管理员 | 异议列表 |
| GET | `/api/admin/logs` | 管理员 | 操作日志 |

## 6. 内部 Feign API

| 提供方 | 方法 | 路径 | 调用方 | 用途 |
|---|---|---|---|---|
| auth-service | GET | `/internal/users/{id}/public` | item/rental/message/admin | 查询用户公开信息 |
| auth-service | GET | `/internal/users/{id}/status` | rental/message/admin | 查询封禁和信用状态 |
| auth-service | GET | `/internal/admin/users/stats` | admin | 管理端首页用户总数与封禁数统计 |
| item-service | GET | `/internal/items/{id}/info` | rental/message | 查询物品核心信息（含首图 URL） |
| item-service | POST | `/internal/items/{id}/snapshots` | rental | 创建订单物品快照 |
| item-service | POST | `/internal/items/{id}/rentals/reserve` | rental | 订单创建后按数量扣减可租库存 |
| item-service | POST | `/internal/items/{id}/rentals/release` | rental | 订单取消或完成后释放可租库存 |
| item-service | GET | `/internal/items/snapshots/{id}` | rental | 查询订单物品快照 |
| item-service | GET | `/internal/admin/items/stats` | admin | 管理端首页物品总数统计 |
| wallet-service | GET | `/internal/wallet/users/{userId}/usable` | item/rental/message | 校验用户钱包余额是否大于 0，欠费用户不能发起新交易动作 |
| wallet-service | POST | `/internal/wallet/orders/{orderId}/prepay-rent` | rental | 订单租金预支付或补缴 |
| wallet-service | POST | `/internal/wallet/orders/{orderId}/freeze-deposit` | rental | 订单押金冻结或补冻结 |
| wallet-service | POST | `/internal/wallet/orders/{orderId}/settle` | rental | 订单完成时触发结算：租金和逾期费用入账出借者，押金扣除或释放给租借者 |
| wallet-service | POST | `/internal/wallet/orders/{orderId}/cancel` | rental | 订单取消，退还已支付租金并取消或释放押金 |
| wallet-service | GET | `/internal/wallet/orders/{orderId}/settlement` | rental | 查询订单结算详情 |
| message-service | POST | `/internal/messages/system` | rental/admin/wallet | 创建系统通知 |
| message-service | POST | `/internal/messages/cards` | rental | 创建租借申请/订单卡片消息 |

## 7. 每个接口必须补齐的字段

后续生成正式 API 文档时，每个接口必须包含：

```text
接口名称
功能说明
请求方法
请求路径
权限要求
请求头
请求参数
请求体
成功响应
失败响应
错误码
业务规则
相关数据库表
是否触发 MQ
是否使用 Redis
是否存在 Feign 调用
```

## 8. 阶段 4 接口详细字段

本章节补充阶段 4 物品、收藏、用户资料、文件上传、管理端接口的请求与响应字段。字段定义以 `backend/` 下各服务 DTO 实际声明为准；统一响应外壳（`code` / `message` / `data`）参见第 2 节,未特殊说明的成功响应 HTTP 状态均为 200。

物品状态枚举:`status` 1=已上架、2=已下架、3=强制下架;`auditStatus` 0=待审核、1=审核通过、2=要求整改。物品删除走 MyBatis-Plus `@TableLogic` 软删除字段 `deleted`（0=未删除、1=已删除），不是 `status` 字段。

### 8.1 物品接口

#### POST /api/items

- 功能:发布物品
- 权限:用户（账户 `status=0` 且钱包可用）
- 请求头:`Authorization: Bearer <token>`
- 请求体（`ItemCreateRequest`）:

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| title | string | 是 | 2-60 | 标题 |
| description | string | 是 | 1-2000 | 描述 |
| categoryId | long | 是 | - | 分类 id |
| tags | string | 否 | - | 标签 |
| quantity | int | 是 | min=1 | 库存 |
| supportDelivery | int | 否 | 0/1 | 是否支持快递 |
| deliveryCity | string | 否 | - | 快递城市 |
| supportMeetup | int | 否 | 0/1 | 是否支持面交 |
| meetupLocation | string | 否 | - | 面交地点 |
| priceType | int | 否 | - | 价格类型 |
| dailyPrice | BigDecimal | 是 | >=0 | 日租金 |
| minRentDays | int | 否 | min=1 | 最少租用天数 |
| freeRent | int | 否 | - | 免租天数 |
| depositEnabled | int | 否 | 0/1 | 是否启用押金 |
| depositAmount | BigDecimal | 否 | >=0 | 押金金额 |
| creditDepositEnabled | int | 否 | 0/1 | 是否启用信用免押 |
| minCreditScore | int | 否 | - | 免押最低信用分 |
| freeDepositScore | int | 否 | - | 全免信用分阈值 |
| reducedDepositScore | int | 否 | - | 减押信用分阈值 |
| reducedDepositAmount | BigDecimal | 否 | >=0 | 减押后金额 |

- 成功响应 `data`:`ItemDetailResponse`（结构见 8.1 GET /api/items/{id}）
- 错误码:`VALIDATION_ERROR`、`AUTH_ACCOUNT_BANNED`、`ITEM_PUBLISH_FORBIDDEN`、`ITEM_CATEGORY_NOT_FOUND`
- 业务规则:新物品默认 `status=1`、`audit_status=0`;最多 9 张图片

#### PUT /api/items/{id}

- 功能:编辑物品
- 权限:物品拥有者
- 路径参数:`id` 物品 id
- 请求头:`Authorization: Bearer <token>`
- 请求体（`ItemUpdateRequest`,所有字段均可选,约束与 `ItemCreateRequest` 一致,但 `title`/`description`/`categoryId`/`quantity`/`dailyPrice` 不要求 `@NotNull`）:

| 字段 | 类型 | 约束 |
|---|---|---|
| title | string | 2-60 |
| description | string | 1-2000 |
| categoryId | long | - |
| tags | string | - |
| quantity | int | min=1 |
| supportDelivery | int | 0/1 |
| deliveryCity | string | - |
| supportMeetup | int | 0/1 |
| meetupLocation | string | - |
| priceType | int | - |
| dailyPrice | BigDecimal | >=0 |
| minRentDays | int | min=1 |
| freeRent | int | - |
| depositEnabled | int | 0/1 |
| depositAmount | BigDecimal | >=0 |
| creditDepositEnabled | int | 0/1 |
| minCreditScore | int | - |
| freeDepositScore | int | - |
| reducedDepositScore | int | - |
| reducedDepositAmount | BigDecimal | >=0 |

- 成功响应 `data`:`null`（仅返回统一响应外壳）
- 错误码:`VALIDATION_ERROR`、`ITEM_NOT_FOUND`、`ITEM_OWNER_REQUIRED`
- 业务规则:编辑后 `audit_status` 重置为 0（待审核）

#### POST /api/items/{id}/images

- 功能:上传或追加物品图片
- 权限:物品拥有者
- 路径参数:`id` 物品 id
- 请求头:`Authorization: Bearer <token>`,`Content-Type: multipart/form-data`
- 请求体:form-data,字段名 `file`,可重复（支持多文件上传）
- 成功响应 `data`:`ItemImageResponse` 列表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 图片 id |
| url | string | 访问 URL |
| sortOrder | int | 排序序号 |

- 错误码:`ITEM_NOT_FOUND`、`ITEM_OWNER_REQUIRED`、`ITEM_IMAGE_TOO_MANY`、`FILE_TYPE_NOT_SUPPORTED`、`FILE_TOO_LARGE`、`FILE_UPLOAD_FAILED`
- 业务规则:单物品最多 9 张图片

#### GET /api/items

- 功能:物品列表（公开,可选登录）
- 权限:公开
- 请求头:可选 `Authorization: Bearer <token>`
- 查询参数:

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| categoryId | long | 否 | - | 分类筛选 |
| keyword | string | 否 | - | 关键词 |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量 |

- 成功响应 `data`:分页 `PageResult<ItemListResponse>`,records 元素结构:

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 物品 id |
| title | string | 标题 |
| categoryId | long | 分类 id |
| dailyPrice | BigDecimal | 日租金 |
| minRentDays | int | 最少租用天数 |
| depositAmount | BigDecimal | 押金金额 |
| status | int | 物品状态 |
| auditStatus | int | 审核状态 |
| firstImageUrl | string | 首图 URL |
| createTime | LocalDateTime | 创建时间 |

#### GET /api/items/{id}

- 功能:物品详情（公开,可选登录）
- 权限:公开
- 路径参数:`id` 物品 id
- 请求头:可选 `Authorization: Bearer <token>`
- 成功响应 `data`:`ItemDetailResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 物品 id |
| ownerId | long | 物主用户 id |
| title | string | 标题 |
| description | string | 描述 |
| categoryId | long | 分类 id |
| tags | string | 标签 |
| quantity | int | 库存 |
| rentedCount | int | 已租数量 |
| supportDelivery | int | 是否支持快递 |
| deliveryCity | string | 快递城市 |
| supportMeetup | int | 是否支持面交 |
| meetupLocation | string | 面交地点 |
| priceType | int | 价格类型 |
| dailyPrice | BigDecimal | 日租金 |
| minRentDays | int | 最少租用天数 |
| freeRent | int | 免租天数 |
| depositEnabled | int | 是否启用押金 |
| depositAmount | BigDecimal | 押金金额 |
| creditDepositEnabled | int | 是否启用信用免押 |
| minCreditScore | int | 免押最低信用分 |
| freeDepositScore | int | 全免信用分阈值 |
| reducedDepositScore | int | 减押信用分阈值 |
| reducedDepositAmount | BigDecimal | 减押后金额 |
| status | int | 物品状态 |
| auditStatus | int | 审核状态 |
| auditReason | string | 审核原因 |
| images | array | `ItemImageResponse` 列表 |
| createTime | LocalDateTime | 创建时间 |

- 错误码:`ITEM_NOT_FOUND`、`ITEM_OFF_SHELF`

#### GET /api/items/mine

- 功能:我的物品列表
- 权限:用户
- 请求头:`Authorization: Bearer <token>`
- 查询参数:`page`（默认 1）、`size`（默认 20）
- 成功响应 `data`:分页 `PageResult<ItemListResponse>`（结构同 8.1 GET /api/items）
- 错误码:`UNAUTHORIZED`

#### PUT /api/items/{id}/off-shelf

- 功能:下架物品
- 权限:物品拥有者
- 路径参数:`id` 物品 id
- 请求头:`Authorization: Bearer <token>`
- 成功响应 `data`:`null`
- 错误码:`ITEM_NOT_FOUND`、`ITEM_OWNER_REQUIRED`
- 业务规则:`status` 置为 2（下架）

#### PUT /api/items/{id}/re-list

- 功能:重新上架物品
- 权限:物品拥有者
- 路径参数:`id` 物品 id
- 请求头:`Authorization: Bearer <token>`
- 成功响应 `data`:`null`
- 错误码:`ITEM_NOT_FOUND`、`ITEM_OWNER_REQUIRED`
- 业务规则:`status` 置为 1（在架）;若 `audit_status=2`（整改）需先编辑通过审核

#### DELETE /api/items/{id}

- 功能:删除物品
- 权限:物品拥有者
- 路径参数:`id` 物品 id
- 请求头:`Authorization: Bearer <token>`
- 成功响应 `data`:`null`
- 错误码:`ITEM_NOT_FOUND`、`ITEM_OWNER_REQUIRED`
- 业务规则:软删除,通过 MyBatis-Plus `@TableLogic` 将 `deleted` 字段置为 1；`status` 字段保持原值不变

### 8.2 分类与收藏

#### GET /api/categories

- 功能:分类列表（公开）
- 权限:公开
- 成功响应 `data`:`CategoryResponse` 列表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 分类 id |
| name | string | 分类名 |
| sortOrder | int | 排序序号 |
| status | int | 启用状态 |

#### GET /api/favorites

- 功能:我的收藏列表
- 权限:用户
- 请求头:`Authorization: Bearer <token>`
- 成功响应 `data`:`FavoriteResponse` 列表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 收藏记录 id |
| itemId | long | 物品 id |
| itemTitle | string | 物品标题 |
| dailyPrice | BigDecimal | 日租金 |
| firstImageUrl | string | 首图 URL |
| createTime | LocalDateTime | 收藏时间 |

- 错误码:`UNAUTHORIZED`

#### POST /api/favorites/{itemId}

- 功能:收藏物品
- 权限:用户
- 路径参数:`itemId` 物品 id
- 请求头:`Authorization: Bearer <token>`
- 成功响应 `data`:`null`
- 错误码:`ITEM_NOT_FOUND`、`ITEM_OFF_SHELF`

#### DELETE /api/favorites/{itemId}

- 功能:取消收藏
- 权限:用户
- 路径参数:`itemId` 物品 id
- 请求头:`Authorization: Bearer <token>`
- 成功响应 `data`:`null`
- 错误码:`ITEM_NOT_FOUND`

### 8.3 用户资料与文件

#### POST /api/files/avatars

- 功能:上传头像
- 权限:用户
- 请求头:`Authorization: Bearer <token>`,`Content-Type: multipart/form-data`
- 请求体:form-data,字段名 `file`,单文件
- 成功响应 `data`:`FileUploadResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| url | string | 访问 URL |
| filename | string | 文件名 |
| contentType | string | MIME 类型 |
| size | long | 字节数 |

- 错误码:`FILE_TYPE_NOT_SUPPORTED`、`FILE_TOO_LARGE`、`FILE_UPLOAD_FAILED`
- 业务规则:新头像需重新审核,`avatar_audit_status` 重置为 0

#### PUT /api/users/me

- 功能:修改当前用户资料
- 权限:用户
- 请求头:`Authorization: Bearer <token>`
- 请求体（`UpdateUserProfileRequest`,所有字段可选）:

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| username | string | 否 | 3-20 | 用户名（修改后需重新审核） |
| description | string | 否 | max=255 | 简介 |
| showRentalHistory | int | 否 | 0/1 | 是否展示租借历史 |

- 成功响应 `data`:`UserMeResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 用户 id |
| username | string | 用户名 |
| avatarUrl | string | 头像 URL |
| description | string | 简介 |
| creditScore | int | 信用分 |
| role | int | 角色（0 用户、1 管理员） |
| status | int | 账户状态（0 正常、1 封禁） |
| usernameAuditStatus | int | 用户名审核状态 |
| avatarAuditStatus | int | 头像审核状态 |
| descriptionAuditStatus | int | 简介审核状态 |
| showRentalHistory | int | 是否展示租借历史 |
| lastLoginTime | LocalDateTime | 上次登录时间 |

- 错误码:`VALIDATION_ERROR`、`AUTH_USERNAME_EXISTS`、`AUTH_USERNAME_INVALID`
- 业务规则:修改用户名后 `username_audit_status` 重置为 0

#### PUT /api/users/me/password

- 功能:修改密码
- 权限:用户
- 请求头:`Authorization: Bearer <token>`
- 请求体（`UpdatePasswordRequest`）:

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| oldPassword | string | 是 | - | 原密码 |
| newPassword | string | 是 | 6-32 | 新密码 |

- 成功响应 `data`:`null`
- 错误码:`VALIDATION_ERROR`、`AUTH_BAD_CREDENTIALS`

### 8.4 管理端接口

管理端接口统一要求:请求头 `Authorization: Bearer <adminToken>`、`Content-Type: application/json`（除说明外）,权限为管理员,`X-User-Id` 由 Gateway 注入。

#### GET /api/admin/dashboard

- 功能:后台首页统计
- 成功响应 `data`:`AdminDashboardResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| itemAuditCount | long | 物品审核总数（来自 item-service 审核列表计数） |
| userAuditCount | long | 用户资料审核总数（来自 auth-service 审核列表计数） |
| totalUsers | long | 用户总数（含普通用户与管理员，排除逻辑删除；来自 auth-service `GET /internal/admin/users/stats`） |
| totalItems | long | 物品总数（排除逻辑删除；来自 item-service `GET /internal/admin/items/stats`） |
| bannedUsers | long | 封禁用户数（`status=1`；来自 auth-service `GET /internal/admin/users/stats`） |

- 业务规则:`totalUsers`/`totalItems`/`bannedUsers` 通过 Feign 实时拉取；远程失败时回退到 0 并记录 warn 日志，避免拖垮整个 dashboard。

#### GET /api/admin/users/audits

- 功能:用户资料审核列表
- 查询参数:`auditStatus`（可选,审核状态；不传时返回任一字段处于待审核/要求整改的全部记录）
- 成功响应 `data`:`UserAuditItemResponse` 列表

| 字段 | 类型 | 说明 |
|---|---|---|
| userId | long | 用户 id |
| username | string | 用户名 |
| fieldName | string | 首选审核字段名（username/avatar/description），用作通过/整改操作的目标字段 |
| auditStatus | int | 首选字段当前审核状态 |
| reason | string | 审核原因（列表恒为 null，由 admin-service audit_records 持久化） |
| avatarUrl | string | 用户头像 URL，便于审核头像字段时直接查看 |
| description | string | 用户简介原文，便于审核简介字段时直接查看 |
| creditScore | int | 信用分，辅助判断是否需要更严格审查 |
| status | int | 用户状态：0=正常 1=已封禁 |
| showRentalHistory | int | 是否公开租赁历史 |
| usernameAuditStatus | int | 用户名审核状态 |
| avatarAuditStatus | int | 头像审核状态 |
| descriptionAuditStatus | int | 简介审核状态 |
| fieldNames | string[] | 该用户所有匹配当前筛选的待审字段名列表（一行展示全部待审字段） |
| auditStatuses | int[] | 与 fieldNames 一一对应的审核状态列表 |

- 业务规则：每个用户在列表中只占一行；`fieldNames` 收集所有匹配 `auditStatus` 筛选的字段（`auditStatus=null` 时含待审核 0 与要求整改 2），空头像/简介且已通过的字段不列出；`fieldName`/`auditStatus` 取 `fieldNames`/`auditStatuses` 的首个元素，保证旧操作逻辑兼容。

#### POST /api/admin/users/{id}/audit

- 功能:用户资料审核
- 路径参数:`id` 用户 id
- 请求体（`AdminAuditRequest`）:

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| auditStatus | int | 是 | 1-2 | 1=通过、2=整改 |
| auditReason | string | 否 | max=255 | 审核原因 |
| fieldName | string | 否 | - | 审核的用户资料字段名 |

- 成功响应 `data`:`null`
- 错误码:`VALIDATION_ERROR`、`ADMIN_NOT_ALLOWED`、`ADMIN_AUDIT_TARGET_NOT_FOUND`
- 业务规则:写入 `admin_log`,触发系统通知

#### POST /api/admin/users/{id}/ban

- 功能:封禁用户
- 路径参数:`id` 用户 id
- 请求体（`AdminBanRequest`）:

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| reason | string | 否 | max=255 | 封禁原因 |

- 成功响应 `data`:`null`
- 错误码:`ADMIN_NOT_ALLOWED`、`ADMIN_AUDIT_TARGET_NOT_FOUND`
- 业务规则:用户 `status` 置为 1（封禁）;已封禁用户的新请求被 Gateway 拦截;写入 `admin_log`

#### POST /api/admin/users/{id}/unban

- 功能:解封用户
- 路径参数:`id` 用户 id
- 无请求体
- 成功响应 `data`:`null`
- 错误码:`ADMIN_NOT_ALLOWED`、`ADMIN_AUDIT_TARGET_NOT_FOUND`
- 业务规则:用户 `status` 置为 0（正常）;写入 `admin_log`

#### GET /api/admin/items/audits

- 功能:物品审核列表
- 查询参数:`auditStatus`（可选,审核状态）
- 成功响应 `data`:`ItemAuditResponse` 列表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 物品 id |
| ownerId | long | 物主用户 id |
| title | string | 标题 |
| status | int | 物品状态 |
| auditStatus | int | 审核状态 |
| auditReason | string | 审核原因 |
| auditTime | LocalDateTime | 审核时间 |
| auditAdminId | long | 审核管理员 id |
| description | string | 物品描述正文 |
| categoryId | long | 分类 id |
| categoryName | string | 分类名称，便于审核员阅读 |
| tags | string | 标签 |
| priceType | int | 计价方式：0=免费 1=按天 |
| dailyPrice | decimal | 日租金 |
| depositAmount | decimal | 押金金额 |
| quantity | int | 数量 |
| imageUrls | string[] | 物品图片 URL 列表，按 sort_order 升序 |

#### POST /api/admin/items/{id}/audit

- 功能:物品审核
- 路径参数:`id` 物品 id
- 请求体（`AdminAuditRequest`,结构同用户资料审核,但 `fieldName` 被忽略）
- 成功响应 `data`:`ItemAuditActionResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| itemId | long | 物品 id |
| ownerId | long | 物主用户 id |
| auditStatus | int | 审核状态 |
| auditReason | string | 审核原因 |
| auditTime | LocalDateTime | 审核时间 |
| auditAdminId | long | 审核管理员 id |

- 错误码:`VALIDATION_ERROR`、`ADMIN_NOT_ALLOWED`、`ADMIN_AUDIT_TARGET_NOT_FOUND`
- 业务规则:`audit_status=1` 时 `status` 保持 1（在架）;`audit_status=2` 时 `status` 置为 2（下架待整改）;写入 `admin_log`,触发系统通知

#### POST /api/admin/items/{id}/force-off-shelf

- 功能:强制下架物品
- 路径参数:`id` 物品 id
- 无请求体
- 成功响应 `data`:`null`
- 错误码:`ADMIN_NOT_ALLOWED`、`ADMIN_AUDIT_TARGET_NOT_FOUND`
- 业务规则:物品 `status` 置为 2、`audit_status` 置为 2;写入 `admin_log`,触发系统通知

#### GET /api/admin/logs

- 功能:操作日志列表
- 查询参数:`page`（默认 1）、`size`（默认 20,上限 100）
- 成功响应 `data`:`AdminLogResponse` 列表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 日志 id |
| adminId | long | 操作管理员 id |
| operationType | string | 操作类型 |
| targetType | string | 目标类型 |
| targetId | long | 目标 id |
| remark | string | 备注 |
| ip | string | 操作 IP |
| createTime | LocalDateTime | 创建时间 |

## 9. 阶段 5 接口详细字段

本章节补充阶段 5 租借申请与订单接口的请求与响应字段。字段定义以 `backend/rental-service/` 下 DTO 实际声明为准；统一响应外壳（`code` / `message` / `data`）参见第 2 节，未特殊说明的成功响应 HTTP 状态均为 200。

阶段 5 状态枚举：

- `ApplicationStatusEnum`：`NEGOTIATING(0)` 协商中、`CONFIRMED(1)` 双方确认、`CONVERTED(2)` 已转订单、`CANCELLED(3)` 已取消。
- `OrderStatusEnum`：`PENDING_PAYMENT(0)` 待支付、`PAID_PENDING_DELIVERY(1)` 已付款待交付、`SHIPPED(2)` 已发货、`RENTING(3)` 租借中、`PENDING_RETURN_CONFIRM(4)` 待归还确认、`COMPLETED(5)` 已完成、`CANCELLED(6)` 已取消、`DISPUTING(7)` 异议中、`CLOSED(8)` 已关闭。
- `DeliveryTypeEnum`：`MEETUP(0)` 面交、`EXPRESS(1)` 快递。

Rental 端点请求头：`Authorization: Bearer <token>`、`Content-Type: application/json`（除说明外）。`X-User-Id` 由 Gateway 从 JWT 提取后注入下游业务服务，前端无需显式传递。

### 9.1 租借申请接口

#### POST /api/rentals/applications

- 功能：创建租借申请
- 权限：用户（账户 `status=0`，钱包可用，未被物主拉黑）
- 请求体（`RentalApplicationCreateRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| itemId | long | 是 | - | 物品 id |
| quantity | int | 是 | min=1 | 租借数量 |
| rentStartTime | LocalDateTime | 是 | - | 租借开始时间 |
| rentEndTime | LocalDateTime | 是 | 晚于 rentStartTime | 租借结束时间 |
| deliveryType | int | 是 | 0/1 | 交付方式：0 面交、1 快递 |
| meetupTime | LocalDateTime | 否 | - | 面交时间（deliveryType=0 时填写） |
| meetupLocation | string | 否 | - | 面交地点 |
| receiverName | string | 否 | - | 收件人姓名（deliveryType=1 时填写） |
| receiverPhone | string | 否 | - | 收件人电话 |
| receiverAddress | string | 否 | - | 收件地址 |
| rentAmount | BigDecimal | 否 | >=0 | 协商租金 |
| depositAmount | BigDecimal | 否 | >=0 | 协商押金 |
| remark | string | 否 | - | 备注 |

- 成功响应 `data`：`RentalApplicationResponse`（结构见 9.1 GET /api/rentals/applications/{id}）
- 错误码：`VALIDATION_ERROR`、`RENTAL_OWNER_CANNOT_RENT_OWN_ITEM`、`RENTAL_ITEM_UNAVAILABLE`、`RENTAL_TIME_INVALID`
- 业务规则：申请初始 `status=0`（NEGOTIATING）、`renterConfirmed=1`（申请人即为租借者，`ownerConfirmed=0`）；同时生成首版 proposal（`versionNo=1`）。申请阶段不占用时间段库存、不写入时间锁，库存占用与时间锁在订单创建时处理（见 9.1 PUT /api/rentals/applications/{id}/confirm）。

#### GET /api/rentals/applications

- 功能：我的申请列表（参与方视角，租借者或物主均可）
- 权限：用户
- 无查询参数（返回当前用户作为租借者或物主参与的全部申请，按 `createTime` 倒序，未分页）
- 成功响应 `data`：`List<RentalApplicationResponse>`，元素结构：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 申请 id |
| itemId | long | 物品 id |
| renterId | long | 租借者 id |
| ownerId | long | 物主 id |
| status | int | 申请状态（ApplicationStatusEnum） |
| currentProposalId | long | 当前 proposal id |
| ownerConfirmed | int | 物主是否确认（0/1） |
| renterConfirmed | int | 租借者是否确认（0/1） |
| createTime | LocalDateTime | 创建时间 |
| currentProposal | object | 当前 proposal（`RentalProposalResponse`，结构见 9.1 PUT /api/rentals/applications/{id}/proposal 响应） |

- 错误码：`UNAUTHORIZED`

#### GET /api/rentals/applications/{id}

- 功能：申请详情
- 权限：申请参与者（租借者或物主）
- 路径参数：`id` 申请 id
- 成功响应 `data`：`RentalApplicationDetailResponse`（继承 `RentalApplicationResponse` 全部字段，并附加 `itemTitle`）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 申请 id |
| itemId | long | 物品 id |
| itemTitle | string | 物品标题 |
| renterId | long | 租借者 id |
| ownerId | long | 物主 id |
| status | int | 申请状态 |
| currentProposalId | long | 当前 proposal id |
| ownerConfirmed | int | 物主是否确认 |
| renterConfirmed | int | 租借者是否确认 |
| createTime | LocalDateTime | 创建时间 |
| currentProposal | object | 当前 proposal（`RentalProposalResponse`） |

- 错误码：`RENTAL_APPLICATION_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`

#### PUT /api/rentals/applications/{id}/proposal

- 功能：修改协商 proposal
- 权限：申请参与者（租借者或物主）
- 路径参数：`id` 申请 id
- 请求体（`RentalProposalUpdateRequest`，所有字段可选）：

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| quantity | int | min=1 | 租借数量 |
| rentStartTime | LocalDateTime | - | 租借开始时间 |
| rentEndTime | LocalDateTime | - | 租借结束时间 |
| meetupTime | LocalDateTime | - | 面交时间 |
| meetupLocation | string | - | 面交地点 |
| receiverName | string | - | 收件人姓名 |
| receiverPhone | string | - | 收件人电话 |
| receiverAddress | string | - | 收件地址 |
| rentAmount | BigDecimal | >=0 | 协商租金 |
| depositAmount | BigDecimal | >=0 | 协商押金 |
| remark | string | - | 备注 |

- 成功响应 `data`：`RentalProposalResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | proposal id |
| applicationId | long | 申请 id |
| versionNo | int | 版本号，每次修改自增 |
| operatorId | long | 操作人 id |
| quantity | int | 租借数量 |
| deliveryType | int | 交付方式（DeliveryTypeEnum） |
| rentStartTime | LocalDateTime | 租借开始时间 |
| rentEndTime | LocalDateTime | 租借结束时间 |
| meetupTime | LocalDateTime | 面交时间 |
| meetupLocation | string | 面交地点 |
| receiverName | string | 收件人姓名 |
| receiverPhone | string | 收件人电话 |
| receiverAddress | string | 收件地址 |
| rentAmount | BigDecimal | 协商租金 |
| depositAmount | BigDecimal | 协商押金 |
| changedFields | string | 本次变更字段列表（逗号分隔） |
| remark | string | 备注 |
| createTime | LocalDateTime | 创建时间 |

- 错误码：`VALIDATION_ERROR`、`RENTAL_APPLICATION_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_APPLICATION_STATUS_INVALID`、`RENTAL_PROPOSAL_NOT_FOUND`、`RENTAL_TIME_INVALID`
- 业务规则：仅 `status=0`（NEGOTIATING）的申请可修改；修改生成新版本 proposal（`versionNo` 自增）并重置确认标记：操作方置 1、反方置 0（物主操作则 `ownerConfirmed=1`、`renterConfirmed=0`；租借者操作则反之）；若修改 `rentStartTime`/`rentEndTime`，仅校验开始时间早于结束时间（`RENTAL_TIME_INVALID`），不重新校验时间段库存与时间锁（在订单创建时校验）。

#### PUT /api/rentals/applications/{id}/confirm

- 功能：确认当前 proposal
- 权限：申请参与者（租借者或物主）
- 路径参数：`id` 申请 id
- 无请求体
- 成功响应 `data`：`RentalApplicationResponse`（结构见 9.1 GET /api/rentals/applications/{id}）
- 错误码：`RENTAL_APPLICATION_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_APPLICATION_STATUS_INVALID`、`RENTAL_PROPOSAL_NOT_FOUND`
- 业务规则：当前用户对应的 `ownerConfirmed` 或 `renterConfirmed` 置 1；当双方均确认后，申请 `status` 置 1（CONFIRMED），随即转换为订单（`status=2` CONVERTED）并创建 `RentalOrder`（`PENDING_PAYMENT` 状态），此时写入时间锁记录（`rental_time_lock`）并校验时间段库存与重叠（`RENTAL_TIME_STOCK_NOT_ENOUGH`）。订单创建后发送 RabbitMQ 待付款超时 TTL 消息。

#### POST /api/rentals/applications/{id}/cancel

- 功能：取消申请
- 权限：申请参与者（租借者或物主）
- 路径参数：`id` 申请 id
- 无请求体
- 成功响应 `data`：`null`
- 错误码：`RENTAL_APPLICATION_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_APPLICATION_STATUS_INVALID`
- 业务规则：仅 `status=0`（NEGOTIATING）的申请可取消；申请 `status` 置 3（CANCELLED）。申请阶段未占用时间段库存、未写入时间锁，无需释放。

### 9.2 订单接口

#### GET /api/orders

- 功能：我的订单列表（参与方视角，租借者或物主均可）
- 权限：用户
- 无查询参数（返回当前用户作为租借者或物主参与的全部订单，按 `createTime` 倒序，未分页）
- 成功响应 `data`：`List<RentalOrderResponse>`，元素结构见 9.2 GET /api/orders/{id}
- 错误码：`UNAUTHORIZED`

#### GET /api/orders/{id}

- 功能：订单详情
- 权限：订单参与者（租借者或物主）
- 路径参数：`id` 订单 id
- 成功响应 `data`：`RentalOrderResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 订单 id |
| orderNo | string | 订单号 |
| applicationId | long | 关联申请 id |
| proposalId | long | 关联 proposal id |
| itemId | long | 物品 id |
| itemSnapshotId | long | 物品快照 id |
| ownerId | long | 物主 id |
| renterId | long | 租借者 id |
| quantity | int | 租借数量 |
| deliveryType | int | 交付方式（DeliveryTypeEnum） |
| rentStartTime | LocalDateTime | 租借开始时间 |
| rentEndTime | LocalDateTime | 租借结束时间 |
| dailyPrice | BigDecimal | 日租金（来自快照） |
| rentAmount | BigDecimal | 租金总额 |
| depositAmount | BigDecimal | 押金金额 |
| paidRentAmount | BigDecimal | 已支付租金 |
| frozenDepositAmount | BigDecimal | 已冻结押金 |
| status | int | 订单状态（OrderStatusEnum） |
| shipCompany | string | 发货快递公司 |
| shipTrackingNo | string | 发货快递单号 |
| returnCompany | string | 归还快递公司 |
| returnTrackingNo | string | 归还快递单号 |
| receivedTime | LocalDateTime | 收货时间 |
| returnedTime | LocalDateTime | 归还时间 |
| completedTime | LocalDateTime | 完成时间 |
| overdueMinutes | int | 逾期分钟数 |
| overdueFeeAmount | BigDecimal | 逾期费用 |
| overdueSettled | int | 逾期费用是否已结算（0/1） |
| cancelReason | string | 取消原因 |
| createTime | LocalDateTime | 创建时间 |

- 错误码：`RENTAL_ORDER_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`

#### POST /api/orders/{id}/cancel

- 功能：取消订单
- 权限：订单参与者（租借者或物主）
- 路径参数：`id` 订单 id
- 请求体（`CancelOrderRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| cancelReason | string | 否 | max=255 | 取消原因 |

- 成功响应 `data`：`null`
- 错误码：`VALIDATION_ERROR`、`RENTAL_ORDER_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_ORDER_STATUS_INVALID`
- 业务规则：仅 `PENDING_PAYMENT`、`PAID_PENDING_DELIVERY` 状态可取消；订单 `status` 置 6（CANCELLED）；释放时间段库存与时间锁（`rental_time_lock`）。已支付租金或已冻结押金不为 0 时调用 wallet-service 取消支付。待付款订单也会被 RabbitMQ TTL + DLQ 或兜底定时任务自动取消。

#### POST /api/orders/{id}/ship

- 功能：快递发货
- 权限：出租者
- 路径参数：`id` 订单 id
- 请求体（`ShipOrderRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| shipCompany | string | 是 | 非空 | 快递公司 |
| shipTrackingNo | string | 是 | 非空 | 快递单号 |

- 成功响应 `data`：`null`
- 错误码：`VALIDATION_ERROR`、`RENTAL_ORDER_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_ORDER_STATUS_INVALID`
- 业务规则：仅 `PAID_PENDING_DELIVERY` 状态、且 `deliveryType=1`（EXPRESS）可发货；订单 `status` 置 2（SHIPPED）；写入 `shipCompany`、`shipTrackingNo`。发货通知 MQ 事件为阶段 6 实现。

#### POST /api/orders/{id}/receive

- 功能：确认收到
- 权限：租借者
- 路径参数：`id` 订单 id
- 无请求体
- 成功响应 `data`：`null`
- 错误码：`RENTAL_ORDER_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_ORDER_STATUS_INVALID`
- 业务规则：按交付方式分支确认收到——`deliveryType=1`（EXPRESS）仅 `SHIPPED` 状态可确认；`deliveryType=0`（MEETUP）从 `PAID_PENDING_DELIVERY` 状态直接确认收到；订单 `status` 置 3（RENTING）；写入 `receivedTime`。租借开始 MQ 通知为阶段 6 实现。

#### POST /api/orders/{id}/return

- 功能：提交归还
- 权限：租借者
- 路径参数：`id` 订单 id
- 请求体（`ReturnOrderRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| returnCompany | string | 是 | 非空，max=255 | 归还快递公司 |
| returnTrackingNo | string | 是 | 非空，max=255 | 归还快递单号 |

- 成功响应 `data`：`null`
- 错误码：`VALIDATION_ERROR`、`RENTAL_ORDER_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_ORDER_STATUS_INVALID`
- 业务规则：仅 `RENTING` 状态可提交归还；订单 `status` 置 4（PENDING_RETURN_CONFIRM）；写入 `returnCompany`、`returnTrackingNo`。`overdueMinutes`/`overdueFeeAmount` 的逾期计算为阶段 6 实现，当前阶段不写入。

#### POST /api/orders/{id}/complete

- 功能：确认收回并完成
- 权限：出租者
- 路径参数：`id` 订单 id
- 无请求体
- 成功响应 `data`：`null`
- 错误码：`RENTAL_ORDER_NOT_FOUND`、`RENTAL_APPLICATION_PERMISSION_DENIED`、`RENTAL_ORDER_STATUS_INVALID`
- 业务规则：仅 `PENDING_RETURN_CONFIRM` 状态可完成；订单 `status` 置 5（COMPLETED）；写入 `completedTime`；释放时间段库存与时间锁（`rental_time_lock`）；调用钱包结算租金、押金和逾期费用；完成后向双方发送评价提醒系统通知。

## 10. 阶段 6：预支付、押金冻结、结算、逾期费用

### 10.1 租借者支付租金

POST /api/orders/{id}/pay

请求：
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| amount | BigDecimal | 是 | 支付金额，>= 0.01 |

响应：RentalOrderResponse（含更新后的 paidRentAmount、status）

### 10.2 租借者冻结押金

POST /api/orders/{id}/freeze-deposit

请求：
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| amount | BigDecimal | 是 | 冻结金额，>= 0.01 |

响应：RentalOrderResponse（含更新后的 frozenDepositAmount、status）

### 10.3 订单状态自动推进

当 paidRentAmount >= rentAmount 且 frozenDepositAmount >= depositAmount 时，订单自动从 PENDING_PAYMENT(0) 推进到 PAID_PENDING_DELIVERY(1)。

### 10.4 钱包流水类型

| type | 含义 | direction |
|---:|---|---|
| 1 | 模拟充值 | 1=收入 |
| 2 | 支付租金 | 2=支出 |
| 3 | 冻结押金 | 3=冻结 |
| 4 | 释放押金 | 4=解冻 |
| 5 | 租金收入 | 1=收入 |
| 6 | 取消退款 | 1=收入 |
| 7 | 逾期费用支出 | 2=支出 |
| 8 | 逾期费用收入 | 1=收入 |
| 9 | 押金扣除 | 2=支出 |

### 10.5 押金状态

| status | 含义 |
|---:|---|
| 0 | 已冻结 |
| 1 | 已释放 |
| 2 | 已扣除 |
| 3 | 已取消 |
| 4 | 部分扣除后已释放 |

### 10.6 结算状态

| status | 含义 |
|---:|---|
| 0 | 未结算 |
| 1 | 部分结算 |
| 2 | 已结算 |

### 10.7 逾期费用计算规则

- 逾期时长 = completedTime - rentEndTime
- 不足 24 小时：1.5 × dailyPrice × (逾期分钟数 / 1440)
- 超过 24 小时：前 24 小时 = 1.5 × dailyPrice，之后每天 = 2 × dailyPrice
- 逾期费用优先从冻结押金扣除，押金不足时扣钱包余额（允许为负）
- 逾期费用转入出租者钱包

### 10.8 相关错误码

| 错误码 | 说明 |
|---|---|
| 40401 | 钱包余额不足 |
| 40404 | 押金已冻结 |
| 40405 | 押金未冻结 |
| 40406 | 押金已释放 |
| 40407 | 押金冻结失败 |
| 40408 | 押金释放失败 |
| 40409 | 订单结算记录已存在 |
| 40410 | 订单结算记录不存在 |
| 40411 | 逾期费用计算失败 |
| 40412 | 钱包账户更新冲突，请重试；账户更新未命中时中止后续流水、押金与结算写入，回滚本地事务 |

## 11. 阶段 7 接口详细字段

本章节补充阶段 7 即时通讯、评价与异议接口的请求与响应字段。字段定义以 `backend/message-service/`、`backend/rental-service/`、`backend/admin-service/` 下 DTO 实际声明为准；统一响应外壳（`code` / `message` / `data`）参见第 2 节，未特殊说明的成功响应 HTTP 状态均为 200。

阶段 7 状态枚举：

- `MessageType`：`1` 文本、`2` 图片、`3` 卡片、`4` 系统通知（SYSTEM_NOTIFICATION）。
- `CardType`：`1` 申请、`2` 协商、`3` 订单、`4` 评价提醒、`5` 异议。
- `Review.status`：`0` 正常。
- `Dispute.status`：`0` 待处理（PENDING）、`1` 已裁定（RESOLVED）。

对外端点请求头：`Authorization: Bearer <token>`、`Content-Type: application/json`（除说明外）。`X-User-Id` 由 Gateway 从 JWT 提取后注入下游业务服务，前端无需显式传递。WebSocket 连接通过 `ws://gateway/ws/chat?token={jwt}` 建立，Gateway 已为 `/ws/**` 路由放行 JWT 校验。

### 11.1 消息接口

#### GET /api/messages/conversations

- 功能：当前用户会话列表
- 权限：用户
- 请求头：`X-User-Id`（Gateway 注入）
- 成功响应 `data`：`List<ConversationResponse>`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 会话 id |
| itemId | long | 关联物品 id；系统通知会话为 0 |
| userAId | long | 会话方 A；系统通知会话为 0（表示系统） |
| userBId | long | 会话方 B |
| lastMessageContent | string | 最近一条消息预览（图片消息为 `[图片]`） |
| lastMessageTime | LocalDateTime | 最近一条消息时间 |
| unreadCount | int | 当前用户在该会话的未读数 |
| itemTitle | string | 关联物品标题；系统通知会话为 null。Feign 失败回退 `物品 #id` |
| itemFirstImageUrl | string | 关联物品首图 URL；系统通知会话为 null |
| peerUserId | long | 对方用户 id（系统通知会话为 0） |
| peerUsername | string | 对方用户名（系统通知会话为"系统通知"）。Feign 失败回退 `用户 #id` |
| peerAvatarUrl | string | 对方头像 URL |

- 错误码：`UNAUTHORIZED`
- 业务规则：会话按 `lastMessageTime` 倒序返回；系统通知会话（`itemId=0`、`userAId=0`）也会出现在列表中。`itemTitle`/`itemFirstImageUrl` 由 message-service 通过 Feign 调用 item-service `GET /internal/items/{id}/info` 获取；`peerUsername`/`peerAvatarUrl` 由 message-service 调用 auth-service `GET /internal/users/{id}/public` 获取。远程调用失败时不中断列表，回退到默认展示文案。

#### POST /api/messages/conversations/open

- 功能：按物品和对方用户创建或复用一个会话，用于物品详情页“联系物主”
- 权限：用户
- 请求头：`X-User-Id`（Gateway 注入）
- 请求体：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| targetUserId | long | 是 | 对方用户 id |
| itemId | long | 是 | 关联物品 id |

- 成功响应 `data`：`ConversationResponse`
- 错误码：`VALIDATION_ERROR`、`BAD_REQUEST`、`UNAUTHORIZED`
- 业务规则：同一 `itemId + 双方用户` 只保留一个会话；服务端按较小用户 id 写入 `userAId`、较大用户 id 写入 `userBId`，确保唯一键稳定。

#### GET /api/messages/conversations/{id}/messages

- 功能：会话消息历史（分页，按 `createTime` 倒序）
- 权限：会话参与方
- 路径参数：`id` 会话 id
- 查询参数：

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| page | int | 否 | 1 | 页码，从 1 开始 |
| size | int | 否 | 20 | 每页条数 |

- 成功响应 `data`：`List<MessageResponse>`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 消息 id |
| conversationId | long | 会话 id |
| senderId | long | 发送者 id；系统通知为 0 |
| receiverId | long | 接收者 id |
| messageType | int | 消息类型（MessageType） |
| cardType | int | 卡片类型（CardType，仅 messageType=3 时有值） |
| content | string | 文本内容 |
| imageUrls | string | 图片地址，JSON 数组字符串 |
| relatedApplicationId | long | 关联申请 id（卡片消息） |
| relatedOrderId | long | 关联订单 id（卡片消息） |
| isRead | int | 是否已读（0/1） |
| createTime | LocalDateTime | 创建时间 |

- 错误码：`UNAUTHORIZED`
- 业务规则：仅会话参与方可查询；消息按 `createTime` 倒序分页返回。

#### POST /api/messages/conversations/{id}/messages

- 功能：发送消息
- 权限：会话参与方
- 路径参数：`id` 会话 id
- 请求体（`SendMessageRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| messageType | int | 是 | 1/2/3 | 消息类型：1 文本、2 图片、3 卡片 |
| content | string | 否 | - | 文本内容（messageType=1 时填写） |
| imageUrls | string | 否 | JSON 数组字符串 | 图片地址（messageType=2 时填写） |
| cardType | int | 否 | 1/2/3/4/5 | 卡片类型（messageType=3 时填写） |
| relatedApplicationId | long | 否 | - | 关联申请 id（卡片消息） |
| relatedOrderId | long | 否 | - | 关联订单 id（卡片消息） |

- 成功响应 `data`：`MessageResponse`（结构见 11.1 GET /api/messages/conversations/{id}/messages）
- 错误码：`VALIDATION_ERROR`、`UNAUTHORIZED`
- 业务规则：接收方未读数 +1 并刷新会话 `lastMessageContent`/`lastMessageTime`；若接收方 WebSocket 在线，message-service 通过 `ChatWebSocketHandler.sendToUser` 直接推送 JSON 形式的 `MessageResponse`，推送失败不阻塞消息落库。

#### PUT /api/messages/conversations/{id}/read

- 功能：标记会话已读
- 权限：会话参与方
- 路径参数：`id` 会话 id
- 成功响应 `data`：`null`
- 错误码：`UNAUTHORIZED`
- 业务规则：清零当前用户在该会话的未读数，并将该会话中接收者为当前用户且 `is_read=0` 的消息批量置为 `is_read=1` 并写入 `read_time`。

#### GET /api/messages/unread-count

- 功能：当前用户未读总数
- 权限：用户
- 请求头：`X-User-Id`（Gateway 注入）
- 成功响应 `data`：`UnreadCountResponse`

| 字段 | 类型 | 说明 |
|---|---|---|
| totalUnread | int | 当前用户在所有会话的未读数之和 |

- 错误码：`UNAUTHORIZED`

#### POST /api/messages/images

- 功能：上传聊天图片，返回可嵌入图片消息的 URL
- 权限：用户
- 请求头：`X-User-Id`（Gateway 注入）
- 请求体：`multipart/form-data`，字段 `file` 为图片文件
- 成功响应 `data`：

| 字段 | 类型 | 说明 |
|---|---|---|
| url | string | 上传后的图片访问 URL，形如 `/files/messages/{filename}` |

- 错误码：`UNAUTHORIZED`、`BAD_REQUEST`（文件为空）
- 业务规则：调用公共 `ImageUploadService.storeImage(file, "messages")`，文件落盘到 `uploads/messages/`，自动压缩与重命名；前端拿到 `url` 后通过 `POST /api/messages/conversations/{id}/messages`（`messageType=2`）发送图片消息。

### 11.2 WebSocket 即时通讯

#### WS /ws/chat

- 功能：建立 WebSocket 长连接，接收实时消息与系统通知推送
- 连接地址：`ws://127.0.0.1:8080/ws/chat?token={jwt}`
- 鉴权：Gateway 已为 `/ws/**` 路由放行 JWT 校验；message-service `ChatWebSocketHandler` 在连接建立时解析 `token` 查询参数获取 `userId`，解析失败则以 `CloseStatus.POLICY_VIOLATION` 关闭连接。
- 心跳：客户端发送文本 `ping`，服务端回复 `pong` 并刷新在线状态 TTL。
- 在线状态：Redis key `sr:message:online:{userId}`，TTL 300 秒；心跳每 60 秒刷新一次；连接关闭时删除 key。
- 未读缓存：Redis key `sr:message:unread:{userId}`；发送消息时递增，标记已读后按数据库会话未读聚合刷新或删除。
- 推送内容：
  - 用户发送消息（11.1 POST）时，若接收方在线，直接推送 `MessageResponse` 的 JSON。
  - 系统通知（11.4 内部接口）触发时，若接收方在线，直接推送通知 `content` 文本。
- 实现说明：使用 Spring `TextWebSocketHandler` + `WebSocketConfigurer`（非 JSR-356）；本机 `sessionMap` 维护 `userId -> WebSocketSession`，跨实例在线状态依赖 Redis key。

### 11.5 阶段 8 限流与降级响应

Gateway 限流响应：

```json
{"code":42900,"message":"请求过于频繁","data":null}
```

租借服务下游访问降级响应：

```json
{"code":42901,"message":"服务繁忙","data":null}
```

### 11.3 评价接口

#### POST /api/orders/{orderId}/reviews

- 功能：创建评价
- 权限：订单参与方（租借者或物主）
- 路径参数：`orderId` 订单 id
- 请求体（`ReviewCreateRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| rating | int | 是 | 1-5 | 评分 |
| content | string | 否 | max=500 | 评价内容 |
| imageUrls | string | 否 | JSON 数组字符串 | 评价图片 |

- 成功响应 `data`：`ReviewResponse`（结构见 11.3 GET /api/orders/{orderId}/reviews）
- 错误码：`VALIDATION_ERROR`、`REVIEW_ALREADY_EXISTS`、`REVIEW_PERMISSION_DENIED`、`REVIEW_ORDER_NOT_COMPLETED`
- 业务规则：仅 `COMPLETED` 订单可评价；每方每订单仅可评价一次（数据库 `uk_order_reviewer` 唯一键）；`reviewerId` 为当前用户，`revieweeId` 为订单另一方。

#### GET /api/orders/{orderId}/reviews

- 功能：查询订单评价列表
- 权限：订单参与方
- 路径参数：`orderId` 订单 id
- 成功响应 `data`：`List<ReviewResponse>`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 评价 id |
| orderId | long | 订单 id |
| itemId | long | 物品 id |
| reviewerId | long | 评价者 id |
| revieweeId | long | 被评价者 id |
| rating | int | 评分（1-5） |
| content | string | 评价内容 |
| imageUrls | string | 评价图片，JSON 数组字符串 |
| status | int | 评价状态（0 正常） |
| createTime | LocalDateTime | 创建时间 |

- 错误码：`REVIEW_PERMISSION_DENIED`

### 11.4 异议接口（用户侧）

#### POST /api/orders/{orderId}/disputes

- 功能：创建异议
- 权限：订单参与方（租借者或物主）
- 路径参数：`orderId` 订单 id
- 请求体（`DisputeCreateRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| reason | string | 是 | max=128 | 异议原因 |
| description | string | 否 | max=500 | 异议描述 |
| expectedDepositDeduction | BigDecimal | 否 | >=0 | 期望押金扣除金额 |
| imageUrls | string | 否 | JSON 数组字符串 | 异议图片 |

- 成功响应 `data`：`DisputeResponse`（结构见 11.4 GET /api/orders/{orderId}/disputes）
- 错误码：`VALIDATION_ERROR`、`DISPUTE_ALREADY_EXISTS`、`DISPUTE_PERMISSION_DENIED`
- 业务规则：仅订单的 renter/owner 可提交异议；每订单仅可提交一次；异议不改变订单主状态；`applicantId` 为当前用户。

#### GET /api/orders/{orderId}/disputes

- 功能：查询订单异议列表
- 权限：订单参与方
- 路径参数：`orderId` 订单 id
- 成功响应 `data`：`List<DisputeResponse>`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 异议 id |
| orderId | long | 订单 id |
| applicantId | long | 申请人 id |
| reason | string | 异议原因 |
| description | string | 异议描述 |
| expectedDepositDeduction | BigDecimal | 期望押金扣除金额 |
| imageUrls | string | 异议图片，JSON 数组字符串 |
| status | int | 异议状态（0 待处理、1 处理中、2 已裁定） |
| adminId | long | 裁定管理员 id（未裁定时为 null） |
| adminRemark | string | 管理员处理意见（未裁定时为 null） |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

- 错误码：`DISPUTE_PERMISSION_DENIED`

### 11.5 异议接口（管理端）

管理端异议接口由 admin-service 通过 Feign 转发至 rental-service 内部接口实现。前端仅访问 Gateway 的 `/api/admin/disputes/**` 路径；`/internal/rental/disputes/**` 不暴露给外部，Gateway 拦截 `/internal/**` 前缀。

#### GET /api/admin/disputes

- 功能：管理员查询全部异议列表
- 权限：管理员
- 查询参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | int | 否 | 异议状态过滤（0 待处理、1 处理中、2 已裁定）；不传则返回全部 |

- 成功响应 `data`：`List<DisputeResponse>`（结构见 11.4 GET /api/orders/{orderId}/disputes）
- 错误码：`UNAUTHORIZED`、`FORBIDDEN`
- Feign 转发：admin-service `RentalAdminClient.listDisputes(status)` → rental-service `GET /internal/rental/disputes?status=`

#### PUT /api/admin/disputes/{id}/resolve

- 功能：管理员裁定异议
- 权限：管理员
- 路径参数：`id` 异议 id
- 请求体（`ResolveDisputeRequest`）：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|---|---|---|---|---|
| adminRemark | string | 否 | - | 管理员处理意见 |

- 成功响应 `data`：`null`
- 错误码：`VALIDATION_ERROR`、`DISPUTE_NOT_FOUND`、`DISPUTE_STATUS_INVALID`、`FORBIDDEN`
- 业务规则：`adminId` 由 Gateway 的管理员登录态写入 `X-User-Id`，前端不得传入或覆盖；仅 `PENDING(0)`/`PROCESSING(1)` 状态异议可裁定；裁定后写入 `adminId`/`adminRemark`，状态变为 `RESOLVED(2)`，并刷新 `updateTime`。
- Feign 转发：admin-service 从 `X-User-Id` 补入 `adminId` 后调用 `RentalAdminClient.resolveDispute(id, request)` → rental-service `PUT /internal/rental/disputes/{id}/resolve`（内部请求体 `ResolveDisputeFeignRequest` 包含 `adminId`、`adminRemark`）。

### 11.6 系统通知内部接口

#### POST /internal/messages/system

- 功能：写入系统通知消息（内部 Feign，不暴露 Gateway）
- 权限：服务间调用，需携带 `X-Internal-Token`
- 请求体（`SystemNotificationRequest`）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| receiverId | long | 是 | 接收者用户 id |
| content | string | 是 | 通知内容 |
| messageType | int | 是 | 消息类型（一般传 4 系统通知） |

- 成功响应 `data`：`null`
- 业务规则：复用或新建系统会话（`itemId=0`、`userAId=0`、`userBId=receiverId`），递增接收者未读数，更新会话 `lastMessageContent`/`lastMessageTime`，落库 `Message`（`senderId=0`）；若接收方 WebSocket 在线则推送 `content` 文本。
- 调用方：admin-service 物品审核结果通知、rental-service 订单完成后给双方发送"订单已完成，请评价对方"提醒（`messageType=4`）。

#### POST /internal/messages/cards

- 功能：创建租借申请/订单卡片消息（内部 Feign，不暴露 Gateway）
- 权限：服务间调用，需携带 `X-Internal-Token`
- 请求体（`CardMessageRequest`）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| senderId | long | 是 | 发送者用户 id |
| receiverId | long | 是 | 接收者用户 id |
| itemId | long | 是 | 关联物品 id，用于复用会话 |
| content | string | 是 | 卡片展示文案 |
| cardType | int | 是 | 卡片类型：1=租借申请 |
| relatedApplicationId | long | 否 | 关联租借申请 id |
| relatedOrderId | long | 否 | 关联订单 id |

- 成功响应 `data`：`null`
- 业务规则：按 `itemId + sender/receiver` 复用或新建会话，写入 `Message`（`messageType=3`、`cardType` 取请求值），递增接收者未读数并刷新会话 `lastMessageContent`/`lastMessageTime`；接收方 WebSocket 在线时推送 `MessageResponse`。
- 调用方：rental-service `createApplication` 创建租借申请后 fire-and-forget 调用，向物主发送"收到新的租借申请：{itemTitle}"卡片（`cardType=1`）；消息发送失败仅 warn 日志，不回滚申请创建。

### 11.7 相关错误码

| 错误码 | 常量名 | 说明 |
|---|---|---|
| 40310 | REVIEW_ALREADY_EXISTS | 该订单已评价过 |
| 40311 | REVIEW_NOT_FOUND | 评价不存在 |
| 40312 | REVIEW_PERMISSION_DENIED | 无权操作此评价 |
| 40313 | REVIEW_ORDER_NOT_COMPLETED | 订单未完成，不可评价 |
| 40314 | DISPUTE_ALREADY_EXISTS | 该订单已提交异议 |
| 40315 | DISPUTE_NOT_FOUND | 异议不存在 |
| 40316 | DISPUTE_PERMISSION_DENIED | 无权操作此异议 |
| 40317 | DISPUTE_STATUS_INVALID | 异议状态不允许此操作 |

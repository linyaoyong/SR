// 状态枚举文本映射。数值与 docs/03-api-contract.md、docs/04-database-design.md 对齐。

// 物品审核状态：docs/03-api-contract.md 第 8 节物品状态枚举。
export const itemAuditText: Record<number, string> = {
  0: '待审核',
  1: '审核通过',
  2: '要求整改',
};

// 物品状态：1 已上架、2 已下架、3 强制下架。
export const itemStatusText: Record<number, string> = {
  1: '已上架',
  2: '已下架',
  3: '强制下架',
};

// 申请状态 ApplicationStatusEnum：docs/03-api-contract.md 第 9 节。
export const applicationStatusText: Record<number, string> = {
  0: '协商中',
  1: '双方确认',
  2: '已转订单',
  3: '已取消',
};

// 订单状态 OrderStatusEnum：docs/03-api-contract.md 第 9 节。
export const orderStatusText: Record<number, string> = {
  0: '待付款',
  1: '已付款待交付',
  2: '已发货',
  3: '租借中',
  4: '待归还确认',
  5: '已完成',
  6: '已取消',
  7: '异议中',
  8: '已关闭',
};

// 交付方式 DeliveryTypeEnum：docs/03-api-contract.md 第 9 节。
export const deliveryTypeText: Record<number, string> = {
  0: '面交',
  1: '快递',
};

// 消息类型 MessageType：docs/03-api-contract.md 第 11 节。
export const messageTypeText: Record<number, string> = {
  1: '文本',
  2: '图片',
  3: '卡片',
  4: '系统通知',
};

// 卡片类型 CardType：docs/03-api-contract.md 第 11 节。
export const cardTypeText: Record<number, string> = {
  1: '申请',
  2: '协商',
  3: '订单',
  4: '评价提醒',
  5: '异议',
};

// 钱包流水类型：docs/03-api-contract.md 第 10.4 节。
export const walletTransactionTypeText: Record<number, string> = {
  1: '模拟充值',
  2: '支付租金',
  3: '冻结押金',
  4: '释放押金',
  5: '租金收入',
  6: '取消退款',
  7: '逾期费用支出',
  8: '逾期费用收入',
  9: '押金扣除',
};

// 钱包流水方向：docs/03-api-contract.md 第 10.4 节 direction 列。
export const walletTransactionDirectionText: Record<number, string> = {
  1: '收入',
  2: '支出',
  3: '冻结',
  4: '解冻',
};

// 押金状态：docs/03-api-contract.md 第 10.5 节。
export const depositStatusText: Record<number, string> = {
  0: '已冻结',
  1: '已释放',
  2: '已扣除',
  3: '已取消',
  4: '部分扣除后已释放',
};

// 结算状态：docs/03-api-contract.md 第 10.6 节。
export const settlementStatusText: Record<number, string> = {
  0: '未结算',
  1: '部分结算',
  2: '已结算',
};

// 用户资料审核状态：与物品审核状态数值一致（0 待审核、1 审核通过、2 要求整改）。
export const userAuditStatusText: Record<number, string> = {
  0: '待审核',
  1: '审核通过',
  2: '要求整改',
};

// 用户账户状态：docs/03-api-contract.md 第 8.3 节。
export const userStatusText: Record<number, string> = {
  0: '正常',
  1: '封禁',
};

// 用户角色：docs/03-api-contract.md 第 8.3 节。
export const userRoleText: Record<number, string> = {
  0: '用户',
  1: '管理员',
};

// 评价状态：docs/03-api-contract.md 第 11 节（0 正常）。
export const reviewStatusText: Record<number, string> = {
  0: '正常',
};

// 异议状态：docs/03-api-contract.md 第 11.4 节 DisputeResponse.status。
export const disputeStatusText: Record<number, string> = {
  0: '待处理',
  1: '处理中',
  2: '已裁定',
};

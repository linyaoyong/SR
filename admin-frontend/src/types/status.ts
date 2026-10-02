// 审核状态文本映射，对应后端 audit_status 字段
export const auditStatusText: Record<number, string> = {
  0: '待审核',
  1: '审核通过',
  2: '要求整改',
};

// 异议状态文本映射，对应后端 dispute.status 字段
export const disputeStatusText: Record<number, string> = {
  0: '待处理',
  1: '处理中',
  2: '已裁定',
};

// 操作日志操作类型中文映射，对应后端 admin_operation_logs.operation_type
export const operationTypeText: Record<string, string> = {
  AUDIT_USER: '用户审核',
  BAN_USER: '封禁用户',
  UNBAN_USER: '解封用户',
  AUDIT_ITEM: '物品审核',
  FORCE_OFF_SHELF: '强制下架',
  RESOLVE_DISPUTE: '异议裁定',
  HANDLE_DISPUTE: '处理异议',
};

// 操作日志目标类型中文映射，对应后端 admin_operation_logs.target_type
export const targetTypeText: Record<string, string> = {
  USER: '用户',
  ITEM: '物品',
  DISPUTE: '异议',
  ORDER: '订单',
  REVIEW: '评价',
};

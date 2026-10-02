// 后端统一响应结构
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 登录返回，role 字段为 'ADMIN' | 'USER'
export interface LoginResponse {
  userId: number;
  username: string;
  role: string;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// 后台仪表盘聚合数据，字段顺序与后端 AdminDashboardResponse 一致
export interface AdminDashboardResponse {
  itemAuditCount: number;
  userAuditCount: number;
  totalUsers: number;
  totalItems: number;
  bannedUsers: number;
}

// 用户审核列表项（后端 UserAuditItemResponse，字段是 userId 不是 id）
export interface UserAuditItem {
  userId: number;
  username: string;
  fieldName: string;
  auditStatus: number;
  reason?: string;
  // 新增字段（后端 commit f74d62e 起列表响应携带完整 profile）
  avatarUrl?: string;
  description?: string;
  creditScore?: number;
  status?: number;
  showRentalHistory?: number;
  usernameAuditStatus?: number;
  avatarAuditStatus?: number;
  descriptionAuditStatus?: number;
  /** 该用户所有待审核字段名列表（后端 fieldNames） */
  fieldNames?: string[];
  /** 与 fieldNames 一一对应的审核状态列表 */
  auditStatuses?: number[];
}

// 物品审核列表项（后端 ItemAuditResponse，含 status + auditStatus 双字段）
export interface ItemAuditItem {
  id: number;
  ownerId: number;
  title: string;
  status: number;
  auditStatus: number;
  auditReason?: string;
  auditTime?: string;
  auditAdminId?: number;
  // 新增字段（后端 commit f74d62e 起列表响应携带完整物品资料）
  description?: string;
  categoryId?: number;
  categoryName?: string;
  tags?: string;
  priceType?: number;
  dailyPrice?: number;
  depositAmount?: number;
  quantity?: number;
  imageUrls?: string[];
}

// 物品审核操作返回（后端 ItemAuditActionResponse，字段是 itemId 不是 id）
export interface ItemAuditActionResponse {
  itemId: number;
  ownerId: number;
  auditStatus: number;
  auditReason?: string;
  auditTime?: string;
  auditAdminId?: number;
}

// 异议（后端 DisputeResponse）
export interface Dispute {
  id: number;
  orderId: number;
  applicantId: number;
  reason: string;
  description?: string;
  expectedDepositDeduction?: number;
  imageUrls?: string;
  status: number;
  adminId?: number;
  adminRemark?: string;
  createTime: string;
  updateTime?: string;
}

// 操作日志（后端 AdminLogResponse）
export interface AdminLog {
  id: number;
  adminId: number;
  operationType: string;
  targetType: string;
  targetId: number;
  remark?: string;
  ip?: string;
  createTime: string;
}

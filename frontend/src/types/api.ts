// 统一响应外壳与分页结构。字段命名严格对齐 docs/03-api-contract.md 第 2 节。
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
  pages: number;
}

// 认证响应字段来源 docs/03-api-contract.md 第 5 节 auth-service 示例。
export interface LoginResponse {
  userId: number;
  username: string;
  role: 'USER' | 'ADMIN';
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// 字段来源 docs/03-api-contract.md 第 8.3 节 UserMeResponse。
export interface UserMeResponse {
  id: number;
  username: string;
  avatarUrl?: string;
  description?: string;
  creditScore: number;
  role: number; // 0 用户、1 管理员
  status: number; // 0 正常、1 封禁
  usernameAuditStatus: number;
  avatarAuditStatus: number;
  descriptionAuditStatus: number;
  showRentalHistory: number;
  lastLoginTime?: string;
}

// 公开主页字段。docs/03-api-contract.md 未单独列字段，依据 GET /api/users/{id} 公开主页信息约束。
export interface UserPublicProfile {
  id: number;
  username: string;
  avatarUrl?: string;
  description?: string;
  creditScore: number;
  showRentalHistory: number;
}

// 字段来源 docs/03-api-contract.md 第 8.2 节 CategoryResponse。
export interface Category {
  id: number;
  name: string;
  sortOrder: number;
  status: number; // 0 启用、1 禁用（与 backend/scripts/init.sql 一致）
}

// 字段来源 docs/03-api-contract.md 第 8.1 节 ItemImageResponse。
export interface ItemImage {
  id: number;
  url: string;
  sortOrder: number;
}

// 字段来源 docs/03-api-contract.md 第 8.1 节 ItemListResponse。
export interface ItemListItem {
  id: number;
  title: string;
  categoryId: number;
  dailyPrice: number;
  minRentDays: number;
  depositAmount: number;
  status: number; // 物品状态：1 已上架、2 已下架、3 强制下架
  auditStatus: number; // 审核状态：0 待审核、1 审核通过、2 要求整改
  firstImageUrl?: string;
  createTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 8.1 节 ItemDetailResponse。
export interface ItemDetail {
  id: number;
  ownerId: number;
  title: string;
  description: string;
  categoryId: number;
  tags?: string;
  quantity: number;
  rentedCount: number;
  supportDelivery: number;
  deliveryCity?: string;
  supportMeetup: number;
  meetupLocation?: string;
  priceType: number;
  dailyPrice: number;
  minRentDays: number;
  freeRent: number;
  depositEnabled: number;
  depositAmount: number;
  creditDepositEnabled: number;
  minCreditScore: number;
  freeDepositScore: number;
  reducedDepositScore: number;
  reducedDepositAmount: number;
  status: number;
  auditStatus: number;
  auditReason?: string;
  images: ItemImage[];
  createTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 8.2 节 FavoriteResponse。
export interface FavoriteItem {
  id: number;
  itemId: number;
  itemTitle: string;
  dailyPrice: number;
  firstImageUrl?: string;
  createTime?: string;
}

// 字段来源 docs/04-database-design.md wallet_accounts；API 契约未单独列字段，依据数据库结构映射。
export interface WalletAccount {
  id: number;
  userId: number;
  balance: number;
  frozenAmount: number;
  status: number; // 0 正常、1 欠费
  createTime?: string;
  updateTime?: string;
}

// 字段来源 docs/04-database-design.md wallet_transactions；API 契约未单独列字段，依据数据库结构映射。
export interface WalletTransaction {
  id: number;
  transactionNo: string;
  userId: number;
  counterpartyUserId?: number;
  applicationId?: number;
  proposalId?: number;
  orderId?: number;
  type: number; // 1 模拟充值、2 支付租金、3 冻结押金、4 释放押金、5 租金收入、6 取消退款、7 逾期费用支出、8 逾期费用收入、9 押金扣除
  direction: number; // 1 收入、2 支出、3 冻结、4 解冻
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  frozenBefore: number;
  frozenAfter: number;
  remark?: string;
  createTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 9.1 节 RentalProposalResponse。
export interface RentalProposal {
  id: number;
  applicationId: number;
  versionNo: number;
  operatorId: number;
  quantity: number;
  deliveryType: number; // 0 面交、1 快递
  rentStartTime: string;
  rentEndTime: string;
  meetupTime?: string;
  meetupLocation?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  rentAmount: number;
  depositAmount: number;
  changedFields?: string;
  remark?: string;
  createTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 9.1 节 RentalApplicationResponse / RentalApplicationDetailResponse。
export interface RentalApplication {
  id: number;
  itemId: number;
  itemTitle?: string;
  renterId: number;
  ownerId: number;
  status: number; // ApplicationStatusEnum：0 协商中、1 双方确认、2 已转订单、3 已取消
  currentProposalId: number;
  ownerConfirmed: number;
  renterConfirmed: number;
  createTime?: string;
  currentProposal: RentalProposal | null;
}

// 字段来源 docs/03-api-contract.md 第 9.1 节 POST /api/rentals/applications 请求体。
export interface RentalApplicationCreateRequest {
  itemId: number;
  quantity: number;
  rentStartTime: string;
  rentEndTime: string;
  deliveryType: number;
  meetupTime?: string;
  meetupLocation?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  rentAmount?: number;
  depositAmount?: number;
  remark?: string;
}

// 字段来源 docs/03-api-contract.md 第 9.1 节 PUT /api/rentals/applications/{id}/proposal 请求体，所有字段可选。
export interface RentalProposalUpdateRequest {
  quantity?: number;
  rentStartTime?: string;
  rentEndTime?: string;
  meetupTime?: string;
  meetupLocation?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  rentAmount?: number;
  depositAmount?: number;
  remark?: string;
}

// 字段来源 docs/03-api-contract.md 第 9.2 节 RentalOrderResponse。
export interface RentalOrder {
  id: number;
  orderNo: string;
  applicationId: number;
  proposalId: number;
  itemId: number;
  itemSnapshotId: number;
  itemSnapshotTitle?: string;
  itemSnapshotDescription?: string;
  itemSnapshotCategoryName?: string;
  itemSnapshotImageUrls?: string;
  ownerId: number;
  renterId: number;
  quantity: number;
  deliveryType: number;
  rentStartTime: string;
  rentEndTime: string;
  dailyPrice: number;
  rentAmount: number;
  depositAmount: number;
  paidRentAmount: number;
  frozenDepositAmount: number;
  status: number; // OrderStatusEnum 0-8
  shipCompany?: string;
  shipTrackingNo?: string;
  returnCompany?: string;
  returnTrackingNo?: string;
  receivedTime?: string;
  returnedTime?: string;
  completedTime?: string;
  overdueMinutes: number;
  overdueFeeAmount: number;
  overdueSettled: number;
  cancelReason?: string;
  createTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 11.3 节 ReviewResponse。
export interface Review {
  id: number;
  orderId: number;
  itemId: number;
  reviewerId: number;
  reviewerName?: string;
  revieweeId: number;
  rating: number;
  content?: string;
  imageUrls?: string;
  status: number; // 0 正常
  createTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 11.4 节 DisputeResponse。
export interface Dispute {
  id: number;
  orderId: number;
  applicantId: number;
  reason: string;
  description?: string;
  expectedDepositDeduction?: number;
  imageUrls?: string;
  status: number; // 0 待处理、1 处理中、2 已裁定
  adminId?: number | null;
  adminRemark?: string | null;
  createTime?: string;
  updateTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 11.1 节 ConversationResponse。
export interface Conversation {
  id: number;
  itemId: number; // 系统通知会话为 0
  userAId: number; // 系统通知会话为 0
  userBId: number;
  lastMessageContent?: string;
  lastMessageTime?: string;
  unreadCount: number;
  itemTitle?: string;
  itemFirstImageUrl?: string;
  peerUserId?: number;
  peerUsername?: string;
  peerAvatarUrl?: string;
}

// 字段来源 docs/03-api-contract.md 第 11.1 节 MessageResponse。
export interface Message {
  id: number;
  conversationId: number;
  senderId: number; // 系统通知为 0
  receiverId: number;
  messageType: number; // 1 文本、2 图片、3 卡片、4 系统通知
  cardType?: number; // 1 申请、2 协商、3 订单、4 评价提醒、5 异议
  content?: string;
  imageUrls?: string; // JSON 数组字符串
  relatedApplicationId?: number;
  relatedOrderId?: number;
  isRead: number;
  createTime?: string;
}

// 字段来源 docs/03-api-contract.md 第 11.1 节 UnreadCountResponse。
export interface UnreadCount {
  totalUnread: number;
}

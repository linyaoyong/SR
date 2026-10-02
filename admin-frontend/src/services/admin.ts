import { http, requestData } from './http';
import type {
  AdminDashboardResponse,
  AdminLog,
  Dispute,
  ItemAuditActionResponse,
  ItemAuditItem,
  UserAuditItem,
} from '../types/api';

// 管理员业务服务，对接 /api/admin/* 系列接口。
// 约定：所有列表方法返回 T[]（数组）；写操作的 adminId 由网关 X-User-Id 头注入，前端不传。
export const adminService = {
  dashboard: () => requestData<AdminDashboardResponse>(http.get('/api/admin/dashboard')),

  userAudits: (auditStatus?: number) =>
    requestData<UserAuditItem[]>(
      http.get('/api/admin/users/audits', { params: { auditStatus } }),
    ),

  auditUser: (
    id: number,
    body: { auditStatus: number; auditReason?: string; fieldName?: string },
  ) => requestData<null>(http.post(`/api/admin/users/${id}/audit`, body)),

  banUser: (id: number, reason?: string) =>
    requestData<null>(http.post(`/api/admin/users/${id}/ban`, { reason })),

  unbanUser: (id: number) =>
    requestData<null>(http.post(`/api/admin/users/${id}/unban`)),

  itemAudits: (auditStatus?: number) =>
    requestData<ItemAuditItem[]>(
      http.get('/api/admin/items/audits', { params: { auditStatus } }),
    ),

  auditItem: (id: number, body: { auditStatus: number; auditReason?: string }) =>
    requestData<ItemAuditActionResponse>(http.post(`/api/admin/items/${id}/audit`, body)),

  forceOffShelf: (id: number) =>
    requestData<null>(http.post(`/api/admin/items/${id}/force-off-shelf`)),

  disputes: (status?: number) =>
    requestData<Dispute[]>(http.get('/api/admin/disputes', { params: { status } })),

  resolveDispute: (id: number, adminRemark?: string) =>
    requestData<null>(http.put(`/api/admin/disputes/${id}/resolve`, { adminRemark })),

  logs: (page = 1, size = 20) =>
    requestData<AdminLog[]>(http.get('/api/admin/logs', { params: { page, size } })),
};

import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';

// 仅 mock http 实例，保留真实的 requestData 解包逻辑，
// 这样测试既验证 URL/方法/payload，又验证解包行为。
vi.mock('./http', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./http')>();
  return {
    ...actual,
    http: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
    },
  };
});

import { http } from './http';
import { adminService } from './admin';
import type { AdminDashboardResponse, ItemAuditActionResponse } from '../types/api';

const mockHttp = http as unknown as { get: Mock; post: Mock; put: Mock };

// 构造后端统一响应：code=0 表示业务成功，data 为实际载荷
function ok<T>(data: T) {
  return Promise.resolve({ data: { code: 0, message: 'ok', data } });
}

describe('adminService', () => {
  beforeEach(() => {
    mockHttp.get.mockReset();
    mockHttp.post.mockReset();
    mockHttp.put.mockReset();
  });

  describe('dashboard', () => {
    it('GET /api/admin/dashboard 并返回 AdminDashboardResponse', async () => {
      const payload: AdminDashboardResponse = {
        itemAuditCount: 3,
        userAuditCount: 1,
        totalUsers: 100,
        totalItems: 50,
        bannedUsers: 2,
      };
      mockHttp.get.mockReturnValueOnce(ok(payload));

      const result = await adminService.dashboard();

      expect(mockHttp.get).toHaveBeenCalledWith('/api/admin/dashboard');
      expect(result).toEqual(payload);
    });
  });

  describe('userAudits', () => {
    it('无参时 GET /api/admin/users/audits，params.auditStatus 为 undefined（axios 会剔除）', async () => {
      mockHttp.get.mockReturnValueOnce(ok([]));
      await adminService.userAudits();
      expect(mockHttp.get).toHaveBeenCalledWith('/api/admin/users/audits', {
        params: { auditStatus: undefined },
      });
    });

    it('传 0 时 GET /api/admin/users/audits，params.auditStatus 为 0', async () => {
      mockHttp.get.mockReturnValueOnce(ok([]));
      await adminService.userAudits(0);
      expect(mockHttp.get).toHaveBeenCalledWith('/api/admin/users/audits', {
        params: { auditStatus: 0 },
      });
    });
  });

  describe('itemAudits', () => {
    it('GET /api/admin/items/audits?auditStatus=1', async () => {
      mockHttp.get.mockReturnValueOnce(ok([]));
      await adminService.itemAudits(1);
      expect(mockHttp.get).toHaveBeenCalledWith('/api/admin/items/audits', {
        params: { auditStatus: 1 },
      });
    });
  });

  describe('auditUser', () => {
    it('POST /api/admin/users/5/audit，body 含 auditStatus/auditReason/fieldName，返回 null', async () => {
      mockHttp.post.mockReturnValueOnce(ok(null));

      const result = await adminService.auditUser(5, {
        auditStatus: 2,
        auditReason: '整改原因',
        fieldName: 'username',
      });

      expect(mockHttp.post).toHaveBeenCalledWith('/api/admin/users/5/audit', {
        auditStatus: 2,
        auditReason: '整改原因',
        fieldName: 'username',
      });
      expect(result).toBeNull();
    });
  });

  describe('auditItem', () => {
    it('POST /api/admin/items/3/audit，body={auditStatus:1}，返回 ItemAuditActionResponse', async () => {
      const payload: ItemAuditActionResponse = {
        itemId: 3,
        ownerId: 7,
        auditStatus: 1,
        auditReason: '通过',
        auditTime: '2026-06-28 10:00:00',
        auditAdminId: 1,
      };
      mockHttp.post.mockReturnValueOnce(ok(payload));

      const result = await adminService.auditItem(3, { auditStatus: 1 });

      expect(mockHttp.post).toHaveBeenCalledWith('/api/admin/items/3/audit', {
        auditStatus: 1,
      });
      expect(result).toEqual(payload);
    });
  });

  describe('forceOffShelf', () => {
    it('POST /api/admin/items/3/force-off-shelf，无 body', async () => {
      mockHttp.post.mockReturnValueOnce(ok(null));
      await adminService.forceOffShelf(3);
      expect(mockHttp.post).toHaveBeenCalledWith('/api/admin/items/3/force-off-shelf');
    });
  });

  describe('disputes', () => {
    it('GET /api/admin/disputes?status=0', async () => {
      mockHttp.get.mockReturnValueOnce(ok([]));
      await adminService.disputes(0);
      expect(mockHttp.get).toHaveBeenCalledWith('/api/admin/disputes', {
        params: { status: 0 },
      });
    });
  });

  describe('resolveDispute', () => {
    it('PUT /api/admin/disputes/7/resolve，body={adminRemark}，不含 adminId', async () => {
      mockHttp.put.mockReturnValueOnce(ok(null));
      await adminService.resolveDispute(7, '裁定');
      expect(mockHttp.put).toHaveBeenCalledWith('/api/admin/disputes/7/resolve', {
        adminRemark: '裁定',
      });
    });
  });

  describe('logs', () => {
    it('GET /api/admin/logs?page=2&size=10', async () => {
      mockHttp.get.mockReturnValueOnce(ok([]));
      await adminService.logs(2, 10);
      expect(mockHttp.get).toHaveBeenCalledWith('/api/admin/logs', {
        params: { page: 2, size: 10 },
      });
    });
  });

  describe('banUser / unbanUser', () => {
    it('banUser: POST /api/admin/users/5/ban，body={reason}', async () => {
      mockHttp.post.mockReturnValueOnce(ok(null));
      await adminService.banUser(5, 'x');
      expect(mockHttp.post).toHaveBeenCalledWith('/api/admin/users/5/ban', { reason: 'x' });
    });

    it('unbanUser: POST /api/admin/users/5/unban，无 body', async () => {
      mockHttp.post.mockReturnValueOnce(ok(null));
      await adminService.unbanUser(5);
      expect(mockHttp.post).toHaveBeenCalledWith('/api/admin/users/5/unban');
    });
  });
});

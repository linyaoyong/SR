import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

vi.mock('../services/admin', () => ({
  adminService: {
    logs: vi.fn(),
  },
}));

import { adminService } from '../services/admin';
import LogsPage from './LogsPage';
import type { AdminLog } from '../types/api';

const mockLogs = adminService.logs as unknown as Mock;

// 构造 n 条假日志，id 从 startId 起递增
function makeLogs(n: number, startId = 1): AdminLog[] {
  return Array.from({ length: n }, (_, i) => ({
    id: startId + i,
    adminId: 1,
    operationType: 'AUDIT_USER',
    targetType: 'USER',
    targetId: 100 + i,
    remark: `测试日志 ${startId + i}`,
    ip: '127.0.0.1',
    createTime: '2026-06-28T10:00:00',
  }));
}

function renderPage() {
  return render(
    <MemoryRouter>
      <LogsPage />
    </MemoryRouter>,
  );
}

describe('LogsPage', () => {
  beforeEach(() => {
    mockLogs.mockReset();
  });

  it('初始加载调用 logs(1, 20)', async () => {
    mockLogs.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockLogs).toHaveBeenCalledWith(1, 20));
  });

  it('点击"下一页"调用 logs(2, 20)', async () => {
    // 第一页满页（20 条），下一页按钮启用
    mockLogs.mockResolvedValueOnce(makeLogs(20));
    // 第二页返回空
    mockLogs.mockResolvedValueOnce([]);
    renderPage();

    await waitFor(() => expect(mockLogs).toHaveBeenCalledWith(1, 20));

    // antd Button 对中文会自动插入空格，用正则容错
    await userEvent.click(screen.getByRole('button', { name: /下\s*一\s*页/ }));

    await waitFor(() => expect(mockLogs).toHaveBeenCalledWith(2, 20));
  });

  it('当返回条数 < size 时"下一页"禁用', async () => {
    // 返回 5 条（< 20），说明没有下一页
    mockLogs.mockResolvedValue(makeLogs(5));
    renderPage();

    await waitFor(() => expect(mockLogs).toHaveBeenCalled());

    const nextBtn = screen.getByRole('button', { name: /下\s*一\s*页/ });
    expect(nextBtn).toBeDisabled();
  });

  it('page=1 时"上一页"禁用', async () => {
    mockLogs.mockResolvedValue(makeLogs(20));
    renderPage();

    await waitFor(() => expect(mockLogs).toHaveBeenCalled());

    const prevBtn = screen.getByRole('button', { name: /上\s*一\s*页/ });
    expect(prevBtn).toBeDisabled();
  });

  it('空日志列表显示"暂无日志"', async () => {
    mockLogs.mockResolvedValue([]);
    renderPage();

    await waitFor(() => expect(mockLogs).toHaveBeenCalled());

    expect(await screen.findByText(/暂无日志/)).toBeInTheDocument();
  });

  it('操作类型和目标类型渲染为中文，不显示英文 code 和 IP 列', async () => {
    mockLogs.mockResolvedValue([
      {
        id: 1,
        adminId: 1,
        operationType: 'AUDIT_USER',
        targetType: 'USER',
        targetId: 100,
        remark: '审核通过',
        ip: '127.0.0.1',
        createTime: '2026-06-28T10:00:00',
      },
    ]);
    renderPage();

    await waitFor(() => expect(mockLogs).toHaveBeenCalled());

    expect(await screen.findByText('用户审核')).toBeInTheDocument();
    expect(screen.getByText('用户')).toBeInTheDocument();
    expect(screen.queryByText('AUDIT_USER')).toBeNull();
    expect(screen.queryByText('USER')).toBeNull();
    expect(screen.queryByText('IP')).not.toBeInTheDocument();
    expect(screen.queryByText('127.0.0.1')).not.toBeInTheDocument();
  });
});

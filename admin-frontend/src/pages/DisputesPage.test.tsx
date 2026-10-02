import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

// mock antd 的 message，避免真实 message 创建游离 DOM 干扰断言
vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>();
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
      warning: vi.fn(),
      loading: vi.fn(),
    },
  };
});

vi.mock('../services/admin', () => ({
  adminService: {
    disputes: vi.fn(),
    resolveDispute: vi.fn(),
  },
}));

import { message } from 'antd';
import { adminService } from '../services/admin';
import DisputesPage from './DisputesPage';
import type { Dispute } from '../types/api';

const mockDisputes = adminService.disputes as unknown as Mock;
const mockResolveDispute = adminService.resolveDispute as unknown as Mock;
const mockMessage = message as unknown as { success: Mock; error: Mock; warning: Mock };

const DISPUTE_PENDING: Dispute = {
  id: 11,
  orderId: 1001,
  applicantId: 5,
  reason: '物品损坏',
  description: '租赁的物品出现严重划痕',
  expectedDepositDeduction: 200,
  imageUrls: '["https://example.com/a.png","https://example.com/b.png"]',
  status: 0,
  createTime: '2026-06-28T10:00:00',
};

const DISPUTE_RESOLVED: Dispute = {
  id: 22,
  orderId: 1002,
  applicantId: 6,
  reason: '送达延迟',
  status: 2,
  adminId: 1,
  adminRemark: '已与用户协商',
  createTime: '2026-06-27T10:00:00',
  updateTime: '2026-06-27T12:00:00',
};

function renderPage() {
  return render(
    <MemoryRouter>
      <DisputesPage />
    </MemoryRouter>,
  );
}

describe('DisputesPage', () => {
  beforeEach(() => {
    mockDisputes.mockReset();
    mockResolveDispute.mockReset();
    mockMessage.success.mockReset();
    mockMessage.error.mockReset();
    mockMessage.warning.mockReset();
  });

  it('默认筛选"待处理"，首次调用 disputes(0)', async () => {
    mockDisputes.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockDisputes).toHaveBeenCalledWith(0));
  });

  it('切换到"全部"时调用 disputes(undefined)', async () => {
    mockDisputes.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockDisputes).toHaveBeenCalled());
    mockDisputes.mockClear();

    // antd Radio 的原生 input 被隐藏，点击可见 label 文本触发
    await userEvent.click(screen.getByText('全部'));

    await waitFor(() => expect(mockDisputes).toHaveBeenCalledWith(undefined));
  });

  it('status=2 的行不显示"裁定"按钮', async () => {
    mockDisputes.mockResolvedValue([DISPUTE_RESOLVED]);
    renderPage();

    await waitFor(() => expect(screen.getByText('送达延迟')).toBeInTheDocument());

    // 已裁定（status=2）的异议行不应出现"裁定"按钮
    expect(screen.queryByRole('button', { name: /裁\s*定/ })).toBeNull();
  });

  it('status=0 的行点击"裁定"输入 adminRemark 后调用 resolveDispute(id, adminRemark)，payload 不含 adminId', async () => {
    mockDisputes.mockResolvedValueOnce([DISPUTE_PENDING]);
    mockResolveDispute.mockResolvedValueOnce(null);
    mockDisputes.mockResolvedValueOnce([]);
    renderPage();

    await waitFor(() => expect(screen.getByText('物品损坏')).toBeInTheDocument());

    // antd Button 对两字中文会自动插入空格（"裁定"→"裁 定"），用正则容错
    await userEvent.click(screen.getByRole('button', { name: /裁\s*定/ }));

    const textarea = await screen.findByPlaceholderText(/请输入裁定备注/);
    await userEvent.type(textarea, '扣除押金 200');

    await userEvent.click(screen.getByRole('button', { name: /提\s*交/ }));

    await waitFor(() =>
      expect(mockResolveDispute).toHaveBeenCalledWith(11, '扣除押金 200'),
    );
    // payload 只含 adminRemark，不含 adminId
    expect(mockResolveDispute.mock.calls[0]).toEqual([11, '扣除押金 200']);
    expect(mockMessage.success).toHaveBeenCalled();
  });

  it('点击"查看详情"打开抽屉，解析 imageUrls 显示缩略图', async () => {
    mockDisputes.mockResolvedValue([DISPUTE_PENDING]);
    renderPage();

    await waitFor(() => expect(screen.getByText('物品损坏')).toBeInTheDocument());

    await userEvent.click(screen.getByRole('button', { name: /查看详情/ }));

    // 抽屉打开后应展示两张缩略图（imageUrls 是 JSON 数组字符串，需 JSON.parse）
    await waitFor(() => {
      const imgs = document.querySelectorAll(
        'img[src="https://example.com/a.png"], img[src="https://example.com/b.png"]',
      );
      expect(imgs).toHaveLength(2);
    });
  });
});

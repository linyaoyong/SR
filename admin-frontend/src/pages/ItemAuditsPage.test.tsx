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
    itemAudits: vi.fn(),
    auditItem: vi.fn(),
    forceOffShelf: vi.fn(),
  },
}));

import { message } from 'antd';
import { adminService } from '../services/admin';
import ItemAuditsPage from './ItemAuditsPage';
import type { ItemAuditItem } from '../types/api';

const mockItemAudits = adminService.itemAudits as unknown as Mock;
const mockAuditItem = adminService.auditItem as unknown as Mock;
const mockForceOffShelf = adminService.forceOffShelf as unknown as Mock;
const mockMessage = message as unknown as { success: Mock; error: Mock; warning: Mock };

const ITEM: ItemAuditItem = {
  id: 12,
  ownerId: 5,
  title: '电动自行车',
  status: 0,
  auditStatus: 0,
};

function renderPage() {
  return render(
    <MemoryRouter>
      <ItemAuditsPage />
    </MemoryRouter>,
  );
}

describe('ItemAuditsPage', () => {
  beforeEach(() => {
    mockItemAudits.mockReset();
    mockAuditItem.mockReset();
    mockForceOffShelf.mockReset();
    mockMessage.success.mockReset();
    mockMessage.error.mockReset();
    mockMessage.warning.mockReset();
  });

  it('默认筛选"待审核"，首次调用 itemAudits(0)', async () => {
    mockItemAudits.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockItemAudits).toHaveBeenCalledWith(0));
  });

  it('切换到"全部"时调用 itemAudits(undefined)', async () => {
    mockItemAudits.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockItemAudits).toHaveBeenCalled());
    mockItemAudits.mockClear();

    // antd Radio 的原生 input 被隐藏，点击可见 label 文本触发
    await userEvent.click(screen.getByText('全部'));

    await waitFor(() => expect(mockItemAudits).toHaveBeenCalledWith(undefined));
  });

  it('点击"通过"按钮调用 auditItem(id, { auditStatus:1 })（不含 auditReason）并刷新列表', async () => {
    mockItemAudits.mockResolvedValueOnce([ITEM]);
    mockAuditItem.mockResolvedValueOnce({
      itemId: 12,
      ownerId: 5,
      auditStatus: 1,
      auditTime: '2026-06-28T10:00:00',
      auditAdminId: 1,
    });
    mockItemAudits.mockResolvedValueOnce([]);
    renderPage();

    await waitFor(() => expect(screen.getByText('电动自行车')).toBeInTheDocument());

    // antd Button 对两字中文会自动插入空格（"通过"→"通 过"），用正则容错
    await userEvent.click(screen.getByRole('button', { name: /通\s*过/ }));

    await waitFor(() =>
      expect(mockAuditItem).toHaveBeenCalledWith(12, { auditStatus: 1 }),
    );
    // 操作成功后重新拉取列表
    await waitFor(() => expect(mockItemAudits).toHaveBeenCalledTimes(2));
    expect(mockMessage.success).toHaveBeenCalled();
  });

  it('点击"要求整改"打开 Modal 输入原因后调用 auditItem(id, { auditStatus:2, auditReason })', async () => {
    mockItemAudits.mockResolvedValue([ITEM]);
    mockAuditItem.mockResolvedValue(null);
    renderPage();

    await waitFor(() => expect(screen.getByText('电动自行车')).toBeInTheDocument());

    await userEvent.click(screen.getByRole('button', { name: '要求整改' }));

    const textarea = await screen.findByPlaceholderText(/请输入整改原因/);
    await userEvent.type(textarea, '标题违规');

    await userEvent.click(screen.getByRole('button', { name: /提\s*交/ }));

    await waitFor(() =>
      expect(mockAuditItem).toHaveBeenCalledWith(12, {
        auditStatus: 2,
        auditReason: '标题违规',
      }),
    );
    expect(mockMessage.success).toHaveBeenCalled();
  });

  it('点击"强制下架"Popconfirm 确认后调用 forceOffShelf(id)', async () => {
    mockItemAudits.mockResolvedValueOnce([ITEM]);
    mockForceOffShelf.mockResolvedValueOnce(null);
    mockItemAudits.mockResolvedValueOnce([]);
    renderPage();

    await waitFor(() => expect(screen.getByText('电动自行车')).toBeInTheDocument());

    // 点击"强制下架"按钮触发 Popconfirm
    await userEvent.click(screen.getByRole('button', { name: /强\s*制\s*下\s*架/ }));

    // Popconfirm 弹出后点击"确定"按钮
    const okBtn = await screen.findByRole('button', { name: /确\s*定/ });
    await userEvent.click(okBtn);

    await waitFor(() => expect(mockForceOffShelf).toHaveBeenCalledWith(12));
    expect(mockMessage.success).toHaveBeenCalled();
  });
});

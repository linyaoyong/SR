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
    userAudits: vi.fn(),
    auditUser: vi.fn(),
    banUser: vi.fn(),
    unbanUser: vi.fn(),
  },
}));

import { message } from 'antd';
import { adminService } from '../services/admin';
import UserAuditsPage from './UserAuditsPage';
import UserAuditDetailPage from './UserAuditDetailPage';
import type { UserAuditItem } from '../types/api';

const mockUserAudits = adminService.userAudits as unknown as Mock;
const mockAuditUser = adminService.auditUser as unknown as Mock;
const mockBanUser = adminService.banUser as unknown as Mock;
const mockUnbanUser = adminService.unbanUser as unknown as Mock;
const mockMessage = message as unknown as { success: Mock; error: Mock; warning: Mock };

const ITEM: UserAuditItem = {
  userId: 5,
  username: 'user05',
  fieldName: 'username',
  auditStatus: 0,
};

function renderPage() {
  return render(
    <MemoryRouter>
      <UserAuditsPage />
    </MemoryRouter>,
  );
}

describe('UserAuditsPage', () => {
  beforeEach(() => {
    mockUserAudits.mockReset();
    mockAuditUser.mockReset();
    mockBanUser.mockReset();
    mockUnbanUser.mockReset();
    mockMessage.success.mockReset();
    mockMessage.error.mockReset();
    mockMessage.warning.mockReset();
  });

  it('默认筛选"待审核"，首次调用 userAudits(0)', async () => {
    mockUserAudits.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockUserAudits).toHaveBeenCalledWith(0));
  });

  it('切换到"全部"时调用 userAudits(undefined)', async () => {
    mockUserAudits.mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(mockUserAudits).toHaveBeenCalled());
    mockUserAudits.mockClear();

    // antd Radio 的原生 input 被隐藏（pointer-events:none），点击可见 label 文本触发
    await userEvent.click(screen.getByText('全部'));

    await waitFor(() => expect(mockUserAudits).toHaveBeenCalledWith(undefined));
  });

  it('点击"通过"按钮调用 auditUser(userId, { auditStatus:1, fieldName }) 并刷新列表', async () => {
    mockUserAudits.mockResolvedValueOnce([ITEM]);
    mockAuditUser.mockResolvedValueOnce(null);
    mockUserAudits.mockResolvedValueOnce([]);
    renderPage();

    await waitFor(() => expect(screen.getByText('user05')).toBeInTheDocument());

    // antd Button 对两字中文会自动插入空格（"通过"→"通 过"），用正则容错
    await userEvent.click(screen.getByRole('button', { name: /通\s*过/ }));

    await waitFor(() =>
      expect(mockAuditUser).toHaveBeenCalledWith(5, {
        auditStatus: 1,
        fieldName: 'username',
      }),
    );
    // 操作成功后重新拉取列表
    await waitFor(() => expect(mockUserAudits).toHaveBeenCalledTimes(2));
    expect(mockMessage.success).toHaveBeenCalled();
  });

  it('点击"要求整改"打开 Modal 输入原因后调用 auditUser(userId, { auditStatus:2, auditReason, fieldName })', async () => {
    mockUserAudits.mockResolvedValue([ITEM]);
    mockAuditUser.mockResolvedValue(null);
    renderPage();

    await waitFor(() => expect(screen.getByText('user05')).toBeInTheDocument());

    await userEvent.click(screen.getByRole('button', { name: '要求整改' }));

    const textarea = await screen.findByPlaceholderText(/请输入整改原因/);
    await userEvent.type(textarea, '昵称违规');

    await userEvent.click(screen.getByRole('button', { name: /提\s*交/ }));

    await waitFor(() =>
      expect(mockAuditUser).toHaveBeenCalledWith(5, {
        auditStatus: 2,
        auditReason: '昵称违规',
        fieldName: 'username',
      }),
    );
    expect(mockMessage.success).toHaveBeenCalled();
  });

  it('一行展示所有待审核字段（fieldNames 渲染为中文顿号连接）', async () => {
    mockUserAudits.mockResolvedValue([
      {
        userId: 6,
        username: 'user06',
        fieldName: 'username',
        auditStatus: 0,
        fieldNames: ['username', 'avatar'],
        auditStatuses: [0, 0],
      },
    ]);
    renderPage();

    await waitFor(() => expect(screen.getByText('user06')).toBeInTheDocument());
    expect(screen.getByText('用户名、头像')).toBeInTheDocument();
  });
});

describe('UserAuditDetailPage', () => {
  beforeEach(() => {
    mockUserAudits.mockReset();
    mockAuditUser.mockReset();
    mockBanUser.mockReset();
    mockUnbanUser.mockReset();
    mockMessage.success.mockReset();
    mockMessage.error.mockReset();
    mockMessage.warning.mockReset();
  });

  const DETAIL_ITEM: UserAuditItem = {
    userId: 5,
    username: 'user05',
    fieldName: 'username',
    auditStatus: 0,
    avatarUrl: 'https://example.com/avatar.png',
    description: '这是一个测试用户的简介',
    creditScore: 80,
    status: 0,
    showRentalHistory: 1,
    usernameAuditStatus: 0,
    avatarAuditStatus: 1,
    descriptionAuditStatus: 2,
  };

  it('展示用户头像和简介字段（带新字段数据）', () => {
    mockUserAudits.mockResolvedValue([]);
    render(
      <MemoryRouter
        initialEntries={[{ pathname: '/users/audits/5', state: { item: DETAIL_ITEM } }]}
      >
        <UserAuditDetailPage />
      </MemoryRouter>,
    );

    // 面板标题
    expect(screen.getByText('用户资料审核详情')).toBeInTheDocument();
    // 头像图片
    const avatarImg = screen.getByAltText('用户头像');
    expect(avatarImg).toHaveAttribute('src', 'https://example.com/avatar.png');
    // 简介原文
    expect(screen.getByText('这是一个测试用户的简介')).toBeInTheDocument();
  });
});

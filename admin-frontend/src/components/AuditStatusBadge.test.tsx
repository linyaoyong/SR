import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import AuditStatusBadge from './AuditStatusBadge';

// 审核状态徽标：3 个状态的文案与颜色 class 必须一一对应
describe('AuditStatusBadge', () => {
  it('auditStatus=0 渲染"待审核"并使用灰色徽标 class', () => {
    render(<AuditStatusBadge auditStatus={0} />);
    const el = screen.getByText('待审核');
    expect(el).toHaveClass('admin-badge--muted');
  });

  it('auditStatus=1 渲染"审核通过"并使用绿色徽标 class', () => {
    render(<AuditStatusBadge auditStatus={1} />);
    const el = screen.getByText('审核通过');
    expect(el).toHaveClass('admin-badge--success');
  });

  it('auditStatus=2 渲染"要求整改"并使用橙色徽标 class', () => {
    render(<AuditStatusBadge auditStatus={2} />);
    const el = screen.getByText('要求整改');
    expect(el).toHaveClass('admin-badge--warning');
  });

  it('未知状态回退到"未知"文案', () => {
    render(<AuditStatusBadge auditStatus={99} />);
    expect(screen.getByText('未知')).toBeInTheDocument();
  });
});

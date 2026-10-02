import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import OperationLogTable from './OperationLogTable';
import type { AdminLog } from '../types/api';

const LOG: AdminLog = {
  id: 1,
  adminId: 7,
  operationType: 'AUDIT_ITEM',
  targetType: 'ITEM',
  targetId: 99,
  remark: '图片不清晰',
  ip: '127.0.0.1',
  createTime: '2026-06-28T10:00:00',
};

describe('OperationLogTable', () => {
  it('已知操作类型/目标类型渲染为中文，不显示英文 code', () => {
    render(<OperationLogTable logs={[LOG]} />);

    expect(screen.getByText('物品审核')).toBeInTheDocument();
    expect(screen.getByText('物品')).toBeInTheDocument();
    expect(screen.queryByText('AUDIT_ITEM')).toBeNull();
    expect(screen.queryByText('ITEM')).toBeNull();
  });

  it('未知 code 回退到原值', () => {
    render(
      <OperationLogTable
        logs={[{ ...LOG, operationType: 'SOMETHING_NEW', targetType: 'WEIRD' }]}
      />,
    );

    expect(screen.getByText('SOMETHING_NEW')).toBeInTheDocument();
    expect(screen.getByText('WEIRD')).toBeInTheDocument();
  });

  it('备注为空时显示 -，且不显示 IP 列', () => {
    render(<OperationLogTable logs={[{ ...LOG, remark: '', ip: '' }]} />);

    expect(screen.getByText('-')).toBeInTheDocument();
    expect(screen.queryByText('IP')).not.toBeInTheDocument();
    expect(screen.queryByText('127.0.0.1')).not.toBeInTheDocument();
  });
});

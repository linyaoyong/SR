import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import ItemForm from './ItemForm';
import type { Category } from '../types/api';

const enabledCategory: Category = { id: 1, name: '数码设备', sortOrder: 1, status: 0 };
const disabledCategory: Category = { id: 2, name: '停用分类', sortOrder: 2, status: 1 };

function renderForm(options: {
  categories?: Category[];
  initialValues?: React.ComponentProps<typeof ItemForm>['initialValues'];
  onSubmit?: React.ComponentProps<typeof ItemForm>['onSubmit'];
} = {}) {
  const onSubmit = options.onSubmit ?? vi.fn().mockResolvedValue(undefined);
  render(
    <ItemForm
      mode="create"
      categories={options.categories ?? [enabledCategory]}
      initialValues={options.initialValues}
      onSubmit={onSubmit}
      submitting={false}
    />,
  );
  return { onSubmit };
}

describe('ItemForm', () => {
  it('shows status=0 categories as selectable and hides status=1 categories', async () => {
    renderForm({ categories: [enabledCategory, disabledCategory] });

    await userEvent.click(screen.getByRole('combobox', { name: '分类' }));

    await waitFor(() => {
      expect(screen.getByText('数码设备')).toBeInTheDocument();
    });
    expect(screen.queryByText('停用分类')).not.toBeInTheDocument();
  });

  it('adds removable tags and submits them as a space separated string', async () => {
    const { onSubmit } = renderForm({
      initialValues: {
        title: '电钻',
        description: '适合家庭维修',
        categoryId: 1,
        quantity: 1,
        supportMeetup: 1,
        priceType: 0,
        dailyPrice: 20,
        minRentDays: 1,
        depositEnabled: 0,
        creditDepositEnabled: 0,
      },
    });

    await userEvent.type(screen.getByLabelText('标签'), '工具');
    await userEvent.click(screen.getByRole('button', { name: '添加标签' }));

    const tagList = screen.getByLabelText('已添加标签');
    expect(within(tagList).getByText('工具')).toBeInTheDocument();
    expect(screen.getByLabelText('标签')).toHaveValue('');

    await userEvent.type(screen.getByLabelText('标签'), '家用');
    await userEvent.click(screen.getByRole('button', { name: '添加标签' }));
    expect(within(tagList).getByText('家用')).toBeInTheDocument();

    await userEvent.click(within(tagList).getByRole('button', { name: '删除标签 工具' }));
    expect(within(tagList).queryByText('工具')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: '发布物品' }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          tags: '家用',
        }),
      );
    });
  });
});

import { expect, test } from '@playwright/test';

// 管理后台冒烟测试：验证登录页核心元素可见且可交互。
// antd 5 会对相邻 CJK 字符自动插空格（"登录"→"登 录"），用正则容错。
test('admin login page renders', async ({ page }) => {
  await page.goto('/login');
  // 登录按钮：antd Button 文本相邻 CJK 会被自动插空格，正则容错
  await expect(page.getByRole('button', { name: /^登\s?录$/ })).toBeVisible();
  // 表单 label：AdminLoginPage.tsx 使用 label="账号"（非"用户名"）
  await expect(page.getByLabel('账号')).toBeVisible();
  await expect(page.getByLabel('密码')).toBeVisible();
});

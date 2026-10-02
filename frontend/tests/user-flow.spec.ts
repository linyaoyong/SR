import { expect, test } from '@playwright/test';

// 冒烟测试：验证公开物品列表与鉴权页面能正常渲染。
// 不依赖后端数据：/items 在 API 失败时仍渲染顶栏导航与页面标题；
// /login、/register 为纯前端页面。
test('public item list and auth pages render', async ({ page }) => {
  await page.goto('/items');
  // desktop 渲染顶栏导航、mobile 渲染底栏导航，二者至少有一个可见
  await expect(page.getByRole('navigation')).toBeVisible();
  // 页面标题“租赁”在 desktop/mobile 下均唯一可见
  await expect(page.getByRole('heading', { name: '租赁' })).toBeVisible();
  await page.goto('/login');
  // antd 5 的 Button 会对两个中文字符自动插入空格（CJK spacing），
  // “登录”实际可访问名称为“登 录”，故用正则兼容。
  await expect(page.getByRole('button', { name: /^登\s?录$/ })).toBeVisible();
  await page.goto('/register');
  await expect(page.getByRole('button', { name: /^注\s?册$/ })).toBeVisible();
});

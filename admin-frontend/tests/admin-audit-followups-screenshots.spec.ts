import { expect, test } from '@playwright/test';
import { existsSync, statSync } from 'node:fs';
import { resolve } from 'node:path';

// 管理员审核功能桌面截图验收（1440x900，由 playwright.config.ts 的 desktop 项目提供视口）。
// 覆盖：用户审核列表/详情、物品审核列表/详情、封禁管理页。
// 注意：登录页用户名输入框 label 实际为 "账号"（见 AdminLoginPage.tsx），非 "用户名"。
// 详情页依赖 location.state 携带列表项，必须从列表点击 "详情" 按钮进入，不能直接访问 URL。

const SCREENSHOT_DIR = 'docs/screenshots';
const ADMIN_BASE = 'http://127.0.0.1:5174';

const SHOTS = {
  userList: resolve(SCREENSHOT_DIR, 'audit-followups-user-audits-list.png'),
  userDetail: resolve(SCREENSHOT_DIR, 'audit-followups-user-audit-detail.png'),
  itemList: resolve(SCREENSHOT_DIR, 'audit-followups-item-audits-list.png'),
  itemDetail: resolve(SCREENSHOT_DIR, 'audit-followups-item-audit-detail.png'),
  bans: resolve(SCREENSHOT_DIR, 'audit-followups-bans.png'),
} as const;

// 登录并验证 token 写入 localStorage
async function adminLogin(page: import('@playwright/test').Page) {
  await page.goto(`${ADMIN_BASE}/login`);
  // 用户名输入框 label="账号"
  await page.getByLabel('账号').fill('admin');
  await page.getByLabel('密码').fill('123456');
  // antd 两字中文按钮会自动插空格，用正则容错
  await page.getByRole('button', { name: /^登\s?录$/ }).click();
  // 登录成功后跳转离开 /login
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 30_000 });
  // 校验 token 已写入 localStorage（直接在页面上下文读取，兼容各 Playwright 版本）
  const token = await page.evaluate(() => localStorage.getItem('sr_admin_access_token'));
  expect(token, '登录后 localStorage 应写入 sr_admin_access_token').toBeTruthy();
}

test.describe('管理员审核功能桌面截图验收', () => {
  test('登录并截取 5 个审核相关页面', async ({ page }) => {
    await adminLogin(page);

    // ---------- 1. 用户审核列表 ----------
    await page.goto(`${ADMIN_BASE}/users/audits`);
    // 等待表格加载完成（loading Spin 消失，表格主体出现）
    await expect(page.getByRole('heading', { name: '用户审核' })).toBeVisible();
    await page.waitForSelector('.ant-table-tbody', { timeout: 30_000 });
    // 等待 Spin 消失
    await expect(page.locator('.ant-spin-spinning')).toHaveCount(0, { timeout: 30_000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: SHOTS.userList, fullPage: true });

    // ---------- 2. 用户审核详情 ----------
    // 从列表第一行点击 "详情" 进入详情页（携带 location.state）。
    // 用 href 定位：<Link to={`/users/audits/${id}`}> 渲染为 <a href="/users/audits/123">。
    // 不用 getByRole(link, { name: '详情' })，因为 <a> 内嵌 <Button> 时链接可访问名可能不含按钮文本。
    const userDetailLinks = page.locator('a[href^="/users/audits/"]');
    const userDetailCount = await userDetailLinks.count();
    if (userDetailCount > 0) {
      await userDetailLinks.first().click();
      await page.waitForURL(/\/users\/audits\/\d+$/, { timeout: 30_000 });
      await expect(page.getByText('用户资料审核详情')).toBeVisible({ timeout: 30_000 });
      await page.waitForTimeout(500);
      await page.screenshot({ path: SHOTS.userDetail, fullPage: true });
    } else {
      // 列表为空：截图空状态作为替代
      await page.screenshot({ path: SHOTS.userDetail, fullPage: true });
      test.info().annotations.push({
        type: 'empty-list',
        description: '用户审核列表为空，详情页截图以列表空状态替代',
      });
    }

    // ---------- 3. 物品审核列表 ----------
    await page.goto(`${ADMIN_BASE}/items/audits`);
    await expect(page.getByRole('heading', { name: '物品审核' })).toBeVisible();
    await page.waitForSelector('.ant-table-tbody', { timeout: 30_000 });
    await expect(page.locator('.ant-spin-spinning')).toHaveCount(0, { timeout: 30_000 });
    await page.waitForTimeout(500);
    await page.screenshot({ path: SHOTS.itemList, fullPage: true });

    // ---------- 4. 物品审核详情 ----------
    const itemDetailLinks = page.locator('a[href^="/items/audits/"]');
    const itemDetailCount = await itemDetailLinks.count();
    if (itemDetailCount > 0) {
      await itemDetailLinks.first().click();
      await page.waitForURL(/\/items\/audits\/\d+$/, { timeout: 30_000 });
      await expect(page.getByText('物品内容审核详情')).toBeVisible({ timeout: 30_000 });
      await page.waitForTimeout(500);
      await page.screenshot({ path: SHOTS.itemDetail, fullPage: true });
    } else {
      await page.screenshot({ path: SHOTS.itemDetail, fullPage: true });
      test.info().annotations.push({
        type: 'empty-list',
        description: '物品审核列表为空，详情页截图以列表空状态替代',
      });
    }

    // ---------- 5. 封禁管理页 ----------
    await page.goto(`${ADMIN_BASE}/bans`);
    await expect(page.getByRole('heading', { name: '封禁管理' })).toBeVisible();
    // 说明卡片：推荐从用户审核详情页进入封禁操作
    await expect(page.getByText('推荐从用户审核详情页进入封禁操作')).toBeVisible();
    await page.waitForTimeout(500);
    await page.screenshot({ path: SHOTS.bans, fullPage: true });

    // ---------- 验收：校验所有截图文件存在且大小 > 0 ----------
    for (const [name, path] of Object.entries(SHOTS)) {
      const exists = existsSync(path);
      expect(exists, `截图文件应存在: ${name} -> ${path}`).toBeTruthy();
      if (exists) {
        const size = statSync(path).size;
        expect(size, `截图文件大小应 > 0: ${name}`).toBeGreaterThan(0);
      }
    }
  });
});

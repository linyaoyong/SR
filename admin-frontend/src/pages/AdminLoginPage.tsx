import { useState } from 'react';
import { Form, Input, Button, message } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import type { Location } from 'react-router-dom';
import { adminAuthService } from '../services/adminAuth';
import { useAdminAuthStore } from '../stores/adminAuthStore';

interface LoginFromState {
  from?: Location;
}

// 管理员登录页：调用 /api/auth/admin/login，成功后写回 store 并跳回来源页
export default function AdminLoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAdminAuthStore((s) => s.setSession);
  const from = (location.state as LoginFromState | null)?.from;

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const session = await adminAuthService.login(values);
      setSession(session); // 非 ADMIN 账号会在此处抛错
      message.success('登录成功');
      navigate(from?.pathname ?? '/', { replace: true });
    } catch (err) {
      message.error(err instanceof Error ? err.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="admin-login">
      <section className="admin-panel admin-login-panel">
        <header className="admin-login-header">
          <span className="admin-brand-mark">邻享</span>
          <div>
            <h1 className="admin-title">管理后台登录</h1>
            <p className="admin-caption">邻享租借平台管理员后台</p>
          </div>
        </header>
        <Form
          layout="vertical"
          onFinish={onFinish}
          autoComplete="on"
          requiredMark={false}
          initialValues={{ username: '', password: '' }}
        >
          <Form.Item
            label="账号"
            name="username"
            rules={[{ required: true, message: '请输入账号' }]}
          >
            <Input
              name="username"
              autoComplete="username"
              placeholder="请输入管理员账号"
              allowClear
            />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              name="password"
              autoComplete="current-password"
              placeholder="请输入密码"
            />
          </Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            loading={loading}
            block
            className="admin-btn-primary"
          >
            登录
          </Button>
        </Form>
      </section>
    </div>
  );
}

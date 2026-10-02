import { useState } from 'react';
import { Button, Form, Input, message } from 'antd';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authService, type LoginRequest } from '../services/auth';
import { useAuthStore } from '../stores/authStore';
import type { LocationState } from '../types/router';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((state) => state.setSession);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

  const from = (location.state as LocationState | null)?.from?.pathname || '/items';

  const onFinish = async (values: LoginRequest) => {
    setLoading(true);
    try {
      const session = await authService.login(values);
      setSession(session);
      const me = await authService.me();
      setCurrentUser(me);
      message.success('登录成功');
      navigate(from, { replace: true });
    } catch (error) {
      const text = error instanceof Error ? error.message : '登录失败';
      message.error(text);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="sr-page sr-auth-page">
      <section className="sr-section sr-auth-section">
        <div className="sr-card sr-auth-card">
          <header className="sr-auth-header">
            <p className="sr-caption">邻享租借平台</p>
            <h1 className="sr-title">登录</h1>
            <p className="sr-text">输入账号密码以浏览、发布和租借物品。</p>
          </header>
          <Form<LoginRequest>
            layout="vertical"
            onFinish={onFinish}
            autoComplete="off"
            className="sr-auth-form"
          >
            <Form.Item
              label="用户名"
              name="username"
              rules={[{ required: true, message: '请输入用户名' }, { min: 3, max: 20, message: '用户名长度 3-20' }]}
            >
              <Input autoComplete="username" placeholder="请输入用户名" />
            </Form.Item>
            <Form.Item
              label="密码"
              name="password"
              rules={[{ required: true, message: '请输入密码' }, { min: 6, max: 32, message: '密码长度 6-32' }]}
            >
              <Input.Password autoComplete="current-password" placeholder="请输入密码" />
            </Form.Item>
            <Form.Item style={{ marginBottom: 0 }}>
              <Button type="primary" htmlType="submit" block loading={loading}>
                登录
              </Button>
            </Form.Item>
          </Form>
          <p className="sr-text sr-auth-switch">
            还没有账号？<Link to="/register">立即注册</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

import { useState } from 'react';
import { Button, Form, Input, message } from 'antd';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authService, type RegisterRequest } from '../services/auth';
import { useAuthStore } from '../stores/authStore';
import type { LocationState } from '../types/router';

export default function RegisterPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((state) => state.setSession);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

  const from = (location.state as LocationState | null)?.from?.pathname || '/items';

  const onFinish = async (values: RegisterRequest) => {
    setLoading(true);
    try {
      const loginPayload = {
        username: values.username,
        password: values.password,
      };
      await authService.register(loginPayload);
      const session = await authService.login(loginPayload);
      setSession(session);
      const me = await authService.me();
      setCurrentUser(me);
      message.success('注册成功');
      navigate(from, { replace: true });
    } catch (error) {
      const text = error instanceof Error ? error.message : '注册失败';
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
            <h1 className="sr-title">注册</h1>
            <p className="sr-text">创建账号后即可发布物品、发起租借申请。</p>
          </header>
          <Form<RegisterRequest>
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
              <Input autoComplete="username" placeholder="3-20 个字符" />
            </Form.Item>
            <Form.Item
              label="密码"
              name="password"
              rules={[{ required: true, message: '请输入密码' }, { min: 6, max: 32, message: '密码长度 6-32' }]}
            >
              <Input.Password autoComplete="new-password" placeholder="6-32 个字符" />
            </Form.Item>
            <Form.Item
              label="确认密码"
              name="confirmPassword"
              dependencies={['password']}
              rules={[
                { required: true, message: '请再次输入密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('password') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('两次输入的密码不一致'));
                  },
                }),
              ]}
            >
              <Input.Password autoComplete="new-password" placeholder="请再次输入密码" />
            </Form.Item>
            <Form.Item style={{ marginBottom: 0 }}>
              <Button type="primary" htmlType="submit" block loading={loading}>
                注册
              </Button>
            </Form.Item>
          </Form>
          <p className="sr-text sr-auth-switch">
            已有账号？<Link to="/login">直接登录</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

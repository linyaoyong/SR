import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert, Avatar, Button, Form, Input, Radio, Spin, Tag, message } from 'antd';
import { Link } from 'react-router-dom';
import { userService } from '../services/user';
import { useAuthStore } from '../stores/authStore';
import { useWalletStore } from '../stores/walletStore';
import { userAuditStatusText, userRoleText, userStatusText } from '../types/status';
import type { UserMeResponse } from '../types/api';

interface ProfileFormValues {
  username: string;
  description?: string;
  showRentalHistory: number;
}

interface PasswordFormValues {
  oldPassword: string;
  newPassword: string;
  confirm: string;
}

// 我的资料页：展示用户资料 + 审核状态 + 封禁/欠费提示 + 编辑资料 / 修改密码表单。
// 字段对齐 docs/03-api-contract.md 第 8.3 节 UserMeResponse。
export default function ProfilePage() {
  const currentUser = useAuthStore((s) => s.currentUser);
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser);
  const hasDebt = useWalletStore((s) => s.hasDebt());

  const [loading, setLoading] = useState(true);
  const [profileForm] = Form.useForm<ProfileFormValues>();
  const [passwordForm] = Form.useForm<PasswordFormValues>();
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const avatarInputRef = useRef<HTMLInputElement>(null);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    try {
      const me = await userService.me();
      setCurrentUser(me);
      profileForm.setFieldsValue({
        username: me.username,
        description: me.description ?? '',
        showRentalHistory: me.showRentalHistory,
      });
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [profileForm, setCurrentUser]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleProfileSubmit = async (values: ProfileFormValues) => {
    setSavingProfile(true);
    try {
      const updated = await userService.updateMe({
        username: values.username,
        description: values.description,
        showRentalHistory: values.showRentalHistory,
      });
      setCurrentUser(updated);
      profileForm.setFieldsValue({
        username: updated.username,
        description: updated.description ?? '',
        showRentalHistory: updated.showRentalHistory,
      });
      message.success('资料已保存');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    } finally {
      setSavingProfile(false);
    }
  };

  const handlePasswordSubmit = async (values: PasswordFormValues) => {
    setSavingPassword(true);
    try {
      await userService.changePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword,
      });
      message.success('密码已修改');
      passwordForm.resetFields();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '修改失败');
    } finally {
      setSavingPassword(false);
    }
  };

  const handleAvatarChange = async (file: File | undefined) => {
    if (!file) return;
    setUploadingAvatar(true);
    try {
      await userService.updateAvatar(file);
      await loadProfile();
      message.success('头像已上传，等待审核');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '头像上传失败');
    } finally {
      setUploadingAvatar(false);
      if (avatarInputRef.current) avatarInputRef.current.value = '';
    }
  };

  if (loading && !currentUser) {
    return (
      <main className="sr-page">
        <section className="sr-section sr-detail-loading">
          <Spin />
        </section>
      </main>
    );
  }

  const me: UserMeResponse | null = currentUser;

  return (
    <main className="sr-page">
      <section className="sr-section">
        <h1 className="sr-title">我的</h1>
        {me && me.status === 1 && (
          <Alert
            type="error"
            showIcon
            message="账户已被封禁"
            description="你的账户已被管理员封禁，部分功能将无法使用。如有疑问请联系管理员。"
            style={{ marginBottom: 'var(--space-4)' }}
          />
        )}
        {hasDebt && (
          <Alert
            type="warning"
            showIcon
            message="钱包欠费"
            description="钱包余额不足，将无法发起新的租借交易。"
            action={
              <Link to="/wallet" className="sr-btn sr-btn-sm sr-btn-primary">
                模拟充值
              </Link>
            }
            style={{ marginBottom: 'var(--space-4)' }}
          />
        )}

        {me && (
          <section
            className="sr-card"
            aria-label="用户资料"
            style={{ padding: 'var(--space-5)', marginBottom: 'var(--space-5)' }}
          >
            <div style={{ display: 'flex', gap: 'var(--space-4)', alignItems: 'center', flexWrap: 'wrap' }}>
              <div className="sr-profile-avatar-box">
                {me.avatarUrl ? (
                  <Avatar size={64} src={me.avatarUrl} />
                ) : (
                  <Avatar size={64}>{me.username.slice(0, 1)}</Avatar>
                )}
                <input
                  ref={avatarInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  style={{ display: 'none' }}
                  onChange={(e) => handleAvatarChange(e.target.files?.[0])}
                />
                <Button
                  size="small"
                  loading={uploadingAvatar}
                  onClick={() => avatarInputRef.current?.click()}
                >
                  上传头像
                </Button>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
                <h2 style={{ margin: 0, font: 'var(--font-display)' }}>{me.username}</h2>
                <div style={{ display: 'flex', gap: 'var(--space-2)', flexWrap: 'wrap' }}>
                  <Tag>{userRoleText[me.role] ?? '用户'}</Tag>
                  <Tag color={me.status === 1 ? 'error' : 'success'}>
                    {userStatusText[me.status] ?? '正常'}
                  </Tag>
                  <Tag color="blue">信用分 {me.creditScore}</Tag>
                </div>
                <div style={{ display: 'flex', gap: 'var(--space-4)', flexWrap: 'wrap', color: 'var(--color-text-muted)', fontSize: 12 }}>
                  <span>用户名审核：{userAuditStatusText[me.usernameAuditStatus] ?? '-'}</span>
                  <span>头像审核：{userAuditStatusText[me.avatarAuditStatus] ?? '-'}</span>
                  <span>简介审核：{userAuditStatusText[me.descriptionAuditStatus] ?? '-'}</span>
                </div>
              </div>
            </div>
            {me.description && (
              <p style={{ margin: 'var(--space-4) 0 0', color: 'var(--color-text-muted)' }}>
                {me.description}
              </p>
            )}
            <div
              className="sr-actions"
              style={{ marginTop: 'var(--space-4)' }}
            >
              <Link to="/wallet" className="sr-btn sr-btn-sm sr-btn-secondary">我的钱包</Link>
              <Link to="/blacklist" className="sr-btn sr-btn-sm sr-btn-secondary">黑名单管理</Link>
              <Link to="/my-items" className="sr-btn sr-btn-sm sr-btn-secondary">我的物品</Link>
              <Link to="/orders" className="sr-btn sr-btn-sm sr-btn-secondary">我的订单</Link>
              <Link to="/messages" className="sr-btn sr-btn-sm sr-btn-secondary">我的消息</Link>
            </div>
          </section>
        )}

        <section
          className="sr-card"
          aria-label="编辑资料"
          style={{ padding: 'var(--space-5)', marginBottom: 'var(--space-5)' }}
        >
          <h3 className="sr-detail-heading">编辑资料</h3>
          <Form
            form={profileForm}
            layout="vertical"
            onFinish={handleProfileSubmit}
            initialValues={
              me
                ? {
                    username: me.username,
                    description: me.description ?? '',
                    showRentalHistory: me.showRentalHistory,
                  }
                : undefined
            }
          >
            <Form.Item
              name="username"
              label="用户名"
              tooltip="修改用户名后需要重新审核"
              rules={[
                { required: true, message: '请输入用户名' },
                { min: 3, max: 20, message: '用户名长度 3-20' },
              ]}
            >
              <Input maxLength={20} />
            </Form.Item>
            <Form.Item
              name="description"
              label="简介"
              rules={[{ max: 255, message: '简介最长 255 字' }]}
            >
              <Input.TextArea autoSize={{ minRows: 2 }} maxLength={255} />
            </Form.Item>
            <Form.Item name="showRentalHistory" label="是否展示租借历史">
              <Radio.Group>
                <Radio value={1}>展示</Radio>
                <Radio value={0}>不展示</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={savingProfile}>
                保存资料
              </Button>
            </Form.Item>
          </Form>
        </section>

        <section
          className="sr-card"
          aria-label="修改密码"
          style={{ padding: 'var(--space-5)' }}
        >
          <h3 className="sr-detail-heading">修改密码</h3>
          <Form form={passwordForm} layout="vertical" onFinish={handlePasswordSubmit}>
            <Form.Item
              name="oldPassword"
              label="原密码"
              rules={[{ required: true, message: '请输入原密码' }]}
            >
              <Input.Password autoComplete="current-password" />
            </Form.Item>
            <Form.Item
              name="newPassword"
              label="新密码"
              rules={[
                { required: true, message: '请输入新密码' },
                { min: 6, max: 32, message: '密码长度 6-32' },
              ]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <Form.Item
              name="confirm"
              label="确认新密码"
              dependencies={['newPassword']}
              rules={[
                { required: true, message: '请再次输入新密码' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve();
                    }
                    return Promise.reject(new Error('两次输入的密码不一致'));
                  },
                }),
              ]}
            >
              <Input.Password autoComplete="new-password" />
            </Form.Item>
            <Form.Item>
              <Button htmlType="submit" loading={savingPassword}>
                修改密码
              </Button>
            </Form.Item>
          </Form>
        </section>
      </section>
    </main>
  );
}

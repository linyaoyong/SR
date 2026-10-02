import { useState } from 'react';
import { Button, Form, Input, InputNumber, Alert, message } from 'antd';
import { Link } from 'react-router-dom';
import { adminService } from '../services/admin';

interface BanFormValues {
  userId: number;
  reason?: string;
}

interface UnbanFormValues {
  userId: number;
}

type OpResult = { tone: 'success' | 'error'; text: string };

// 封禁管理页：由于后端无 GET /api/admin/users 列表接口，
// 采用"按用户 ID 操作 + 查看操作日志"方案：两个独立表单分别封禁/解封，
// 操作结果 inline 展示，并指向 /logs 查看审计轨迹。
export default function BansPage() {
  const [banForm] = Form.useForm<BanFormValues>();
  const [unbanForm] = Form.useForm<UnbanFormValues>();
  const [banLoading, setBanLoading] = useState(false);
  const [unbanLoading, setUnbanLoading] = useState(false);
  const [result, setResult] = useState<OpResult | null>(null);

  const handleBan = async (values: BanFormValues) => {
    setBanLoading(true);
    try {
      await adminService.banUser(values.userId, values.reason?.trim() || undefined);
      message.success('已封禁用户');
      setResult({ tone: 'success', text: `已封禁用户 ${values.userId}` });
      banForm.resetFields();
    } catch (err) {
      const text = err instanceof Error ? err.message : '操作失败';
      message.error(text);
      setResult({ tone: 'error', text: `封禁用户 ${values.userId} 失败：${text}` });
    } finally {
      setBanLoading(false);
    }
  };

  const handleUnban = async (values: UnbanFormValues) => {
    setUnbanLoading(true);
    try {
      await adminService.unbanUser(values.userId);
      message.success('已解封用户');
      setResult({ tone: 'success', text: `已解封用户 ${values.userId}` });
      unbanForm.resetFields();
    } catch (err) {
      const text = err instanceof Error ? err.message : '操作失败';
      message.error(text);
      setResult({ tone: 'error', text: `解封用户 ${values.userId} 失败：${text}` });
    } finally {
      setUnbanLoading(false);
    }
  };

  return (
    <>
      <section className="admin-panel">
        <h2 className="admin-title" style={{ fontSize: 16 }}>
          封禁用户
        </h2>
        <Form<BanFormValues>
          form={banForm}
          layout="vertical"
          onFinish={handleBan}
          requiredMark={false}
          style={{ maxWidth: 480, marginTop: 12 }}
        >
          <Form.Item
            label="用户 ID"
            name="userId"
            rules={[{ required: true, message: '请输入用户 ID' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入用户 ID" />
          </Form.Item>
          <Form.Item
            label="封禁原因"
            name="reason"
            rules={[{ max: 255, message: '原因不超过 255 个字符' }]}
          >
            <Input.TextArea
              placeholder="请输入封禁原因（可选）"
              maxLength={255}
              autoSize={{ minRows: 3, maxRows: 6 }}
            />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={banLoading}
              className="admin-btn-danger"
            >
              确认封禁
            </Button>
          </Form.Item>
        </Form>
      </section>

      <section className="admin-panel">
        <h2 className="admin-title" style={{ fontSize: 16 }}>
          解封用户
        </h2>
        <Form<UnbanFormValues>
          form={unbanForm}
          layout="vertical"
          onFinish={handleUnban}
          requiredMark={false}
          style={{ maxWidth: 480, marginTop: 12 }}
        >
          <Form.Item
            label="用户 ID"
            name="userId"
            rules={[{ required: true, message: '请输入用户 ID' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入用户 ID" />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={unbanLoading}
              className="admin-btn-primary"
            >
              确认解封
            </Button>
          </Form.Item>
        </Form>
      </section>

      {result && (
        <Alert
          showIcon
          type={result.tone}
          message={result.text}
          className={
            result.tone === 'success' ? 'admin-alert--success' : 'admin-alert--error'
          }
        />
      )}

      <p className="admin-caption">
        查看历史操作轨迹请前往
        <Link to="/logs" style={{ marginLeft: 4 }}>
          操作日志
        </Link>
        。
      </p>
    </>
  );
}

import { useEffect, useState } from 'react';
import { Form, Input, Modal, message } from 'antd';
import { adminService } from '../services/admin';

interface BanUserDialogProps {
  userId: number | null;
  visible: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

// 封禁用户对话框：输入封禁原因（可选，max 255），调用 banUser(userId, reason)。
// userId 由外部传入（来自 BansPage 的输入或列表选择）。
export default function BanUserDialog({
  userId,
  visible,
  onClose,
  onSuccess,
}: BanUserDialogProps) {
  const [form] = Form.useForm<{ reason: string }>();
  const [loading, setLoading] = useState(false);

  // 对话框每次打开时重置原因字段
  useEffect(() => {
    if (visible) form.resetFields();
  }, [visible, form]);

  const submit = async () => {
    if (userId == null) return;
    let values: { reason?: string };
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setLoading(true);
    try {
      await adminService.banUser(userId, values.reason?.trim() || undefined);
      message.success('已封禁用户');
      onSuccess?.();
      onClose();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="封禁用户"
      open={visible}
      okText="确认封禁"
      cancelText="取消"
      confirmLoading={loading}
      destroyOnHidden
      onOk={submit}
      onCancel={onClose}
    >
      <p className="admin-caption" style={{ marginBottom: 12 }}>
        目标用户 ID：{userId ?? '-'}
      </p>
      <Form form={form} layout="vertical">
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
      </Form>
    </Modal>
  );
}

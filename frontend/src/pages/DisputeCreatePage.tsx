import { useCallback, useEffect, useState } from 'react';
import { Button, Form, Input, InputNumber, Spin, message } from 'antd';
import { ArrowLeft } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { rentalService } from '../services/rental';
import type { RentalOrder } from '../types/api';

interface DisputeFormValues {
  reason: string;
  description?: string;
  expectedDepositDeduction?: number;
  imageUrls?: string;
}

// 将逗号分隔的图片地址转为 JSON 数组字符串（对齐 docs/03-api-contract.md 第 11.4 节 imageUrls 字段）。
function parseImageUrls(raw?: string): string | undefined {
  if (!raw || !raw.trim()) return undefined;
  const urls = raw
    .split(/[,，\s]+/)
    .map((s) => s.trim())
    .filter(Boolean);
  return urls.length > 0 ? JSON.stringify(urls) : undefined;
}

// 异议创建页：展示订单上下文 + 异议表单。
// 字段校验对齐 docs/03-api-contract.md 第 11.4 节 DisputeCreateRequest。
export default function DisputeCreatePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const orderId = Number(id);

  const [order, setOrder] = useState<RentalOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<DisputeFormValues>();

  const loadOrder = useCallback(async () => {
    if (!orderId || Number.isNaN(orderId)) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await rentalService.orderDetail(orderId);
      setOrder(data);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载订单失败');
    } finally {
      setLoading(false);
    }
  }, [orderId]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  const handleSubmit = async (values: DisputeFormValues) => {
    setSubmitting(true);
    try {
      await rentalService.createDispute(orderId, {
        reason: values.reason,
        description: values.description,
        expectedDepositDeduction: values.expectedDepositDeduction,
        imageUrls: parseImageUrls(values.imageUrls),
      });
      message.success('异议已提交');
      navigate(`/orders/${orderId}`, { replace: true });
    } catch (err) {
      message.error(err instanceof Error ? err.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <main className="sr-page">
        <section className="sr-section sr-detail-loading">
          <Spin />
        </section>
      </main>
    );
  }

  return (
    <main className="sr-page">
      <section className="sr-section">
        <button
          type="button"
          className="sr-btn sr-btn-sm sr-btn-secondary sr-detail-back"
          onClick={() => navigate(-1)}
        >
          <ArrowLeft size={16} /> 返回
        </button>
        <div className="sr-card" style={{ padding: 'var(--space-5)' }}>
          <h1 className="sr-title">提交异议</h1>
          {order && (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
                gap: 'var(--space-3)',
                padding: 'var(--space-3) var(--space-4)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-card)',
                marginBottom: 'var(--space-5)',
                background: 'var(--color-surface)',
              }}
            >
              <div>
                <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>订单号</div>
                <div>{order.orderNo}</div>
              </div>
              <div>
                <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>物品 ID</div>
                <div>{order.itemId}</div>
              </div>
              <div>
                <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>租金总额</div>
                <div>¥{order.rentAmount}</div>
              </div>
              <div>
                <div style={{ color: 'var(--color-text-muted)', fontSize: 12 }}>押金金额</div>
                <div>¥{order.depositAmount}</div>
              </div>
            </div>
          )}
          <Form form={form} layout="vertical" onFinish={handleSubmit}>
            <Form.Item
              name="reason"
              label="异议原因"
              rules={[
                { required: true, message: '请输入异议原因' },
                { max: 128, message: '异议原因最长 128 字' },
              ]}
            >
              <Input maxLength={128} placeholder="简要说明异议原因" />
            </Form.Item>
            <Form.Item
              name="description"
              label="异议描述"
              rules={[{ max: 500, message: '描述最长 500 字' }]}
            >
              <Input.TextArea autoSize={{ minRows: 3 }} maxLength={500} placeholder="详细描述情况" />
            </Form.Item>
            <Form.Item
              name="expectedDepositDeduction"
              label="期望押金扣除金额"
              rules={[
                {
                  validator: (_, value) => {
                    if (value === undefined || value === null) return Promise.resolve();
                    if (value < 0) return Promise.reject(new Error('金额不能为负'));
                    return Promise.resolve();
                  },
                },
              ]}
            >
              <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="0.00" />
            </Form.Item>
            <Form.Item name="imageUrls" label="图片地址（可选，多个用逗号分隔）">
              <Input.TextArea
                autoSize={{ minRows: 2 }}
                placeholder="https://example.com/a.png, https://example.com/b.png"
              />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={submitting}>
                提交异议
              </Button>
              <Button
                style={{ marginLeft: 'var(--space-3)' }}
                onClick={() => navigate(`/orders/${orderId}`)}
              >
                取消
              </Button>
            </Form.Item>
          </Form>
        </div>
      </section>
    </main>
  );
}

import { Button, DatePicker, Form, Input, InputNumber, Modal, message } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useState } from 'react';
import { rentalService } from '../services/rental';
import type { RentalApplication, RentalProposalUpdateRequest } from '../types/api';

interface ProposalEditorProps {
  application: RentalApplication;
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}

// 表单内部值：把 LocalDateTime 字符串转成 Dayjs 便于 DatePicker 操作。
interface FormValues {
  quantity?: number;
  rentStartTime?: Dayjs;
  rentEndTime?: Dayjs;
  meetupTime?: Dayjs;
  meetupLocation?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  rentAmount?: number;
  depositAmount?: number;
  remark?: string;
}

const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

function toDayjs(value?: string): Dayjs | undefined {
  if (!value) return undefined;
  const d = dayjs(value);
  return d.isValid() ? d : undefined;
}

// 协商 proposal 编辑器：用于在 status=0 的申请上修改字段并提交新版本。
// 字段对齐 docs/03-api-contract.md 第 9.1 节 RentalProposalUpdateRequest。
export default function ProposalEditor({
  application,
  open,
  onClose,
  onSaved,
}: ProposalEditorProps) {
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const proposal = application.currentProposal;
  const watchedRentAmount = Form.useWatch('rentAmount', form);
  const watchedDepositAmount = Form.useWatch('depositAmount', form);
  // RentalApplicationCard 仅在 proposal 存在时渲染本组件，此处防御性兜底。
  if (!proposal) return null;

  const initialValues: FormValues = {
    quantity: proposal.quantity,
    rentStartTime: toDayjs(proposal.rentStartTime),
    rentEndTime: toDayjs(proposal.rentEndTime),
    meetupTime: toDayjs(proposal.meetupTime),
    meetupLocation: proposal.meetupLocation,
    receiverName: proposal.receiverName,
    receiverPhone: proposal.receiverPhone,
    receiverAddress: proposal.receiverAddress,
    rentAmount: proposal.rentAmount,
    depositAmount: proposal.depositAmount,
    remark: proposal.remark,
  };
  const expectedTotal = Number((
    Number(watchedRentAmount ?? proposal.rentAmount ?? 0)
    + Number(watchedDepositAmount ?? proposal.depositAmount ?? 0)
  ).toFixed(2));

  const handleFinish = async (values: FormValues) => {
    setSubmitting(true);
    try {
      const body: RentalProposalUpdateRequest = {
        quantity: values.quantity,
        rentStartTime: values.rentStartTime?.format(DATETIME_FORMAT),
        rentEndTime: values.rentEndTime?.format(DATETIME_FORMAT),
        meetupTime: values.meetupTime?.format(DATETIME_FORMAT),
        meetupLocation: values.meetupLocation,
        receiverName: values.receiverName,
        receiverPhone: values.receiverPhone,
        receiverAddress: values.receiverAddress,
        rentAmount: values.rentAmount,
        depositAmount: values.depositAmount,
        remark: values.remark,
      };
      await rentalService.updateProposal(application.id, body);
      message.success('已更新协商方案');
      onClose();
      onSaved();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="修改协商方案"
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnClose
      width={920}
      className="sr-apply-modal sr-proposal-modal"
    >
      <Form
        form={form}
        layout="vertical"
        className="sr-apply-form sr-proposal-form"
        initialValues={initialValues}
        onFinish={handleFinish}
        preserve={false}
      >
        <div className="sr-apply-form-grid">
          <Form.Item
            name="quantity"
            label="数量"
            rules={[{ required: true, message: '请输入数量' }, { type: 'number', min: 1, message: '数量至少为 1' }]}
          >
            <InputNumber min={1} precision={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="rentStartTime"
            label="租借开始时间"
            rules={[{ required: true, message: '请选择开始时间' }]}
          >
            <DatePicker showTime format={DATETIME_FORMAT} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="rentEndTime"
            label="租借结束时间"
            dependencies={['rentStartTime']}
            rules={[
              { required: true, message: '请选择结束时间' },
              {
                validator: (_, value: Dayjs) => {
                  const start = form.getFieldValue('rentStartTime');
                  if (!value || !start) return Promise.resolve();
                  if (value.isAfter(start)) return Promise.resolve();
                  return Promise.reject(new Error('结束时间必须晚于开始时间'));
                },
              },
            ]}
          >
            <DatePicker showTime format={DATETIME_FORMAT} style={{ width: '100%' }} />
          </Form.Item>
        </div>
        {proposal.deliveryType === 0 && (
          <div className="sr-apply-form-grid">
            <Form.Item name="meetupTime" label="面交时间">
              <DatePicker showTime format={DATETIME_FORMAT} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="meetupLocation" label="面交地点">
              <Input placeholder="如：学校南门" maxLength={100} />
            </Form.Item>
          </div>
        )}
        {proposal.deliveryType === 1 && (
          <div className="sr-apply-form-grid sr-apply-form-grid--address">
            <Form.Item name="receiverName" label="收件人姓名">
              <Input placeholder="收件人" maxLength={64} />
            </Form.Item>
            <Form.Item name="receiverPhone" label="收件人电话">
              <Input placeholder="联系电话" maxLength={20} />
            </Form.Item>
            <Form.Item name="receiverAddress" label="收件地址">
              <Input.TextArea placeholder="收件地址" maxLength={255} autoSize={{ minRows: 2 }} />
            </Form.Item>
          </div>
        )}
        <div className="sr-apply-money-grid">
          <Form.Item
            name="rentAmount"
            label="协商租金（元）"
            className="sr-money-field"
            rules={[{ type: 'number', min: 0, message: '租金不能为负' }]}
          >
            <InputNumber min={0} precision={2} prefix="¥" className="sr-money-input" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="depositAmount"
            label="协商押金（元）"
            className="sr-money-field"
            rules={[{ type: 'number', min: 0, message: '押金不能为负' }]}
          >
            <InputNumber min={0} precision={2} prefix="¥" className="sr-money-input" style={{ width: '100%' }} />
          </Form.Item>
          <div className="sr-money-summary">
            <div>
              <span className="sr-money-summary-label">协商总金额</span>
              <span className="sr-money-summary-note">协商租金 + 协商押金</span>
            </div>
            <span className="sr-money-total-value">¥{expectedTotal.toFixed(2)}</span>
          </div>
        </div>
        <Form.Item name="remark" label="备注">
          <Input.TextArea placeholder="可不填" maxLength={255} autoSize={{ minRows: 2 }} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting}>
            提交修改
          </Button>
        </Form.Item>
      </Form>
    </Modal>
  );
}

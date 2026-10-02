import { useEffect, useMemo, useState } from 'react';
import { Button, DatePicker, Form, Input, InputNumber, Modal, Select, Spin, message } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import RentalApplicationCard from './RentalApplicationCard';
import { itemService } from '../services/item';
import { rentalService } from '../services/rental';
import { userService } from '../services/user';
import type {
  ItemDetail,
  RentalApplication,
  RentalApplicationCreateRequest,
  UserPublicProfile,
} from '../types/api';

const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

interface ApplyFormValues {
  quantity: number;
  deliveryType: number;
  rentStartTime: Dayjs;
  rentEndTime: Dayjs;
  meetupTime?: Dayjs;
  meetupLocation?: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  rentAmount?: number;
  depositAmount?: number;
  remark?: string;
}

interface ApplyRentalModalProps {
  itemId: number;
  open: boolean;
  currentUserId?: number;
  onClose: () => void;
}

const toFiniteMoney = (...values: unknown[]): number => {
  for (const value of values) {
    if (value === undefined || value === null || value === '') continue;
    const numeric = Number(value);
    if (Number.isFinite(numeric)) return numeric;
  }
  return 0;
};

/**
 * 申请租借弹窗：可复用于商品详情页和消息对话页。
 * 内部按 itemId 拉取物品详情并渲染申请表单，提交后展示申请卡片。
 */
export default function ApplyRentalModal({ itemId, open, currentUserId, onClose }: ApplyRentalModalProps) {
  const [item, setItem] = useState<ItemDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [createdApplication, setCreatedApplication] = useState<RentalApplication | null>(null);
  const [summaryAmounts, setSummaryAmounts] = useState({ rentAmount: 0, depositAmount: 0 });
  const [ownerProfile, setOwnerProfile] = useState<UserPublicProfile | null>(null);
  const [applyForm] = Form.useForm<ApplyFormValues>();
  const watchedQuantity = Form.useWatch('quantity', applyForm);
  const watchedRentStartTime = Form.useWatch('rentStartTime', applyForm);
  const watchedRentEndTime = Form.useWatch('rentEndTime', applyForm);
  const watchedRentAmount = Form.useWatch('rentAmount', applyForm);
  const watchedDepositAmount = Form.useWatch('depositAmount', applyForm);
  const watchedDeliveryType = Form.useWatch('deliveryType', applyForm);

  useEffect(() => {
    if (!open || !itemId) return;
    setLoading(true);
    setCreatedApplication(null);
    setItem(null);
    setOwnerProfile(null);
    itemService
      .detail(itemId)
      .then((data) => setItem(data))
      .catch((err) => message.error(err instanceof Error ? err.message : '加载物品失败'))
      .finally(() => setLoading(false));
  }, [open, itemId]);

  useEffect(() => {
    if (!open || !item?.ownerId) return;
    let active = true;
    userService
      .publicProfile(item.ownerId)
      .then((profile) => {
        if (active) setOwnerProfile(profile);
      })
      .catch(() => {
        if (active) setOwnerProfile(null);
      });
    return () => {
      active = false;
    };
  }, [item?.ownerId, open]);

  const defaultDeliveryType = useMemo(() => {
    if (!item) return 0;
    if (item.supportDelivery === 1) return 1;
    return 0;
  }, [item]);

  const computeBillingDays = (start?: Dayjs, end?: Dayjs): number => {
    if (!item || !start || !end) return 0;
    const durationHours = Math.max(0, end.diff(start, 'hour', true));
    return Math.max(item.minRentDays, Math.ceil(durationHours / 24), 1);
  };

  const computeSuggestedRent = (quantity: number, start?: Dayjs, end?: Dayjs): number => {
    if (!item || !start || !end) return 0;
    const days = computeBillingDays(start, end);
    return Number((item.dailyPrice * quantity * days).toFixed(2));
  };

  useEffect(() => {
    if (!item || !open || createdApplication) return;
    const baseTime = dayjs();
    const rentStartTime = baseTime.add(1, 'day');
    const rentEndTime = baseTime.add(2, 'day');
    const defaultRentAmount = computeSuggestedRent(1, rentStartTime, rentEndTime);
    applyForm.setFieldsValue({
      quantity: 1,
      deliveryType: defaultDeliveryType,
      rentStartTime,
      rentEndTime,
      rentAmount: defaultRentAmount,
      depositAmount: item.depositAmount,
      meetupLocation: defaultDeliveryType === 0 ? item.meetupLocation : undefined,
    });
    setSummaryAmounts({ rentAmount: defaultRentAmount, depositAmount: item.depositAmount });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, createdApplication, item?.id, defaultDeliveryType]);

  useEffect(() => {
    if (!item || !open || createdApplication) return;
    const quantity = watchedQuantity ?? 1;
    if (!watchedRentStartTime || !watchedRentEndTime) return;
    const nextRentAmount = computeSuggestedRent(quantity, watchedRentStartTime, watchedRentEndTime);
    applyForm.setFieldValue('rentAmount', nextRentAmount);
    setSummaryAmounts((prev) => ({ ...prev, rentAmount: nextRentAmount }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, createdApplication, item?.id, watchedQuantity, watchedRentStartTime, watchedRentEndTime]);

  useEffect(() => {
    if (!item || !open || createdApplication) return;
    const rentAmount = toFiniteMoney(watchedRentAmount, applyForm.getFieldValue('rentAmount'), summaryAmounts.rentAmount);
    const depositAmount = toFiniteMoney(watchedDepositAmount, applyForm.getFieldValue('depositAmount'), item.depositAmount);
    setSummaryAmounts((prev) =>
      prev.rentAmount === rentAmount && prev.depositAmount === depositAmount
        ? prev
        : { rentAmount, depositAmount },
    );
  }, [
    open,
    createdApplication,
    item,
    watchedRentAmount,
    watchedDepositAmount,
    applyForm,
    summaryAmounts.rentAmount,
  ]);

  useEffect(() => {
    if (!item || !open || createdApplication || watchedDeliveryType !== 0 || !item.meetupLocation) return;
    if (!applyForm.getFieldValue('meetupLocation')) {
      applyForm.setFieldValue('meetupLocation', item.meetupLocation);
    }
  }, [applyForm, createdApplication, item, open, watchedDeliveryType]);

  const defaultRentAmount = item ? Number((item.dailyPrice * Math.max(item.minRentDays, 1)).toFixed(2)) : 0;
  const rentAmount = toFiniteMoney(
    watchedRentAmount,
    applyForm.getFieldValue('rentAmount'),
    summaryAmounts.rentAmount > 0 ? summaryAmounts.rentAmount : undefined,
    defaultRentAmount,
  );
  const depositAmount = toFiniteMoney(
    watchedDepositAmount,
    applyForm.getFieldValue('depositAmount'),
    summaryAmounts.depositAmount > 0 ? summaryAmounts.depositAmount : undefined,
    item?.depositAmount,
  );
  const expectedTotal = Number((rentAmount + depositAmount).toFixed(2));
  const availableQuantity = item ? Math.max(0, item.quantity - (item.rentedCount ?? 0)) : 0;

  const handleSubmit = async (values: ApplyFormValues) => {
    if (!item) return;
    setSubmitting(true);
    try {
      const body: RentalApplicationCreateRequest = {
        itemId: item.id,
        quantity: values.quantity,
        rentStartTime: values.rentStartTime.format(DATETIME_FORMAT),
        rentEndTime: values.rentEndTime.format(DATETIME_FORMAT),
        deliveryType: values.deliveryType,
        meetupTime: values.meetupTime?.format(DATETIME_FORMAT),
        meetupLocation: values.deliveryType === 0
          ? values.meetupLocation || item.meetupLocation
          : values.meetupLocation,
        receiverName: values.receiverName,
        receiverPhone: values.receiverPhone,
        receiverAddress: values.receiverAddress,
        rentAmount: values.rentAmount,
        depositAmount: values.depositAmount,
        remark: values.remark,
      };
      const created = await rentalService.createApplication(body);
      message.success('已创建租借申请');
      setCreatedApplication(created);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    } finally {
      setSubmitting(false);
    }
  };

  const refreshApplication = async () => {
    if (!createdApplication) return;
    try {
      const updated = await rentalService.applicationDetail(createdApplication.id);
      setCreatedApplication(updated);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '加载失败');
    }
  };

  const handleCancel = () => {
    applyForm.resetFields();
    setSummaryAmounts({ rentAmount: 0, depositAmount: 0 });
    onClose();
  };

  return (
    <Modal
      title="申请租借"
      open={open}
      onCancel={handleCancel}
      footer={null}
      width={920}
      className="sr-apply-modal"
      destroyOnHidden
    >
      <div className="sr-apply-modal-body">
        {loading ? (
          <div className="sr-apply-modal-loading"><Spin /></div>
        ) : !item ? (
          <div className="sr-empty"><p>物品不存在</p></div>
        ) : availableQuantity <= 0 ? (
          <div className="sr-empty"><p>库存不足，暂时无法申请租借</p></div>
        ) : createdApplication ? (
          <RentalApplicationCard
            application={createdApplication}
            currentUserId={currentUserId}
            item={item}
            counterparty={ownerProfile ?? undefined}
            counterpartyLabel="物主"
            onChanged={refreshApplication}
          />
        ) : (
          <Form<ApplyFormValues>
            form={applyForm}
            layout="vertical"
            className="sr-apply-form"
            initialValues={{
              quantity: 1,
              deliveryType: defaultDeliveryType,
              rentStartTime: dayjs().add(1, 'day'),
              rentEndTime: dayjs().add(2, 'day'),
	              rentAmount: computeSuggestedRent(1, dayjs().add(1, 'day'), dayjs().add(2, 'day')),
	              depositAmount: item.depositAmount,
	              meetupLocation: defaultDeliveryType === 0 ? item.meetupLocation : undefined,
	            }}
            onFinish={handleSubmit}
          >
            <div className="sr-apply-form-grid">
              <Form.Item
                name="quantity"
                label="数量"
                rules={[
                  { required: true, message: '请输入数量' },
                  { type: 'number', min: 1, max: availableQuantity, message: `数量 1-${availableQuantity}` },
                ]}
              >
                <InputNumber min={1} max={availableQuantity} precision={0} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item
                name="deliveryType"
                label="交付方式"
                rules={[{ required: true, message: '请选择交付方式' }]}
              >
                <Select
                  options={[
                    { label: '快递', value: 1, disabled: item.supportDelivery !== 1 },
                    { label: '面交', value: 0, disabled: item.supportMeetup !== 1 },
                  ]}
                />
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
                      const start = applyForm.getFieldValue('rentStartTime');
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
            <Form.Item noStyle shouldUpdate={(prev, cur) => prev.deliveryType !== cur.deliveryType}>
              {({ getFieldValue }) =>
                getFieldValue('deliveryType') === 0 ? (
                  <div className="sr-apply-form-grid">
                    <Form.Item name="meetupTime" label="面交时间">
                      <DatePicker showTime format={DATETIME_FORMAT} style={{ width: '100%' }} />
                    </Form.Item>
                    <Form.Item name="meetupLocation" label="面交地点">
                      <Input placeholder={item.meetupLocation || '如：学校南门'} maxLength={100} />
                    </Form.Item>
                  </div>
                ) : (
                  <div className="sr-apply-form-grid sr-apply-form-grid--address">
                    <Form.Item name="receiverName" label="收件人姓名" rules={[{ required: true, message: '请输入收件人' }]}>
                      <Input placeholder="收件人" maxLength={64} />
                    </Form.Item>
                    <Form.Item name="receiverPhone" label="收件人电话" rules={[{ required: true, message: '请输入电话' }]}>
                      <Input placeholder="联系电话" maxLength={20} />
                    </Form.Item>
                    <Form.Item name="receiverAddress" label="收件地址" rules={[{ required: true, message: '请输入收件地址' }]}>
                      <Input.TextArea placeholder="收件地址" maxLength={255} autoSize={{ minRows: 2 }} />
                    </Form.Item>
                  </div>
                )
              }
            </Form.Item>
            <div className="sr-apply-money-grid">
              <Form.Item
                name="rentAmount"
                label="协商租金（元）"
                className="sr-money-field"
                rules={[{ type: 'number', min: 0, message: '租金不能为负' }]}
                extra="默认按日租金 × 数量 × 天数计算"
              >
                <InputNumber
                  min={0}
                  precision={2}
                  prefix="¥"
                  className="sr-money-input"
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <Form.Item
                name="depositAmount"
                label="协商押金（元）"
                className="sr-money-field"
                rules={[{ type: 'number', min: 0, message: '押金不能为负' }]}
              >
                <InputNumber
                  min={0}
                  precision={2}
                  prefix="¥"
                  className="sr-money-input"
                  style={{ width: '100%' }}
                />
              </Form.Item>
              <div className="sr-money-summary">
                <div>
                  <span className="sr-money-summary-label">预计总金额</span>
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
                提交申请
              </Button>
            </Form.Item>
          </Form>
        )}
      </div>
    </Modal>
  );
}

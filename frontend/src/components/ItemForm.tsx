import { Button, Form, Input, InputNumber, Select, Switch } from 'antd';
import { Plus, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import type { Category } from '../types/api';
import type { ItemFormPayload } from '../services/item';

// priceType 在 docs/03-api-contract.md 中仅声明为 int 且无枚举值，系统核心计费字段为 dailyPrice，
// 此处仅提供"按天计费(0)"这一文档可追溯的选项，避免臆造未契约化的取值。
const PRICE_TYPE_OPTIONS = [{ label: '按天计费', value: 0 }];

interface ItemFormProps {
  initialValues?: Partial<ItemFormPayload>;
  categories: Category[];
  onSubmit: (values: ItemFormPayload) => Promise<void>;
  submitting: boolean;
  mode: 'create' | 'edit';
}

// Switch 在表单中以 0/1 存储，这里做 boolean 与 number 的双向转换。
const switchProps = {
  valuePropName: 'checked',
  getValueFromEvent: (checked: boolean) => (checked ? 1 : 0),
  getValueProps: (value: unknown) => ({ checked: value === 1 }),
};

function parseTags(value?: string) {
  return (value ?? '')
    .split(/\s+/)
    .map((tag) => tag.trim())
    .filter(Boolean);
}

function serializeTags(tags: string[]) {
  return tags.join(' ');
}

export default function ItemForm({
  initialValues,
  categories,
  onSubmit,
  submitting,
  mode,
}: ItemFormProps) {
  const [form] = Form.useForm<ItemFormPayload>();
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>(() => parseTags(initialValues?.tags));

  const supportDelivery = Form.useWatch('supportDelivery', form);
  const supportMeetup = Form.useWatch('supportMeetup', form);
  const depositEnabled = Form.useWatch('depositEnabled', form);
  const creditDepositEnabled = Form.useWatch('creditDepositEnabled', form);

  // 编辑模式载入 initialValues 后，触发一次校验以反映"交付方式"等联动状态。
  useEffect(() => {
    if (initialValues) {
      const initialTags = parseTags(initialValues.tags);
      setTags(initialTags);
      form.setFieldValue('tags', serializeTags(initialTags));
      form.validateFields(['supportMeetup']).catch(() => undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialValues]);

  const handleFinish = async (values: ItemFormPayload) => {
    const tagsText = serializeTags(tags);
    await onSubmit({
      ...values,
      tags: tagsText || undefined,
    });
  };

  const updateTags = (nextTags: string[]) => {
    setTags(nextTags);
    form.setFieldValue('tags', serializeTags(nextTags));
  };

  const addTag = () => {
    const nextTag = tagInput.trim();
    if (!nextTag || tags.includes(nextTag)) return;
    updateTags([...tags, nextTag]);
    setTagInput('');
  };

  const removeTag = (tagToRemove: string) => {
    updateTags(tags.filter((tag) => tag !== tagToRemove));
  };

  return (
    <Form
      form={form}
      layout="vertical"
      initialValues={{
        quantity: 1,
        supportDelivery: 0,
        supportMeetup: 1,
        priceType: 0,
        dailyPrice: 0,
        minRentDays: 1,
        depositEnabled: 0,
        creditDepositEnabled: 0,
        ...initialValues,
      }}
      onFinish={handleFinish}
      className="sr-item-form"
      requiredMark="optional"
    >
      <div className="sr-item-form-section">
        <h2 className="sr-item-form-heading">基本信息</h2>
        <Form.Item
          name="title"
          label="标题"
          rules={[
            { required: true, message: '请输入标题' },
            { min: 2, max: 60, message: '标题长度 2-60 个字符' },
          ]}
        >
          <Input placeholder="2-60 个字符" maxLength={60} showCount />
        </Form.Item>
        <Form.Item
          name="description"
          label="描述"
          rules={[
            { required: true, message: '请输入描述' },
            { min: 1, max: 2000, message: '描述长度 1-2000 个字符' },
          ]}
        >
          <Input.TextArea placeholder="介绍物品状况、使用注意等" maxLength={2000} showCount autoSize={{ minRows: 4 }} />
        </Form.Item>
        <Form.Item
          name="categoryId"
          label="分类"
          rules={[{ required: true, message: '请选择分类' }]}
        >
          <Select
            placeholder="选择分类"
            optionFilterProp="label"
            options={categories
              .filter((c) => c.status !== 1)
              .map((c) => ({ label: c.name, value: c.id }))}
          />
        </Form.Item>
        <Form.Item name="tags" hidden>
          <Input />
        </Form.Item>
        <Form.Item label="标签">
          <div>
            <div className="sr-tag-input-row">
              <Input
                aria-label="标签"
                value={tagInput}
                placeholder="输入标签后点击加号"
                maxLength={30}
                onChange={(event) => setTagInput(event.target.value)}
                onPressEnter={(event) => {
                  event.preventDefault();
                  addTag();
                }}
              />
              <Button
                aria-label="添加标签"
                icon={<Plus size={16} />}
                onClick={addTag}
                disabled={!tagInput.trim()}
              />
            </div>
            <div className="sr-tag-list" aria-label="已添加标签">
              {tags.map((tag) => (
                <span className="sr-tag-chip" key={tag}>
                  <span>{tag}</span>
                  <button
                    type="button"
                    aria-label={`删除标签 ${tag}`}
                    className="sr-tag-remove"
                    onClick={() => removeTag(tag)}
                  >
                    <X size={13} />
                  </button>
                </span>
              ))}
            </div>
          </div>
        </Form.Item>
        <Form.Item
          name="quantity"
          label="库存"
          rules={[
            { required: true, message: '请输入库存' },
            { type: 'integer', min: 1, message: '库存至少为 1' },
          ]}
        >
          <InputNumber min={1} precision={0} style={{ width: '100%' }} />
        </Form.Item>
      </div>

      <div className="sr-item-form-section">
        <h2 className="sr-item-form-heading">价格与租期</h2>
        <Form.Item name="priceType" label="计费方式">
          <Select options={PRICE_TYPE_OPTIONS} />
        </Form.Item>
        <Form.Item
          name="dailyPrice"
          label="日租金（元）"
          className="sr-money-field"
          rules={[
            { required: true, message: '请输入日租金' },
            { type: 'number', min: 0, message: '日租金不能为负' },
          ]}
        >
          <InputNumber min={0} precision={2} prefix="¥" className="sr-money-input" style={{ width: '100%' }} placeholder="0 表示免费" />
        </Form.Item>
        <Form.Item
          name="minRentDays"
          label="最少租用天数"
          rules={[
            { required: true, message: '请输入最少租用天数' },
            { type: 'integer', min: 1, message: '最少租用天数为 1' },
          ]}
        >
          <InputNumber min={1} precision={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          name="freeRent"
          label="免租天数"
          rules={[{ type: 'integer', min: 0, message: '免租天数不能为负' }]}
        >
          <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="可不填" />
        </Form.Item>
      </div>

      <div className="sr-item-form-section">
        <h2 className="sr-item-form-heading">交付方式</h2>
        <Form.Item
          {...switchProps}
          name="supportDelivery"
          label="支持快递"
        >
          <Switch />
        </Form.Item>
        {supportDelivery === 1 && (
          <Form.Item name="deliveryCity" label="快递城市">
            <Input placeholder="如：北京，可不填表示不限" maxLength={50} />
          </Form.Item>
        )}
        <Form.Item
          {...switchProps}
          name="supportMeetup"
          label="支持面交"
          dependencies={['supportDelivery']}
          rules={[
            {
              validator: () => {
                const delivery = form.getFieldValue('supportDelivery');
                const meetup = form.getFieldValue('supportMeetup');
                if (delivery === 1 || meetup === 1) return Promise.resolve();
                return Promise.reject(new Error('快递和面交至少选择一项'));
              },
            },
          ]}
        >
          <Switch />
        </Form.Item>
        {supportMeetup === 1 && (
          <Form.Item name="meetupLocation" label="面交地点">
            <Input placeholder="如：学校南门，可不填" maxLength={100} />
          </Form.Item>
        )}
      </div>

      <div className="sr-item-form-section">
        <h2 className="sr-item-form-heading">押金</h2>
        <Form.Item {...switchProps} name="depositEnabled" label="启用押金">
          <Switch />
        </Form.Item>
        {depositEnabled === 1 && (
          <Form.Item
            name="depositAmount"
            label="押金金额（元）"
            className="sr-money-field"
            rules={[{ type: 'number', min: 0, message: '押金不能为负' }]}
          >
            <InputNumber min={0} precision={2} prefix="¥" className="sr-money-input" style={{ width: '100%' }} />
          </Form.Item>
        )}
        <Form.Item
          {...switchProps}
          name="creditDepositEnabled"
          label="启用信用免押"
        >
          <Switch />
        </Form.Item>
        {creditDepositEnabled === 1 && (
          <>
            <Form.Item
              name="minCreditScore"
              label="免押最低信用分"
              rules={[{ type: 'integer', min: 0, message: '不能为负' }]}
            >
              <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="如 600" />
            </Form.Item>
            <Form.Item
              name="freeDepositScore"
              label="全免信用分阈值"
              rules={[{ type: 'integer', min: 0, message: '不能为负' }]}
            >
              <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="如 800" />
            </Form.Item>
            <Form.Item
              name="reducedDepositScore"
              label="减押信用分阈值"
              rules={[{ type: 'integer', min: 0, message: '不能为负' }]}
            >
              <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="如 700" />
            </Form.Item>
            <Form.Item
              name="reducedDepositAmount"
              label="减押后金额（元）"
              className="sr-money-field"
              rules={[{ type: 'number', min: 0, message: '不能为负' }]}
            >
              <InputNumber min={0} precision={2} prefix="¥" className="sr-money-input" style={{ width: '100%' }} />
            </Form.Item>
          </>
        )}
      </div>

      <div className="sr-actions">
        <Button type="primary" htmlType="submit" loading={submitting}>
          {mode === 'create' ? '发布物品' : '保存修改'}
        </Button>
      </div>
    </Form>
  );
}

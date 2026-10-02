import { useEffect, useRef, useState } from 'react';
import { Form, Input, Modal, Rate, message } from 'antd';
import { ImageIcon, Plus, X } from 'lucide-react';
import { rentalService } from '../services/rental';
import type { Review } from '../types/api';

interface ReviewDialogProps {
  open: boolean;
  orderId: number;
  currentUserId?: number;
  onClose: () => void;
  onReviewed?: () => void;
}

interface ReviewFormValues {
  rating: number;
  content?: string;
  imageUrls?: string;
}

function parseStoredImageUrls(raw?: string): string[] {
  if (!raw || !raw.trim()) return [];
  const trimmed = raw.trim();
  if (trimmed.startsWith('[')) {
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) {
        return parsed.map((url) => String(url).trim()).filter(Boolean);
      }
    } catch {
      // Fall back to delimiter parsing for legacy values.
    }
  }
  return trimmed
    .split(/[,，\s]+/)
    .map((url) => url.trim())
    .filter(Boolean);
}

function stringifyImageUrls(urls: string[]): string | undefined {
  return urls.length > 0 ? JSON.stringify(urls) : undefined;
}

// 评价弹窗：评分 1-5 + 可选内容 + 本地图片上传。
// 字段校验对齐 docs/03-api-contract.md 第 11.3 节 ReviewCreateRequest。
export default function ReviewDialog({
  open,
  orderId,
  currentUserId,
  onClose,
  onReviewed,
}: ReviewDialogProps) {
  const [form] = Form.useForm<ReviewFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const [existingReview, setExistingReview] = useState<Review | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (open) {
      form.resetFields();
      setImageUrls([]);
      setExistingReview(null);
      rentalService
        .reviews(orderId)
        .then((reviews) => {
          const ownReview = currentUserId
            ? reviews.find((review) => review.reviewerId === currentUserId)
            : undefined;
          if (!ownReview) return;
          setExistingReview(ownReview);
          form.setFieldsValue({
            rating: ownReview.rating,
            content: ownReview.content,
          });
          setImageUrls(parseStoredImageUrls(ownReview.imageUrls));
        })
        .catch(() => undefined);
    }
  }, [currentUserId, form, open, orderId]);

  const handleImageSelected = async (file?: File) => {
    if (!file) return;
    setUploading(true);
    try {
      const uploaded = await rentalService.uploadReviewImage(orderId, file);
      setImageUrls((current) => [...current, uploaded.url]);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '图片上传失败');
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      await rentalService.createReview(orderId, {
        rating: values.rating,
        content: values.content,
        imageUrls: stringifyImageUrls(imageUrls),
      });
      message.success(existingReview ? '评价已更新' : '评价已提交');
      onReviewed?.();
      onClose();
    } catch (err) {
      // 表单校验失败不弹 message，仅业务错误提示。
      if (err instanceof Error) {
        message.error(err.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="评价订单"
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={submitting}
      okText={existingReview ? '更新评价' : '提交评价'}
      okButtonProps={{ disabled: uploading }}
      cancelText="取消"
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="rating"
          label="评分"
          rules={[{ required: true, message: '请选择评分' }]}
        >
          <Rate count={5} />
        </Form.Item>
        <Form.Item
          name="content"
          label="评价内容（可选）"
          rules={[{ max: 500, message: '评价内容最长 500 字' }]}
        >
          <Input.TextArea
            autoSize={{ minRows: 3 }}
            maxLength={500}
            placeholder="说说你的租借体验"
          />
        </Form.Item>
        <div className="sr-review-upload">
          <div className="sr-review-upload-label">评价图片（可选）</div>
          <div className="sr-image-uploader-grid">
            {imageUrls.map((url) => (
              <div className="sr-image-uploader-item" key={url}>
                <img src={url} alt="评价图片预览" />
                <button
                  type="button"
                  className="sr-image-uploader-remove"
                  aria-label="移除图片"
                  onClick={() => setImageUrls((current) => current.filter((item) => item !== url))}
                >
                  <X size={14} />
                </button>
              </div>
            ))}
            {imageUrls.length < 6 && (
              <button
                type="button"
                className="sr-image-uploader-add"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
              >
                {uploading ? <ImageIcon size={22} /> : <Plus size={22} />}
                <span>{uploading ? '上传中' : '添加图片'}</span>
              </button>
            )}
          </div>
          <input
            ref={fileInputRef}
            className="sr-visually-hidden"
            type="file"
            accept="image/*"
            aria-label="选择评价图片"
            onChange={(event) => handleImageSelected(event.target.files?.[0])}
            disabled={uploading}
          />
        </div>
      </Form>
    </Modal>
  );
}

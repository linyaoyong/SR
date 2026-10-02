import { useCallback, useEffect, useState } from 'react';
import { Button, Spin, message } from 'antd';
import { ArrowLeft } from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import ImageUploader from '../components/ImageUploader';
import ItemForm from '../components/ItemForm';
import { authService } from '../services/auth';
import { itemMutations, itemService, type ItemFormPayload } from '../services/item';
import { useAuthStore } from '../stores/authStore';
import type { Category, ItemDetail } from '../types/api';

interface ItemEditPageProps {
  mode: 'create' | 'edit';
}

const PAGE_TITLE = { create: '发布物品', edit: '编辑物品' };

export default function ItemEditPage({ mode }: ItemEditPageProps) {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.currentUser);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

  const [categories, setCategories] = useState<Category[]>([]);
  const [item, setItem] = useState<ItemDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);

  const isEdit = mode === 'edit';
  const itemId = isEdit ? Number(id) : undefined;

  const loadCategories = useCallback(async () => {
    try {
      const data = await itemService.categories();
      setCategories(data);
    } catch {
      setCategories([]);
    }
  }, []);

  // 确保当前用户资料已加载（用于编辑模式下的物主校验）。
  useEffect(() => {
    if (!useAuthStore.getState().accessToken) return;
    if (!currentUser) {
      authService.me().then(setCurrentUser).catch(() => undefined);
    }
  }, [currentUser, setCurrentUser]);

  useEffect(() => {
    let active = true;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        await loadCategories();
        if (isEdit) {
          if (!itemId || Number.isNaN(itemId)) {
            if (active) setError('物品不存在');
            return;
          }
          const detail = await itemService.detail(itemId);
          if (!active) return;
          setItem(detail);
          // 物主校验由下方独立 effect 处理（currentUser 可能晚于 detail 到达）。
        }
      } catch (err) {
        if (active) setError(err instanceof Error ? err.message : '加载失败');
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, [isEdit, itemId, loadCategories]);

  // 物主身份确认后再次校验（currentUser 可能晚于 detail 到达）。
  useEffect(() => {
    if (isEdit && item && currentUser && item.ownerId !== currentUser.id) {
      setError('无权编辑他人的物品');
    }
  }, [isEdit, item, currentUser]);

  const handleSubmit = async (values: ItemFormPayload) => {
    setSubmitting(true);
    try {
      if (isEdit && itemId) {
        await itemMutations.update(itemId, values);
        message.success('保存成功');
        navigate(`/items/${itemId}`, { replace: true });
      } else {
        const created = await itemMutations.create(values);
        if (pendingFiles.length > 0) {
          try {
            await itemMutations.uploadImages(created.id, pendingFiles);
          } catch {
            message.warning('物品已发布，但部分图片上传失败，可在编辑页重新上传');
          }
        }
        message.success('发布成功');
        navigate(`/items/${created.id}`, { replace: true });
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
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

  if (error) {
    return (
      <main className="sr-page">
        <section className="sr-section">
          <div className="sr-empty">
            <p>{error}</p>
            <Button onClick={() => navigate(-1)}>返回</Button>
          </div>
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
        <div className="sr-card">
          <h1 className="sr-title">{PAGE_TITLE[mode]}</h1>
          <div className="sr-item-form-section" style={{ marginBottom: 'var(--space-5)' }}>
            <h2 className="sr-item-form-heading">物品图片</h2>
            <ImageUploader
              itemId={itemId}
              existingImages={isEdit && item ? item.images : []}
              maxCount={9}
              onPendingFilesChange={setPendingFiles}
            />
          </div>
          <ItemForm
            mode={mode}
            categories={categories}
            initialValues={isEdit && item ? mapDetailToPayload(item) : undefined}
            onSubmit={handleSubmit}
            submitting={submitting}
          />
        </div>
      </section>
    </main>
  );
}

// 将 ItemDetail 映射为表单初始值。ItemDetail 是 ItemFormPayload 的超集，直接裁剪即可。
function mapDetailToPayload(detail: ItemDetail): Partial<ItemFormPayload> {
  return {
    title: detail.title,
    description: detail.description,
    categoryId: detail.categoryId,
    tags: detail.tags,
    quantity: detail.quantity,
    supportDelivery: detail.supportDelivery,
    deliveryCity: detail.deliveryCity,
    supportMeetup: detail.supportMeetup,
    meetupLocation: detail.meetupLocation,
    priceType: detail.priceType,
    dailyPrice: detail.dailyPrice,
    minRentDays: detail.minRentDays,
    freeRent: detail.freeRent,
    depositEnabled: detail.depositEnabled,
    depositAmount: detail.depositAmount,
    creditDepositEnabled: detail.creditDepositEnabled,
    minCreditScore: detail.minCreditScore,
    freeDepositScore: detail.freeDepositScore,
    reducedDepositScore: detail.reducedDepositScore,
    reducedDepositAmount: detail.reducedDepositAmount,
  };
}

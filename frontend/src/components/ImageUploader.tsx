import { Plus, X } from 'lucide-react';
import { message } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { itemMutations } from '../services/item';
import type { ItemImage } from '../types/api';

// 接受的图片类型与单文件大小上限。后端会再次压缩，前端先做基本校验。
const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const ACCEPTED_EXT = ['.jpg', '.jpeg', '.png', '.webp'];
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

interface ImageUploaderProps {
  // 编辑模式传入物品 id：选择文件后立即上传。创建模式不传：文件暂存，由父组件在创建后统一上传。
  itemId?: number;
  existingImages?: ItemImage[];
  maxCount?: number;
  // 创建模式下，待上传文件变化时回调父组件。
  onPendingFilesChange?: (files: File[]) => void;
}

interface PreviewItem {
  uid: string;
  file?: File;
  url: string;
  uploading: boolean;
  isExisting: boolean;
}

function buildExistingPreviews(images: ItemImage[]): PreviewItem[] {
  return images.map((img) => ({
    uid: `existing-${img.id}`,
    url: img.url,
    uploading: false,
    isExisting: true,
  }));
}

export default function ImageUploader({
  itemId,
  existingImages = [],
  maxCount = 9,
  onPendingFilesChange,
}: ImageUploaderProps) {
  const isEditMode = typeof itemId === 'number';
  const [previews, setPreviews] = useState<PreviewItem[]>(() =>
    buildExistingPreviews(existingImages),
  );
  // 用 ref 跟踪所有已创建的 object URL，确保卸载时能拿到最新列表进行回收。
  const objectUrlsRef = useRef<string[]>([]);
  const inputRef = useRef<HTMLInputElement>(null);

  // 卸载时清理所有 object URL，避免内存泄漏。
  useEffect(() => {
    return () => {
      objectUrlsRef.current.forEach((url) => URL.revokeObjectURL(url));
      objectUrlsRef.current = [];
    };
  }, []);

  const emitPending = (next: PreviewItem[]) => {
    if (!isEditMode && onPendingFilesChange) {
      const pending = next
        .filter((p) => !p.isExisting && p.file)
        .map((p) => p.file!) as File[];
      onPendingFilesChange(pending);
    }
  };

  const validateFile = (file: File): string | null => {
    const lowerName = file.name.toLowerCase();
    const extOk = ACCEPTED_EXT.some((ext) => lowerName.endsWith(ext));
    const typeOk = ACCEPTED_TYPES.includes(file.type);
    if (!extOk && !typeOk) {
      return '仅支持 jpg/jpeg/png/webp 格式';
    }
    if (file.size > MAX_FILE_SIZE) {
      return '单张图片不能超过 5MB';
    }
    return null;
  };

  const handleFiles = async (fileList: FileList | null) => {
    if (!fileList || fileList.length === 0) return;
    const remaining = maxCount - previews.length;
    if (remaining <= 0) {
      message.warning(`最多 ${maxCount} 张图片`);
      return;
    }

    const files = Array.from(fileList).slice(0, remaining);
    const validFiles: File[] = [];
    for (const file of files) {
      const err = validateFile(file);
      if (err) {
        message.error(`${file.name}：${err}`);
        continue;
      }
      validFiles.push(file);
    }
    if (validFiles.length === 0) return;

    // 创建本地预览
    const newPreviews: PreviewItem[] = validFiles.map((file) => {
      const url = URL.createObjectURL(file);
      objectUrlsRef.current.push(url);
      return {
        uid: `pending-${file.name}-${file.size}-${Date.now()}-${Math.random()}`,
        file,
        url,
        uploading: isEditMode,
        isExisting: false,
      };
    });

    const next = [...previews, ...newPreviews];
    setPreviews(next);
    emitPending(next);

    if (inputRef.current) inputRef.current.value = '';

    // 编辑模式：立即上传
    if (isEditMode && itemId != null) {
      try {
        const uploaded = await itemMutations.uploadImages(itemId, validFiles);
        setPreviews((curr) => {
          const updated = curr.map((p) => {
            const match = newPreviews.find((np) => np.uid === p.uid);
            if (match) {
              // 上传成功后视为已存在图片：隐藏删除按钮（无删除接口），与提示文案一致。
              return { ...p, uploading: false, isExisting: true };
            }
            return p;
          });
          emitPending(updated);
          return updated;
        });
        // 后端返回的是全部图片列表，这里仅作成功提示，预览仍用本地缩略图避免重复。
        message.success(`已上传 ${validFiles.length} 张图片`);
        void uploaded;
      } catch (err) {
        message.error(err instanceof Error ? err.message : '图片上传失败');
        // 上传失败的从预览中移除，并回收对应的 object URL
        setPreviews((curr) => {
          curr
            .filter((p) => newPreviews.some((np) => np.uid === p.uid))
            .forEach((p) => {
              URL.revokeObjectURL(p.url);
              objectUrlsRef.current = objectUrlsRef.current.filter((url) => url !== p.url);
            });
          const filtered = curr.filter((p) => !newPreviews.some((np) => np.uid === p.uid));
          emitPending(filtered);
          return filtered;
        });
      }
    }
  };

  const handleRemove = (uid: string) => {
    setPreviews((curr) => {
      const target = curr.find((p) => p.uid === uid);
      if (target && !target.isExisting && target.file) {
        // 仅创建模式的待上传文件可移除（编辑模式无删除接口）。
        URL.revokeObjectURL(target.url);
        objectUrlsRef.current = objectUrlsRef.current.filter((url) => url !== target.url);
      }
      const next = curr.filter((p) => p.uid !== uid);
      emitPending(next);
      return next;
    });
  };

  const canAdd = previews.length < maxCount;

  return (
    <div className="sr-image-uploader">
      <div className="sr-image-uploader-grid">
        {previews.map((preview) => (
          <div key={preview.uid} className="sr-image-uploader-item">
            <img src={preview.url} alt="物品图片预览" />
            {preview.uploading && <div className="sr-image-uploader-mask">上传中…</div>}
            {!preview.isExisting && !preview.uploading && (
              <button
                type="button"
                className="sr-image-uploader-remove"
                onClick={() => handleRemove(preview.uid)}
                aria-label="移除图片"
              >
                <X size={14} />
              </button>
            )}
            {preview.isExisting && (
              <span className="sr-image-uploader-badge">已上传</span>
            )}
          </div>
        ))}
        {canAdd && (
          <button
            type="button"
            className="sr-image-uploader-add"
            onClick={() => inputRef.current?.click()}
            aria-label="添加图片"
          >
            <Plus size={24} />
            <span>添加图片</span>
          </button>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_EXT.join(',')}
        multiple
        hidden
        onChange={(e) => handleFiles(e.target.files)}
      />
      <p className="sr-image-uploader-hint">
        最多 {maxCount} 张，支持 jpg/jpeg/png/webp，单张不超过 5MB
        {isEditMode && '；已上传图片暂不支持删除'}
      </p>
    </div>
  );
}

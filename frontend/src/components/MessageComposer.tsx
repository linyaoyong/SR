import { useRef, useState } from 'react';
import { Button, Input, message } from 'antd';
import { ImageIcon, Send, X } from 'lucide-react';
import { messageService } from '../services/message';
import type { Message } from '../types/api';

interface MessageComposerProps {
  conversationId: number;
  disabled?: boolean;
  onSent?: (message: Message) => void;
}

// 消息输入区：支持文本（messageType=1）与本地上传图片（messageType=2，imageUrls 为 JSON 数组字符串）。
// 字段对齐 docs/03-api-contract.md 第 11.1 节 SendMessageRequest。
export default function MessageComposer({ conversationId, disabled, onSent }: MessageComposerProps) {
  const [text, setText] = useState('');
  const [pendingImageUrl, setPendingImageUrl] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [sending, setSending] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const hasImage = pendingImageUrl.trim().length > 0;
  const hasText = text.trim().length > 0;
  const canSend = !sending && !uploadingImage && !disabled && (hasText || hasImage);

  const handleImageSelected = async (file?: File) => {
    if (!file) return;
    setUploadingImage(true);
    try {
      const uploaded = await messageService.uploadImage(file);
      setPendingImageUrl(uploaded.url);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '图片上传失败');
    } finally {
      setUploadingImage(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleSend = async () => {
    if (!canSend) return;
    setSending(true);
    try {
      let sent: Message;
      if (hasImage) {
        sent = await messageService.send(conversationId, {
          messageType: 2,
          imageUrls: JSON.stringify([pendingImageUrl.trim()]),
        });
        setPendingImageUrl('');
      } else {
        sent = await messageService.send(conversationId, {
          messageType: 1,
          content: text.trim(),
        });
        setText('');
      }
      onSent?.(sent);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '发送失败');
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="sr-msg-composer">
      <input
        ref={fileInputRef}
        className="sr-visually-hidden"
        type="file"
        accept="image/*"
        aria-label="选择图片"
        onChange={(event) => handleImageSelected(event.target.files?.[0])}
        disabled={disabled || sending || uploadingImage}
      />
      {pendingImageUrl && (
        <div className="sr-msg-image-preview">
          <img src={pendingImageUrl} alt="待发送图片" />
          <button
            type="button"
            aria-label="移除图片"
            onClick={() => setPendingImageUrl('')}
            disabled={disabled || sending}
          >
            <X size={14} />
          </button>
        </div>
      )}
      <div className="sr-msg-composer-row">
        <Button
          type="text"
          aria-label="图片"
          icon={<ImageIcon size={18} />}
          onClick={() => fileInputRef.current?.click()}
          disabled={disabled || sending || uploadingImage}
          loading={uploadingImage}
        />
        <Input.TextArea
          aria-label="消息内容"
          placeholder="输入消息..."
          value={text}
          onChange={(e) => setText(e.target.value)}
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault();
              handleSend();
            }
          }}
          autoSize={{ minRows: 1, maxRows: 4 }}
          disabled={disabled || sending}
        />
        <Button
          type="primary"
          icon={<Send size={16} />}
          onClick={handleSend}
          disabled={!canSend}
        >
          发送
        </Button>
      </div>
    </div>
  );
}

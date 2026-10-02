import { render, screen } from '@testing-library/react';
import { message } from 'antd';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import MessageComposer from './MessageComposer';

const mocks = vi.hoisted(() => ({
  send: vi.fn(),
  uploadImage: vi.fn(),
}));

vi.mock('../services/message', () => ({
  messageService: {
    send: mocks.send,
    uploadImage: mocks.uploadImage,
  },
}));

function renderComposer(props: Partial<React.ComponentProps<typeof MessageComposer>> = {}) {
  const defaultProps: React.ComponentProps<typeof MessageComposer> = {
    conversationId: 42,
    onSent: vi.fn(),
    ...props,
  };
  return render(<MessageComposer {...defaultProps} />);
}

describe('MessageComposer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('disables send button when text input is empty', () => {
    renderComposer();
    expect(screen.getByRole('button', { name: '发送' })).toBeDisabled();
  });

  it('enables send button when text is entered', async () => {
    renderComposer();
    const input = screen.getByLabelText('消息内容');
    await userEvent.type(input, '你好');
    expect(screen.getByRole('button', { name: '发送' })).not.toBeDisabled();
  });

  it('sends text message with messageType=1 and content', async () => {
    const onSent = vi.fn();
    const sentMessage = { id: 1, conversationId: 42, messageType: 1, content: '你好' };
    mocks.send.mockResolvedValue(sentMessage);
    renderComposer({ onSent });
    await userEvent.type(screen.getByLabelText('消息内容'), '你好');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));
    await vi.waitFor(() => {
      expect(mocks.send).toHaveBeenCalledWith(42, { messageType: 1, content: '你好' });
      expect(onSent).toHaveBeenCalledWith(sentMessage);
    });
  });

  it('uploads, previews, removes and sends image message with imageUrls as JSON array string', async () => {
    const onSent = vi.fn();
    const sentMessage = {
      id: 2,
      conversationId: 42,
      messageType: 2,
      imageUrls: '["/uploads/messages/a.png"]',
    };
    const file = new File(['image'], 'a.png', { type: 'image/png' });
    mocks.uploadImage.mockResolvedValueOnce({ url: '/uploads/messages/a.png' });
    mocks.uploadImage.mockResolvedValueOnce({ url: '/uploads/messages/a.png' });
    mocks.send.mockResolvedValue(sentMessage);
    renderComposer({ onSent });

    await userEvent.upload(screen.getByLabelText('选择图片'), file);
    expect(await screen.findByAltText('待发送图片')).toHaveAttribute(
      'src',
      '/uploads/messages/a.png',
    );
    await userEvent.click(screen.getByRole('button', { name: '移除图片' }));
    expect(screen.queryByAltText('待发送图片')).not.toBeInTheDocument();

    await userEvent.upload(screen.getByLabelText('选择图片'), file);
    expect(await screen.findByAltText('待发送图片')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '发送' }));
    await vi.waitFor(() => {
      expect(mocks.send).toHaveBeenCalledWith(42, {
        messageType: 2,
        imageUrls: '["/uploads/messages/a.png"]',
      });
      expect(onSent).toHaveBeenCalledWith(sentMessage);
    });
  });

  it('disables send button while sending and resets after', async () => {
    let resolveSend: (value: unknown) => void = () => {};
    mocks.send.mockReturnValue(
      new Promise((resolve) => {
        resolveSend = resolve;
      }),
    );
    renderComposer();
    const input = screen.getByLabelText('消息内容');
    await userEvent.type(input, '正在发送');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));
    await vi.waitFor(() => {
      expect(screen.getByRole('button', { name: '发送' })).toBeDisabled();
    });
    resolveSend({ id: 9, conversationId: 42, messageType: 1, content: '正在发送' });
    // 输入框在发送成功后被清空，按钮因空输入保持禁用；此时再次输入应恢复可点，
    // 以验证 sending 标志已复位（否则会因 sending=true 永久禁用）。
    await vi.waitFor(() => {
      expect(input).toHaveValue('');
    });
    await userEvent.type(input, '再发一条');
    expect(screen.getByRole('button', { name: '发送' })).not.toBeDisabled();
  });

  it('clears text input after successful send', async () => {
    mocks.send.mockResolvedValue({ id: 1, conversationId: 42, messageType: 1, content: 'x' });
    renderComposer();
    const input = screen.getByLabelText('消息内容') as HTMLInputElement;
    await userEvent.type(input, 'hello');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));
    await vi.waitFor(() => {
      expect(input.value).toBe('');
    });
  });

  it('shows error toast and keeps input when send fails', async () => {
    const errorSpy = vi.spyOn(message, 'error').mockImplementation(() => 'fake-key' as never);
    mocks.send.mockRejectedValue(new Error('网络异常'));
    const onSent = vi.fn();
    renderComposer({ onSent });
    const input = screen.getByLabelText('消息内容') as HTMLInputElement;
    await userEvent.type(input, '你好');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));
    await vi.waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('网络异常');
    });
    // 发送失败时不调用 onSent，输入内容保留以便重试。
    expect(onSent).not.toHaveBeenCalled();
    expect(input.value).toBe('你好');
    // sending 标志需复位，否则按钮无法再次点击。
    expect(screen.getByRole('button', { name: '发送' })).not.toBeDisabled();
  });

  it('shows fallback 兜底文案 when send fails with non-Error', async () => {
    const errorSpy = vi.spyOn(message, 'error').mockImplementation(() => 'fake-key' as never);
    mocks.send.mockRejectedValue('unexpected');
    renderComposer();
    const input = screen.getByLabelText('消息内容') as HTMLInputElement;
    await userEvent.type(input, '你好');
    await userEvent.click(screen.getByRole('button', { name: '发送' }));
    await vi.waitFor(() => {
      expect(errorSpy).toHaveBeenCalledWith('发送失败');
    });
  });
});

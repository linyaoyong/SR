import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ConversationList from './ConversationList';
import type { Conversation } from '../types/api';

function dateText(year: number, month: number, day: number, hour = 9, minute = 5) {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')} ${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00`;
}

describe('ConversationList', () => {
  it('renders item and peer summaries instead of raw ids', () => {
    const now = new Date();
    const conversations: Conversation[] = [
      {
        id: 7,
        itemId: 10,
        userAId: 1,
        userBId: 2,
        itemTitle: '索尼微单相机',
        itemFirstImageUrl: '/uploads/items/camera.jpg',
        peerUserId: 2,
        peerUsername: '林同学',
        peerAvatarUrl: '/uploads/avatars/lin.jpg',
        lastMessageContent: '明天下午可以面交',
        lastMessageTime: dateText(now.getFullYear(), now.getMonth() + 1, now.getDate()),
        unreadCount: 3,
      },
    ];

    render(<ConversationList conversations={conversations} onSelect={vi.fn()} />);

    expect(screen.getByText('索尼微单相机')).toBeInTheDocument();
    expect(screen.queryByText('物品 #10')).not.toBeInTheDocument();
    expect(screen.getByText('林同学')).toBeInTheDocument();
    expect(screen.getByAltText('索尼微单相机图片')).toHaveAttribute(
      'src',
      '/uploads/items/camera.jpg',
    );
    expect(screen.getByAltText('林同学头像')).toHaveAttribute('src', '/uploads/avatars/lin.jpg');
    expect(screen.getByText('明天下午可以面交')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('formats same-year and previous-year conversation times', () => {
    const now = new Date();
    const sameYearDay = now.getDate() === 1 ? 2 : 1;
    const sameYearMonth = now.getMonth() + 1;
    const previousYear = now.getFullYear() - 1;

    const conversations: Conversation[] = [
      {
        id: 1,
        itemId: 10,
        userAId: 1,
        userBId: 2,
        itemTitle: '同年消息',
        peerUsername: '用户A',
        lastMessageTime: dateText(now.getFullYear(), sameYearMonth, sameYearDay),
        unreadCount: 0,
      },
      {
        id: 2,
        itemId: 11,
        userAId: 1,
        userBId: 3,
        itemTitle: '跨年消息',
        peerUsername: '用户B',
        lastMessageTime: dateText(previousYear, 6, 29),
        unreadCount: 128,
      },
    ];

    render(<ConversationList conversations={conversations} onSelect={vi.fn()} />);

    expect(screen.getByText(`${String(sameYearMonth).padStart(2, '0')}-${String(sameYearDay).padStart(2, '0')}`)).toBeInTheDocument();
    expect(screen.getByText(`${previousYear}\u201106\u201129`)).toBeInTheDocument();
    expect(screen.getByText('99+')).toBeInTheDocument();
  });
});

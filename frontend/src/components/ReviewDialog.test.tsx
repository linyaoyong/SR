import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ReviewDialog from './ReviewDialog';

const mocks = vi.hoisted(() => ({
  rentalService: {
    createReview: vi.fn(),
    reviews: vi.fn(),
    uploadReviewImage: vi.fn(),
  },
}));

vi.mock('../services/rental', () => ({ rentalService: mocks.rentalService }));

describe('ReviewDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:review-preview'),
      revokeObjectURL: vi.fn(),
    });
    mocks.rentalService.reviews.mockResolvedValue([]);
  });

  it('uploads review images and submits uploaded file URLs as JSON imageUrls', async () => {
    mocks.rentalService.uploadReviewImage.mockResolvedValue({
      url: '/files/reviews/a.jpg',
      filename: 'a.jpg',
      contentType: 'image/jpeg',
      size: 1234,
    });
    mocks.rentalService.createReview.mockResolvedValue({
      id: 1,
      orderId: 9,
      itemId: 11,
      reviewerId: 7,
      revieweeId: 2,
      rating: 5,
      content: '体验很好',
      imageUrls: '["/files/reviews/a.jpg"]',
      status: 0,
    });

    render(
      <ReviewDialog
        open
        orderId={9}
        currentUserId={7}
        onClose={vi.fn()}
      />,
    );

    await userEvent.upload(
      await screen.findByLabelText('选择评价图片'),
      new File(['image'], 'a.jpg', { type: 'image/jpeg' }),
    );
    expect(await screen.findByAltText('评价图片预览')).toHaveAttribute('src', '/files/reviews/a.jpg');

    await userEvent.click(screen.getAllByRole('radio')[4]);
    await userEvent.type(screen.getByLabelText(/评价内容/), '体验很好');
    await userEvent.click(screen.getByRole('button', { name: '提交评价' }));

    await waitFor(() => {
      expect(mocks.rentalService.createReview).toHaveBeenCalledWith(9, {
        rating: 5,
        content: '体验很好',
        imageUrls: '["/files/reviews/a.jpg"]',
      });
    });
  });

  it('loads the current user existing review and allows resubmitting updated content', async () => {
    mocks.rentalService.reviews.mockResolvedValue([
      {
        id: 2,
        orderId: 9,
        itemId: 11,
        reviewerId: 7,
        revieweeId: 2,
        rating: 4,
        content: '已经评价过',
        imageUrls: '["/files/reviews/old.jpg"]',
        status: 0,
      },
    ]);
    mocks.rentalService.createReview.mockResolvedValue({
      id: 2,
      orderId: 9,
      itemId: 11,
      reviewerId: 7,
      revieweeId: 2,
      rating: 5,
      content: '重新评价',
      imageUrls: '["/files/reviews/old.jpg"]',
      status: 0,
    });

    render(
      <ReviewDialog
        open
        orderId={9}
        currentUserId={7}
        onClose={vi.fn()}
      />,
    );

    expect(await screen.findByLabelText(/评价内容/)).toHaveValue('已经评价过');
    expect(screen.getByAltText('评价图片预览')).toHaveAttribute('src', '/files/reviews/old.jpg');
    const content = screen.getByLabelText(/评价内容/);
    await userEvent.clear(content);
    await userEvent.type(content, '重新评价');
    await userEvent.click(screen.getAllByRole('radio')[4]);
    await userEvent.click(screen.getByRole('button', { name: '更新评价' }));

    await waitFor(() => {
      expect(mocks.rentalService.createReview).toHaveBeenCalledWith(9, {
        rating: 5,
        content: '重新评价',
        imageUrls: '["/files/reviews/old.jpg"]',
      });
    });
  });
});

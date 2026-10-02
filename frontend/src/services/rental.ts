import { http, requestData } from './http';
import type {
  Dispute,
  RentalApplication,
  RentalApplicationCreateRequest,
  RentalOrder,
  RentalProposal,
  RentalProposalUpdateRequest,
  Review,
} from '../types/api';

// 租借申请 / 订单 / 评价 / 异议 服务。路径对齐 docs/03-api-contract.md 第 9、10、11 节。
// createReview/createDispute/reviews/disputes 方法在此一并维护，UI 由 Task 7 接入。
export const rentalService = {
  createApplication: (body: RentalApplicationCreateRequest) =>
    requestData<RentalApplication>(http.post('/api/rentals/applications', body)),
  applications: () => requestData<RentalApplication[]>(http.get('/api/rentals/applications')),
  applicationDetail: (id: number) =>
    requestData<RentalApplication>(http.get(`/api/rentals/applications/${id}`)),
  updateProposal: (id: number, body: RentalProposalUpdateRequest) =>
    requestData<RentalProposal>(http.put(`/api/rentals/applications/${id}/proposal`, body)),
  confirmProposal: (id: number) =>
    requestData<RentalApplication>(http.put(`/api/rentals/applications/${id}/confirm`)),
  cancelApplication: (id: number) =>
    requestData<null>(http.post(`/api/rentals/applications/${id}/cancel`)),
  orders: () => requestData<RentalOrder[]>(http.get('/api/orders')),
  orderDetail: (id: number) => requestData<RentalOrder>(http.get(`/api/orders/${id}`)),
  payRent: (id: number, amount: number) =>
    requestData<RentalOrder>(http.post(`/api/orders/${id}/pay`, { amount })),
  freezeDeposit: (id: number, amount: number) =>
    requestData<RentalOrder>(http.post(`/api/orders/${id}/freeze-deposit`, { amount })),
  ship: (id: number, body: { shipCompany: string; shipTrackingNo: string }) =>
    requestData<null>(http.post(`/api/orders/${id}/ship`, body)),
  receive: (id: number) => requestData<null>(http.post(`/api/orders/${id}/receive`)),
  returnOrder: (id: number, body?: { returnCompany?: string; returnTrackingNo?: string }) =>
    requestData<null>(http.post(`/api/orders/${id}/return`, body)),
  complete: (id: number) => requestData<null>(http.post(`/api/orders/${id}/complete`)),
  cancelOrder: (id: number, cancelReason?: string) =>
    requestData<null>(http.post(`/api/orders/${id}/cancel`, { cancelReason })),
  createReview: (orderId: number, body: { rating: number; content?: string; imageUrls?: string }) =>
    requestData<Review>(http.post(`/api/orders/${orderId}/reviews`, body)),
  reviews: (orderId: number) => requestData<Review[]>(http.get(`/api/orders/${orderId}/reviews`)),
  uploadReviewImage: (orderId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return requestData<{ url: string; filename: string; contentType: string; size: number }>(
      http.post(`/api/orders/${orderId}/reviews/images`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    );
  },
  userHistory: (userId: number) =>
    requestData<RentalOrder[]>(http.get(`/api/rentals/users/${userId}/history`)),
  userReviews: (userId: number) =>
    requestData<Review[]>(http.get(`/api/rentals/users/${userId}/reviews`)),
  itemReviews: (itemId: number) =>
    requestData<Review[]>(http.get(`/api/rentals/items/${itemId}/reviews`)),
  disputes: (orderId: number) =>
    requestData<Dispute[]>(http.get(`/api/orders/${orderId}/disputes`)),
  createDispute: (
    orderId: number,
    body: {
      reason: string;
      description?: string;
      expectedDepositDeduction?: number;
      imageUrls?: string;
    },
  ) => requestData<Dispute>(http.post(`/api/orders/${orderId}/disputes`, body)),
};

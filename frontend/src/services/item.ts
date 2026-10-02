import { http, requestData } from './http';
import type {
  Category,
  FavoriteItem,
  ItemDetail,
  ItemImage,
  ItemListItem,
  PageResult,
} from '../types/api';

export const itemService = {
  categories: () => requestData<Category[]>(http.get('/api/categories')),
  list: (params: { categoryId?: number; keyword?: string; page?: number; size?: number }) =>
    requestData<PageResult<ItemListItem>>(http.get('/api/items', { params })),
  detail: (id: number) => requestData<ItemDetail>(http.get(`/api/items/${id}`)),
  favorites: () => requestData<FavoriteItem[]>(http.get('/api/favorites')),
  addFavorite: (itemId: number) => requestData<null>(http.post(`/api/favorites/${itemId}`)),
  removeFavorite: (itemId: number) => requestData<null>(http.delete(`/api/favorites/${itemId}`)),
};

// 发布/编辑物品请求体。字段对齐 docs/03-api-contract.md 第 8.1 节 ItemCreateRequest。
export interface ItemFormPayload {
  title: string;
  description: string;
  categoryId: number;
  tags?: string;
  quantity: number;
  supportDelivery: number;
  deliveryCity?: string;
  supportMeetup: number;
  meetupLocation?: string;
  priceType: number;
  dailyPrice: number;
  minRentDays: number;
  freeRent?: number;
  depositEnabled: number;
  depositAmount?: number;
  creditDepositEnabled: number;
  minCreditScore?: number;
  freeDepositScore?: number;
  reducedDepositScore?: number;
  reducedDepositAmount?: number;
}

// 物品管理相关写操作与"我的物品"查询。路径与 docs/03-api-contract.md 第 8.1 节一致。
export const itemMutations = {
  create: (body: ItemFormPayload) => requestData<ItemDetail>(http.post('/api/items', body)),
  update: (id: number, body: Partial<ItemFormPayload>) =>
    requestData<null>(http.put(`/api/items/${id}`, body)),
  uploadImages: (id: number, files: File[]) => {
    const form = new FormData();
    files.forEach((file) => form.append('file', file));
    return requestData<ItemImage[]>(http.post(`/api/items/${id}/images`, form));
  },
  mine: (page = 1, size = 20) =>
    requestData<PageResult<ItemListItem>>(http.get('/api/items/mine', { params: { page, size } })),
  offShelf: (id: number) => requestData<null>(http.put(`/api/items/${id}/off-shelf`)),
  reList: (id: number) => requestData<null>(http.put(`/api/items/${id}/re-list`)),
  remove: (id: number) => requestData<null>(http.delete(`/api/items/${id}`)),
};

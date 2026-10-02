import { http, requestData } from './http';
import type { UserMeResponse, UserPublicProfile } from '../types/api';

export const userService = {
  me: () => requestData<UserMeResponse>(http.get('/api/users/me')),
  updateMe: (body: Partial<Pick<UserMeResponse, 'username' | 'description' | 'showRentalHistory'>>) =>
    requestData<UserMeResponse>(http.put('/api/users/me', body)),
  updateAvatar: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return requestData<{ url: string; filename: string; contentType: string; size: number }>(
      http.post('/api/files/avatars', formData),
    );
  },
  changePassword: (body: { oldPassword: string; newPassword: string }) =>
    requestData<null>(http.put('/api/users/me/password', body)),
  publicProfile: (id: number) => requestData<UserPublicProfile>(http.get(`/api/users/${id}`)),
  blacklist: () => requestData<UserPublicProfile[]>(http.get('/api/users/blacklist')),
  addBlacklist: (targetUserId: number) => requestData<null>(http.post(`/api/users/blacklist/${targetUserId}`)),
  removeBlacklist: (targetUserId: number) =>
    requestData<null>(http.delete(`/api/users/blacklist/${targetUserId}`)),
};

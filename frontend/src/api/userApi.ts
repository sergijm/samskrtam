import api from './axios';
import { User, Group, GroupDetail, UserGroupSummary, UpdateProfilePayload, UploadUrlResponse, AvatarConfirmResponse, AdminUserListResponse, UserRole } from '../types/user'; // Import UserRole

export const userApi = {
  // Profile
  getMe: () => api.get<User>('/api/v1/users/me'),
  updateProfileDetails: (data: UpdateProfilePayload) => api.put<User>('/api/v1/users/me', data),
  getUser: (userId: string) => api.get<User>(`/api/v1/users/${userId}`),
  getUserGroups: (userId: string) => api.get<UserGroupSummary[]>(`/api/v1/users/${userId}/groups`),

  // Avatar
  generateUploadUrl: (contentType: string) => api.post<UploadUrlResponse>('/api/v1/users/me/avatar/upload-url', {}, { headers: { 'Content-Type': contentType } }),
  confirmAvatarUpload: (objectKey: string) => api.post<AvatarConfirmResponse>('/api/v1/users/me/avatar/confirm', { objectKey }),

  // Admin
  getAdminUsers: (
    page: number,
    size: number,
    sortBy: string,
    sortDirection: string,
    search?: string,
    role?: UserRole, // Corrected to UserRole
    blocked?: boolean
  ) => api.get<AdminUserListResponse>('/api/v1/admin/users', {
    params: {
      page,
      size,
      sortBy,
      sortDirection,
      search,
      role,
      blocked,
    },
  }),

  // Search
  searchUsers: (query: string) => api.get<User[]>(`/api/v1/users/search`, { params: { query } }),

  // Groups
  getGroups: () => api.get<Group[]>('/api/v1/groups'),
  getGroup: (groupId: string) => api.get<GroupDetail>(`/api/v1/groups/${groupId}`),
  createGroup: (name: string) => api.post<Group>('/api/v1/groups', { name }),
  renameGroup: (groupId: string, name: string) => api.patch<Group>('/api/v1/groups/${groupId}', { name }),

  // Members
  addMember: (groupId: string, userId: string) => api.post(`/api/v1/groups/${groupId}/members`, { userId }),
  removeMember: (groupId: string, userId: string) => api.delete(`/api/v1/groups/${groupId}/members/${userId}`),

  // Curator
  setCurator: (groupId: string, userId: string) => api.put(`/api/v1/groups/${groupId}/curator`, { userId }),
};

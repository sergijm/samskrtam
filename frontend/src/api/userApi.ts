import api from './axios';
import { User, Group, GroupDetail, UserGroupSummary, Locale, Theme } from '../types/user';

export const userApi = {
  // Profile
  getMe: () => api.get<User>('/api/v1/users/me'),
  updateMe: (data: { locale?: Locale; theme?: Theme }) => api.patch<User>('/api/v1/users/me', data),
  getUser: (userId: string) => api.get<User>(`/api/v1/users/${userId}`),
  getUserGroups: (userId: string) => api.get<UserGroupSummary[]>(`/api/v1/users/${userId}/groups`),

  // Groups
  getGroups: () => api.get<Group[]>('/api/v1/groups'),
  getGroup: (groupId: string) => api.get<GroupDetail>(`/api/v1/groups/${groupId}`),
  createGroup: (name: string) => api.post<Group>('/api/v1/groups', { name }),
  renameGroup: (groupId: string, name: string) => api.patch<Group>(`/api/v1/groups/${groupId}`, { name }),

  // Members
  addMember: (groupId: string, userId: string) => api.post(`/api/v1/groups/${groupId}/members`, { userId }),
  removeMember: (groupId: string, userId: string) => api.delete(`/api/v1/groups/${groupId}/members/${userId}`),

  // Curator
  setCurator: (groupId: string, userId: string) => api.put(`/api/v1/groups/${groupId}/curator`, { userId }),
};

export interface User {
  id: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
  roles: ('STUDENT' | 'ADMIN')[]; // Changed from 'role' to 'roles' and made it an array of roles
  blocked: boolean;
  createdAt: string; // ISO 8601 string
  locale: 'ru' | 'en';
  theme: 'light' | 'dark';
}

export type Theme = 'light' | 'dark';
export type Locale = 'ru' | 'en';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string | null; // Changed to allow null
}

export type GroupRole = 'CURATOR' | 'MEMBER';

export interface Group {
  id: string;
  name: string;
  curatorId: string;
  curatorName: string;
  memberCount: number;
  createdAt: string; // ISO 8601
}

export interface GroupMember {
  userId: string;
  username: string;
  email: string;
  groupRole: GroupRole;
  joinedAt: string; // ISO 8601
}

export interface GroupDetail extends Group {
  members: GroupMember[];
}

export interface UserGroupSummary {
  groupId: string;
  groupName: string;
  groupRole: GroupRole;
}

// New type for updating user profile (matching backend's UpdateProfileRequest)
export interface UpdateProfilePayload {
  username: string;
  firstName?: string;
  lastName?: string;
}

// New types for avatar upload
export interface UploadUrlResponse {
  uploadUrl: string;
  objectKey: string;
}

export interface AvatarConfirmResponse {
  avatarUrl: string;
}

// New type for admin user list response
export interface AdminUserListResponse {
  users: User[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
  isFirst: boolean;
  isLast: boolean;
}

export enum UserRole {
  STUDENT = 'STUDENT',
  ADMIN = 'ADMIN',
}

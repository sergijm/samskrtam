export interface User {
  id: string;
  username: string;
  email: string;
  role: 'STUDENT' | 'ADMIN';
  locale: 'ru' | 'en';
  theme: 'light' | 'dark';
}

export type Theme = 'light' | 'dark';
export type Locale = 'ru' | 'en';

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
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

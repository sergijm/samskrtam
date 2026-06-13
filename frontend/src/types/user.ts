export interface User {
    id: string;
    username: string;
    email: string;
    firstName?: string;
    lastName?: string;
    avatarUrl?: string;
    roles: string[];
    theme?: string;
    locale?: string;
    quizSize?: number;
}

export interface AuthTokens {
    accessToken: string;
    refreshToken: string | null;
}

export interface UpdateProfilePayload {
    username: string;
    firstName?: string;
    lastName?: string;
    quizSize?: number;
}

export enum UserRole {
    ADMIN = 'ADMIN',
    STUDENT = 'STUDENT',
}

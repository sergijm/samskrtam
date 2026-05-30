export interface User {
  id: string;
  username: string;
  email: string;
  role: 'STUDENT' | 'ADMIN';
  locale: 'ru' | 'en';
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// Ответ от бэкенда при успешном логине
export type AuthResponse = AuthTokens & {
  user: User;
};

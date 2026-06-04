import api from './axios';
import { AuthTokens } from '../types/user';

const API_URL = import.meta.env.VITE_API_URL;

// Define the raw response structure from Keycloak/backend
interface KeycloakAuthResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  refresh_expires_in: number;
  token_type: string;
  'not-before-policy': number;
  session_state: string;
  scope: string;
}

export const authApi = {
  // ROPC - login via form
  login: async (username: string, password: string): Promise<AuthTokens> => {
    const response = await api.post<KeycloakAuthResponse>('/api/v1/auth/login', { username, password });
    return {
      accessToken: response.data.access_token,
      refreshToken: response.data.refresh_token,
    };
  },

  // OAuth2 redirects
  loginWithGoogle: () => {
    window.location.href = `${API_URL}/api/v1/auth/oauth2/google`;
  },

  loginWithMailRu: () => {
    window.location.href = `${API_URL}/api/v1/auth/oauth2/mailru`;
  },

  // Exchange authorization code for tokens
  callback: async (code: string): Promise<AuthTokens> => {
    const response = await api.post<KeycloakAuthResponse>('/api/v1/auth/callback', { code });
    return {
      accessToken: response.data.access_token,
      refreshToken: response.data.refresh_token,
    };
  },

  // Registration
  register: (username: string, email: string, password: string) =>
    api.post('/api/v1/auth/register', { username, email, password }),

  // Password recovery
  forgotPassword: (email: string) =>
    api.post('/api/v1/auth/forgot-password', { email }),

  // Change password (requires JWT)
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/api/v1/auth/change-password', { currentPassword, newPassword }),

  // Refresh access token
  refresh: async (refreshToken: string): Promise<AuthTokens> => {
    const response = await api.post<KeycloakAuthResponse>('/api/v1/auth/refresh', { refreshToken });
    return {
      accessToken: response.data.access_token,
      refreshToken: response.data.refresh_token,
    };
  },

  // Logout
  logout: (refreshToken: string) =>
    api.post('/api/v1/auth/logout', { refreshToken }),
};

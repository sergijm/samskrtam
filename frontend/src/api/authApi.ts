import api from './axios';
import { AuthTokens } from '../types/user';

const API_URL = import.meta.env.VITE_API_URL;

interface AuthResponse extends AuthTokens {
  // The user object is also expected in the response from the backend
  // but we handle it separately in the store.
}

export const authApi = {
  // ROPC - login via form
  login: (username, password) => // Changed 'email' to 'username'
    api.post<AuthResponse>('/api/v1/auth/login', { username, password }), // Changed 'email' to 'username' in request body

  // OAuth2 redirects
  loginWithGoogle: () => {
    window.location.href = `${API_URL}/api/v1/auth/oauth2/google`;
  },

  loginWithMailRu: () => {
    window.location.href = `${API_URL}/api/v1/auth/oauth2/mailru`;
  },

  // Exchange authorization code for tokens
  callback: (code) =>
    api.post<AuthResponse>('/api/v1/auth/callback', { code }),

  // Registration
  register: (username, email, password) =>
    api.post('/api/v1/auth/register', { username, email, password }),

  // Password recovery
  forgotPassword: (email) =>
    api.post('/api/v1/auth/forgot-password', { email }),

  // Change password (requires JWT)
  changePassword: (currentPassword, newPassword) =>
    api.post('/api/v1/auth/change-password', { currentPassword, newPassword }),

  // Refresh access token
  refresh: (refreshToken) =>
    api.post<{ accessToken: string }>('/api/v1/auth/refresh', { refreshToken }),

  // Logout
  logout: (refreshToken) =>
    api.post('/api/v1/auth/logout', { refreshToken }),
};

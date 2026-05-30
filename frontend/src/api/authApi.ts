import api from './axios';
import { AuthResponse } from '../types/auth';

const API_BASE_URL = import.meta.env.VITE_API_URL;

export const authApi = {
  login: (data: { email: string; password: string }): Promise<AuthResponse> =>
    api.post('/api/v1/auth/login', data).then((res) => res.data),

  loginWithGoogle: () => {
    window.location.href = `${API_BASE_URL}/api/v1/auth/oauth2/google`;
  },

  loginWithMailRu: () => {
    window.location.href = `${API_BASE_URL}/api/v1/auth/oauth2/mailru`;
  },

  exchangeCode: (code: string): Promise<AuthResponse> =>
    api.post('/api/v1/auth/callback', { code }).then((res) => res.data),
};

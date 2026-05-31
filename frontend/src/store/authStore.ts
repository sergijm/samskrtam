import { create } from 'zustand';
import { User, AuthTokens } from '../types/user';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  login: (tokens: AuthTokens, user: User) => void;
  logout: () => void;
  setAccessToken: (token: string) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,

  login: (tokens, user) => set({
    user,
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
    isAuthenticated: true,
  }),

  logout: () => {
    // Here you might want to call the API to invalidate the refresh token
    // For now, just clear the state
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
    });
  },

  setAccessToken: (token) => set({ accessToken: token }),
}));

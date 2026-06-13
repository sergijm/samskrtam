import { create } from 'zustand';
import { AuthTokens } from '../types/user';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  redirectPath: string | null;

  login: (tokens: AuthTokens) => void;
  logout: () => void;
  setAuthTokens: (tokens: AuthTokens) => void;
  setRedirectPath: (path: string | null) => void;
}

const getInitialState = () => {
  const accessToken = localStorage.getItem('accessToken');
  let refreshToken: string | null = localStorage.getItem('refreshToken');
  const redirectPath = localStorage.getItem('redirectPath');

  if (refreshToken === "null") {
    refreshToken = null;
  }

  console.log('authStore.ts: Initializing state. redirectPath from localStorage:', redirectPath);
  return {
    accessToken,
    refreshToken,
    isAuthenticated: !!accessToken,
    redirectPath,
  };
};

export const useAuthStore = create<AuthState>((set) => ({
  ...getInitialState(),

  login: (tokens) => {
    const normalizedRefreshToken = tokens.refreshToken === "null" ? null : tokens.refreshToken;

    localStorage.setItem('accessToken', tokens.accessToken);
    if (normalizedRefreshToken) {
      localStorage.setItem('refreshToken', normalizedRefreshToken);
    } else {
      localStorage.removeItem('refreshToken');
    }
    
    set({
      accessToken: tokens.accessToken,
      refreshToken: normalizedRefreshToken,
      isAuthenticated: !!tokens.accessToken,
    });
    console.log('authStore.ts: User logged in (tokens stored).');
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user'); // Keep this to clear old data
    
    set({
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
    });
    console.log('authStore.ts: User logged out.');
  },

  setAuthTokens: (tokens) => {
    const normalizedRefreshToken = tokens.refreshToken === "null" ? null : tokens.refreshToken;

    localStorage.setItem('accessToken', tokens.accessToken);
    if (normalizedRefreshToken) {
      localStorage.setItem('refreshToken', normalizedRefreshToken);
    } else {
      localStorage.removeItem('refreshToken');
    }
    set({
      accessToken: tokens.accessToken,
      refreshToken: normalizedRefreshToken,
      isAuthenticated: !!tokens.accessToken,
    });
    console.log('authStore.ts: Auth tokens set.');
  },

  setRedirectPath: (path) => {
    if (path) {
      localStorage.setItem('redirectPath', path);
      console.log('authStore.ts: Setting redirectPath in localStorage:', path);
    } else {
      localStorage.removeItem('redirectPath');
      console.log('authStore.ts: Clearing redirectPath in localStorage.');
    }
    set({ redirectPath: path });
  },
}));

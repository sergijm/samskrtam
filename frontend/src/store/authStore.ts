import { create } from 'zustand';
import { AuthTokens, User } from '../types/user';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  redirectPath: string | null;

  login: (tokens: AuthTokens, user?: User) => void;
  logout: () => void;
  setAuthTokens: (tokens: AuthTokens) => void;
  setUser: (user: User | null) => void;
  setRedirectPath: (path: string | null) => void;
}

const getInitialState = () => {
  const accessToken = localStorage.getItem('accessToken');
  let refreshToken: string | null = localStorage.getItem('refreshToken');
  const redirectPath = localStorage.getItem('redirectPath');
  const userStr = localStorage.getItem('user');
  let user: User | null = null;
  if (userStr) {
    try {
      user = JSON.parse(userStr);
    } catch {
      localStorage.removeItem('user');
    }
  }

  if (refreshToken === "null") {
    refreshToken = null;
  }

  console.log('authStore.ts: Initializing state. redirectPath from localStorage:', redirectPath);
  return {
    user,
    accessToken,
    refreshToken,
    isAuthenticated: !!accessToken,
    redirectPath,
  };
};

export const useAuthStore = create<AuthState>((set) => ({
  ...getInitialState(),

  login: (tokens, user) => {
    const normalizedRefreshToken = tokens.refreshToken === "null" ? null : tokens.refreshToken;

    localStorage.setItem('accessToken', tokens.accessToken);
    if (normalizedRefreshToken) {
      localStorage.setItem('refreshToken', normalizedRefreshToken);
    } else {
      localStorage.removeItem('refreshToken');
    }
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    }
    
    set({
      user,
      accessToken: tokens.accessToken,
      refreshToken: normalizedRefreshToken,
      isAuthenticated: !!tokens.accessToken,
    });
    console.log('authStore.ts: User logged in (tokens and user stored).');
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    
    set({
      user: null,
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

  setUser: (user) => {
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
    set({ user });
    console.log('authStore.ts: User set.');
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


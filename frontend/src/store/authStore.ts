import { create } from 'zustand';
import { User, AuthTokens } from '../types/user';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  redirectPath: string | null;

  login: (tokens: AuthTokens, user: User) => void;
  logout: () => void;
  setAuthTokens: (tokens: AuthTokens) => void;
  setRedirectPath: (path: string | null) => void;
  setUser: (user: User) => void; // New action to update user data
}

const getInitialState = () => {
  const accessToken = localStorage.getItem('accessToken');
  let refreshToken: string | null = localStorage.getItem('refreshToken');
  const userString = localStorage.getItem('user');
  const redirectPath = localStorage.getItem('redirectPath');
  let user: User | null = null;

  if (refreshToken === "null") {
    refreshToken = null;
  }

  try {
    if (userString) {
      user = JSON.parse(userString);
    }
  } catch (e) {
    console.error("Failed to parse user from localStorage", e);
    localStorage.removeItem('user');
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
    localStorage.setItem('user', JSON.stringify(user));
    set({
      user,
      accessToken: tokens.accessToken,
      refreshToken: normalizedRefreshToken,
      isAuthenticated: !!tokens.accessToken,
    });
    console.log('authStore.ts: User logged in. redirectPath in store:', useAuthStore.getState().redirectPath);
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
    console.log('authStore.ts: User logged out. redirectPath in store (should not be cleared):', useAuthStore.getState().redirectPath);
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
    console.log('authStore.ts: Auth tokens set. redirectPath in store:', useAuthStore.getState().redirectPath);
  },

  setRedirectPath: (path) => {
    if (path) {
      localStorage.setItem('redirectPath', path);
      console.log('authStore.ts: Setting redirectPath in localStorage and store:', path);
    } else {
      localStorage.removeItem('redirectPath');
      console.log('authStore.ts: Clearing redirectPath in localStorage and store.');
    }
    set({ redirectPath: path });
  },

  setUser: (user) => { // New action implementation
    localStorage.setItem('user', JSON.stringify(user));
    set({ user });
    console.log('authStore.ts: User data updated in store.');
  },
}));

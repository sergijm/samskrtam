import { create } from 'zustand';
import { User, AuthTokens } from '../types/user';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  redirectPath: string | null; // New state for redirect path

  login: (tokens: AuthTokens, user: User) => void;
  logout: () => void;
  setAuthTokens: (tokens: AuthTokens) => void; // New action to set both access and refresh tokens
  setRedirectPath: (path: string | null) => void; // New action to set redirect path
}

const getInitialState = () => {
  const accessToken = localStorage.getItem('accessToken');
  let refreshToken: string | null = localStorage.getItem('refreshToken');
  const userString = localStorage.getItem('user');
  const redirectPath = localStorage.getItem('redirectPath'); // Get redirect path from localStorage
  let user: User | null = null;

  // Normalize "null" string to actual null for refreshToken
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

  return {
    user,
    accessToken,
    refreshToken,
    isAuthenticated: !!accessToken, // Changed: Only depend on accessToken for isAuthenticated
    redirectPath, // Include redirectPath in initial state
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
      isAuthenticated: !!tokens.accessToken, // Changed: Only depend on accessToken for isAuthenticated
    });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    // Do NOT clear redirectPath here. It should only be cleared after successful login.
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      // Do NOT clear redirectPath in state here.
    });
  },

  setAuthTokens: (tokens) => { // New action implementation
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
      isAuthenticated: !!tokens.accessToken, // Changed: Only depend on accessToken for isAuthenticated
    });
  },

  setRedirectPath: (path) => {
    if (path) {
      localStorage.setItem('redirectPath', path);
    } else {
      localStorage.removeItem('redirectPath');
    }
    set({ redirectPath: path });
  },
}));

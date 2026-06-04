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

const getInitialState = () => {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');
  const userString = localStorage.getItem('user');
  let user: User | null = null;
  try {
    if (userString) {
      console.log('authStore: getInitialState - userString from localStorage:', userString); // Debug log
      user = JSON.parse(userString);
      console.log('authStore: getInitialState - parsed user:', user); // Debug log
    }
  } catch (e) {
    console.error("Failed to parse user from localStorage", e);
    localStorage.removeItem('user');
  }

  return {
    user,
    accessToken,
    refreshToken,
    isAuthenticated: !!accessToken && !!refreshToken,
  };
};

export const useAuthStore = create<AuthState>((set) => ({
  ...getInitialState(),

  login: (tokens, user) => {
    console.log('authStore: login - received user:', user); // Debug log
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    set({
      user,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      isAuthenticated: true,
    });
    console.log('authStore: login - user after set:', user); // Debug log
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
  },

  setAccessToken: (token) => {
    localStorage.setItem('accessToken', token);
    set({ accessToken: token });
  },
}));

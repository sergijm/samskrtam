import axios from 'axios';
import { useAuthStore } from '../store/authStore';
import { authApi } from './authApi';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

// Request interceptor to add the Bearer token
api.interceptors.request.use(
  (config) => {
    // Directly read from localStorage for robustness, as useAuthStore.getState() might not be fully hydrated yet
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for automatic token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    // Check for 401, that it's not a retry, and not the refresh token endpoint itself
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.includes('/auth/refresh')) {
      originalRequest._retry = true;
      const { refreshToken, setAccessToken, logout } = useAuthStore.getState();

      if (refreshToken) {
        try {
          const { data } = await authApi.refresh(refreshToken);
          setAccessToken(data.accessToken);
          // Update the authorization header of the original request
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          // Retry the original request
          return api(originalRequest);
        } catch (refreshError) {
          // If refresh fails, log the user out
          logout();
          return Promise.reject(refreshError);
        }
      } else {
        // No refresh token available, logout
        logout();
      }
    }
    return Promise.reject(error);
  }
);

export default api;

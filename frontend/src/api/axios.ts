import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

// Request interceptor to add the Bearer token
api.interceptors.request.use(
  (config) => {
    // Read token directly from localStorage to avoid race conditions with Zustand hydration
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
    
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.includes('/auth/refresh')) {
      originalRequest._retry = true;
      const { refreshToken, setAuthTokens, logout } = useAuthStore.getState();

      console.log('axios.ts: Intercepted 401. Refresh token exists:', !!refreshToken);

      if (refreshToken) {
        try {
          console.log('axios.ts: Access token expired. Attempting to refresh with token:', refreshToken);
          const response = await axios.post<{
            access_token: string;
            refresh_token: string;
          }>(
            `${import.meta.env.VITE_API_URL}/api/v1/auth/refresh`,
            { refresh_token: refreshToken },
            { headers: { 'Content-Type': 'application/json' } }
          );
          const newTokens: { accessToken: string; refreshToken: string } = {
            accessToken: response.data.access_token,
            refreshToken: response.data.refresh_token,
          };
          setAuthTokens(newTokens);
          originalRequest.headers.Authorization = `Bearer ${newTokens.accessToken}`;
          console.log('axios.ts: Token refreshed successfully. Retrying original request.');
          return api(originalRequest);
        } catch (refreshError) {
          console.error('axios.ts: Token refresh failed. Logging out.', refreshError);
          logout();
          return Promise.reject(refreshError);
        }
      } else {
        console.warn('axios.ts: No refresh token available. Logging out.');
        logout();
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

export default api;


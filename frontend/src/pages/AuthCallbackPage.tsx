import React, { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useAuthStore } from '../store/authStore';
import { AuthTokens } from '../types/user';
import { userApi } from '../api/userApi';

const AuthCallbackPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const { login, setUser, setRedirectPath } = useAuthStore();

  const hasHandledCallback = useRef(false);

  useEffect(() => {
    const handleCallback = async () => {
      if (hasHandledCallback.current) {
        return;
      }
      hasHandledCallback.current = true;

      const fragment = new URLSearchParams(location.hash.substring(1));
      const accessTokenFromFragment = fragment.get('access_token') || fragment.get('token');
      const refreshTokenFromFragment = fragment.get('refresh_token');
      const error = fragment.get('error');

      if (accessTokenFromFragment) {
        try {
          const tokens: AuthTokens = {
            accessToken: accessTokenFromFragment,
            refreshToken: refreshTokenFromFragment || null,
          };

          // 1. Store tokens and set authenticated state
          login(tokens);
          console.log('AuthCallbackPage: Tokens stored and user is authenticated.');

          // 2. Fetch user profile and update store
          try {
            const userResponse = await userApi.getMe();
            setUser(userResponse.data);
            console.log('AuthCallbackPage: User profile fetched and stored.');
        } catch (err) {
            console.error('AuthCallbackPage: Failed to fetch user profile:', err);
        }

          // 3. Invalidate 'me' query to force a refetch of user data everywhere
          await queryClient.invalidateQueries(['me']);
          console.log('AuthCallbackPage: "me" query invalidated.');

          // 4. Handle redirection
          const storedRedirectPath = localStorage.getItem('redirectPath');
        localStorage.removeItem('redirectPath');
        setRedirectPath(null);

          const targetPath = storedRedirectPath || '/dashboard';
          console.log('AuthCallbackPage: Redirecting to:', targetPath);
          navigate(targetPath, { replace: true });

        } catch (err) {
          console.error('AuthCallbackPage: OAuth callback error:', err);
          logoutAndClear();
    }
      } else {
        console.error('AuthCallbackPage: OAuth callback error from fragment:', error);
        logoutAndClear();
      }
};

    const logoutAndClear = () => {
        useAuthStore.getState().logout();
        localStorage.removeItem('redirectPath');
        setRedirectPath(null);
        navigate('/login', { state: { error: 'Authentication failed.' } });
    }

    handleCallback();
  }, [location, navigate, login, setUser, setRedirectPath, queryClient]);

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <ProgressSpinner />
    </div>
  );
};

export default AuthCallbackPage;


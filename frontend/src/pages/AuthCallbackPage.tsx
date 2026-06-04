import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useAuthStore } from '../store/authStore';
import { userApi } from '../api/userApi'; // Import userApi
import { AuthTokens } from '../types/user'; // Import AuthTokens

const AuthCallbackPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((state) => state.login);

  useEffect(() => {
    const handleCallback = async () => {
      const fragment = new URLSearchParams(location.hash.substring(1));
      const accessTokenFromFragment = fragment.get('access_token') || fragment.get('token'); // Try both names for access token
      const refreshTokenFromFragment = fragment.get('refresh_token'); // Get refresh token

      const error = fragment.get('error');

      if (accessTokenFromFragment) {
        try {
          // Temporarily store access token so axios interceptor can use it for getMe()
          localStorage.setItem('accessToken', accessTokenFromFragment);
          if (refreshTokenFromFragment) {
            localStorage.setItem('refreshToken', refreshTokenFromFragment);
          } else {
            // If refresh token is not provided, ensure it's cleared or set to null
            localStorage.removeItem('refreshToken');
          }

          // Fetch user details using the newly acquired access token
          const userResponse = await userApi.getMe();
          const user = userResponse.data;

          const tokens: AuthTokens = {
            accessToken: accessTokenFromFragment,
            refreshToken: refreshTokenFromFragment || null, // Pass null if refresh token is not available
          };

          login(tokens, user);
          navigate('/dashboard', { replace: true }); // Redirect to dashboard after successful login
        } catch (err) {
          console.error('OAuth callback error:', err);
          // Clear any partially stored tokens on error
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          navigate('/login', { state: { error: 'Authentication failed.' } });
        }
      } else if (error) {
        console.error('OAuth callback error from fragment:', error);
        navigate('/login', { state: { error: error || 'Authentication failed.' } });
      } else {
        // Fallback if no token or error in fragment
        navigate('/login', { state: { error: 'Invalid authentication callback.' } });
      }
    };

    handleCallback();
  }, [location, navigate, login]);

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <ProgressSpinner />
    </div>
  );
};

export default AuthCallbackPage;

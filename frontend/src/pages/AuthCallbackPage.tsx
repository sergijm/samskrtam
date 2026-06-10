import React, { useEffect, useRef } from 'react'; // Import useRef
import { useLocation, useNavigate } from 'react-router-dom';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useAuthStore } from '../store/authStore';
import { userApi } from '../api/userApi';
import { AuthTokens } from '../types/user';

const AuthCallbackPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, setRedirectPath } = useAuthStore();

  const hasHandledCallback = useRef(false); // Ref to track if callback has been handled

  useEffect(() => {
    const handleCallback = async () => {
      // Only run if not already handled
      if (hasHandledCallback.current) {
        console.log('AuthCallbackPage: Callback already handled, skipping.');
        return;
      }
      hasHandledCallback.current = true; // Mark as handled

      const fragment = new URLSearchParams(location.hash.substring(1));
      const accessTokenFromFragment = fragment.get('access_token') || fragment.get('token');
      const refreshTokenFromFragment = fragment.get('refresh_token');

      const error = fragment.get('error');

      console.log('AuthCallbackPage: Received fragment data.');
      console.log('AuthCallbackPage: accessTokenFromFragment present:', !!accessTokenFromFragment);
      console.log('AuthCallbackPage: refreshTokenFromFragment present:', !!refreshTokenFromFragment);
      if (error) {
        console.error('AuthCallbackPage: Error parameter in fragment:', error);
      }

      if (accessTokenFromFragment) {
        try {
          localStorage.setItem('accessToken', accessTokenFromFragment);
          if (refreshTokenFromFragment) {
            localStorage.setItem('refreshToken', refreshTokenFromFragment);
          } else {
            localStorage.removeItem('refreshToken');
          }

          console.log('AuthCallbackPage: Attempting to fetch user details with new access token.');
          const userResponse = await userApi.getMe();
          const user = userResponse.data;
          console.log('AuthCallbackPage: User details fetched successfully:', user);

          const tokens: AuthTokens = {
            accessToken: accessTokenFromFragment,
            refreshToken: refreshTokenFromFragment || null,
          };

          login(tokens, user);
          console.log('AuthCallbackPage: User logged in to store.');

          const storedRedirectPath = localStorage.getItem('redirectPath');
          localStorage.removeItem('redirectPath'); // Clear from localStorage immediately
          setRedirectPath(null); // Clear from Zustand store as well

          console.log('AuthCallbackPage: Stored redirectPath from localStorage:', storedRedirectPath);
          const targetPath = storedRedirectPath || '/dashboard';
          console.log('AuthCallbackPage: Redirecting to:', targetPath);
          navigate(targetPath, { replace: true });
        } catch (err: any) {
          console.error('AuthCallbackPage: OAuth callback error during user fetch or login:', err);
          if (err.response) {
            console.error('AuthCallbackPage: Error response status:', err.response.status);
            console.error('AuthCallbackPage: Error response data:', err.response.data);
          }
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('redirectPath'); // Ensure cleared on error
          setRedirectPath(null); // Clear from Zustand store on error
          navigate('/login', { state: { error: 'Authentication failed.' } });
        }
      } else if (error) {
        console.error('AuthCallbackPage: OAuth callback error from fragment:', error);
        localStorage.removeItem('redirectPath'); // Ensure cleared on error
        setRedirectPath(null); // Clear from Zustand store on error
        navigate('/login', { state: { error: error || 'Authentication failed.' } });
      } else {
        console.warn('AuthCallbackPage: No access token or error found in fragment.');
        localStorage.removeItem('redirectPath'); // Ensure cleared on error
        setRedirectPath(null); // Clear from Zustand store on error
        navigate('/login', { state: { error: 'Invalid authentication callback.' } });
      }
    };

    handleCallback();
  }, [location, navigate, login, setRedirectPath]); // Dependencies remain the same

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <ProgressSpinner />
    </div>
  );
};

export default AuthCallbackPage;

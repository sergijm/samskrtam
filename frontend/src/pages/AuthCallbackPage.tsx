import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useAuthStore } from '../store/authStore';
import { userApi } from '../api/userApi'; // Import userApi
import { AuthTokens } from '../types/user'; // Import AuthTokens

const AuthCallbackPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, redirectPath, setRedirectPath } = useAuthStore(); // Get redirectPath and setRedirectPath

  useEffect(() => {
    const handleCallback = async () => {
      const fragment = new URLSearchParams(location.hash.substring(1));
      const accessTokenFromFragment = fragment.get('access_token') || fragment.get('token'); // Try both names for access token
      const refreshTokenFromFragment = fragment.get('refresh_token'); // Get refresh token

      const error = fragment.get('error');

      console.log('AuthCallbackPage: Received fragment data.');
      console.log('AuthCallbackPage: accessTokenFromFragment present:', !!accessTokenFromFragment);
      console.log('AuthCallbackPage: refreshTokenFromFragment present:', !!refreshTokenFromFragment);
      if (error) {
        console.error('AuthCallbackPage: Error parameter in fragment:', error);
      }

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

          console.log('AuthCallbackPage: Attempting to fetch user details with new access token.');
          // Fetch user details using the newly acquired access token
          const userResponse = await userApi.getMe();
          const user = userResponse.data;
          console.log('AuthCallbackPage: User details fetched successfully:', user);

          const tokens: AuthTokens = {
            accessToken: accessTokenFromFragment,
            refreshToken: refreshTokenFromFragment || null, // Pass null if refresh token is not available
          };

          login(tokens, user);
          console.log('AuthCallbackPage: User logged in to store.');

          // Redirect to the saved path or dashboard
          const targetPath = redirectPath || '/dashboard';
          setRedirectPath(null); // Clear the redirect path
          console.log('AuthCallbackPage: Redirecting to:', targetPath);
          navigate(targetPath, { replace: true });
        } catch (err: any) { // Explicitly type err as any to access response property
          console.error('AuthCallbackPage: OAuth callback error during user fetch or login:', err);
          if (err.response) {
            console.error('AuthCallbackPage: Error response status:', err.response.status);
            console.error('AuthCallbackPage: Error response data:', err.response.data);
          }
          // Clear any partially stored tokens on error
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          setRedirectPath(null); // Clear redirect path on error
          navigate('/login', { state: { error: 'Authentication failed.' } });
        }
      } else if (error) {
        console.error('AuthCallbackPage: OAuth callback error from fragment:', error);
        setRedirectPath(null); // Clear redirect path on error
        navigate('/login', { state: { error: error || 'Authentication failed.' } });
      } else {
        // Fallback if no token or error in fragment
        console.warn('AuthCallbackPage: No access token or error found in fragment.');
        setRedirectPath(null); // Clear redirect path on error
        navigate('/login', { state: { error: 'Invalid authentication callback.' } });
      }
    };

    handleCallback();
  }, [location, navigate, login, redirectPath, setRedirectPath]); // Add redirectPath and setRedirectPath to dependencies

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <ProgressSpinner />
    </div>
  );
};

export default AuthCallbackPage;

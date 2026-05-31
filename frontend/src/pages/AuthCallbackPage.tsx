import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useAuthStore } from '../store/authStore';
// import { authApi } from '../api/authApi'; // authApi.callback is no longer needed here

const AuthCallbackPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((state) => state.login);

  useEffect(() => {
    const handleCallback = async () => {
      // API Gateway now redirects with token in URL fragment (#token=...)
      // The fragment is not sent to the server, so we parse it on the client side.
      const fragment = new URLSearchParams(location.hash.substring(1)); // Remove '#' and parse
      const token = fragment.get('token');
      const error = fragment.get('error'); // Check for error in fragment as well

      if (token) {
        try {
          // Assuming the token from API Gateway is the final AuthResponse needed by login store
          // We need to parse the JWT to get user info or make an additional call if needed
          // For simplicity, let's assume the token itself is enough for login(response.data)
          // and user info will be fetched by a separate mechanism (e.g., useMe hook on dashboard)
          // Or, if the token is a full AuthResponse JSON, we need to parse it.
          // Based on previous discussion, API Gateway returns Keycloak's access_token directly.
          // The authStore.login expects { accessToken, refreshToken, user }.
          // For now, we'll just use the accessToken and assume user info is fetched later.
          // This part might need refinement based on exact AuthResponse structure.

          // For now, let's assume the token is the accessToken and we don't have refreshToken or user info here.
          // This is a simplification. A more robust solution would involve:
          // 1. API Gateway returning accessToken AND refreshToken AND user info in the fragment.
          // 2. Or, making a /me API call after login to fetch user details.

          // Let's create a dummy AuthResponse structure for login()
          const dummyAuthResponse = {
            accessToken: token,
            refreshToken: null, // No refresh token in fragment
            user: null, // No user info in fragment
          };
          login(dummyAuthResponse, null); // Pass null for user for now, will be fetched by useMe
          navigate('/');
        } catch (err) {
          console.error('OAuth callback error:', err);
          navigate('/login', { state: { error: 'Authentication failed.' } });
        }
      } else if (error) {
        console.error('OAuth callback error from fragment:', error);
        navigate('/login', { state: { error: error || 'Authentication failed.' } });
      }
      else {
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

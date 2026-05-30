import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { ProgressSpinner } from 'primereact/progressspinner';

export default function AuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const login = useAuthStore((state) => state.login);
  const hasBeenCalled = useRef(false);

  useEffect(() => {
    if (hasBeenCalled.current) return;
    hasBeenCalled.current = true;

    const code = searchParams.get('code');
    if (code) {
      authApi.exchangeCode(code)
        .then(authData => {
          login(authData);
          navigate('/');
        })
        .catch(() => {
          navigate('/login?error=callback_failed');
        });
    } else {
      navigate('/login?error=no_code');
    }
  }, [searchParams, login, navigate]);

  return (
    <div className="flex justify-content-center align-items-center min-h-screen">
      <ProgressSpinner />
    </div>
  );
}

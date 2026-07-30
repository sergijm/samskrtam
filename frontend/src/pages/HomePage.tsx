import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';
import { PageButton } from '../components/common/buttons';

const HomePage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (isAuthenticated) {
    return null;
  }

        return (
    <div className="relative min-h-screen">
      {/* Фоновая картинка — по центру */}
      <img
        src="/bk-samskrtam.png"
        alt=""
        style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%) scale(2)',
          opacity: '0.9',
          pointerEvents: 'none',
        }}
      />

      {/* Контент поверх */}
      <div className="relative z-10 min-h-screen">
        <div className="absolute top-0 right-0 p-4">
          <Link to="/login">
            <PageButton variant="page-action" labelKey="auth.login" />
          </Link>
        </div>
        <div className="flex flex-column align-items-center pt-32 text-home">
          <h1 className="text-6xl font-bold mb-3">
            <br/>
            Akshara Mārga</h1>
          <p className="text-xl mb-5">Learn Sanskrit with interactive quizzes and tools.</p>
        </div>
      </div>
    </div>
  );
};

export default HomePage;


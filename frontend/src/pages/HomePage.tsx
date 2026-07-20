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
    <div
      className="relative min-h-screen bg-cover bg-center"
      style={{ backgroundImage: 'url(/bk-samskrtam.jpg)' }}
    >
      <div className="absolute top-0 right-0 p-4">
        <Link to="/login">
          <PageButton variant="page-action" labelKey="auth.login" />
        </Link>
      </div>
      <div className="flex flex-column align-items-center justify-content-center min-h-screen text-white">
        <h1 className="text-6xl font-bold mb-3">Akshara Mārga</h1>
        <p className="text-xl mb-5">Learn Sanskrit with interactive quizzes and tools.</p>
      </div>
    </div>
  );
};

export default HomePage;


import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore'; // Import useAuthStore

const HomePage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore(); // Get isAuthenticated status

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true }); // Redirect to dashboard if authenticated
    }
  }, [isAuthenticated, navigate]);

  if (isAuthenticated) {
    return null; // Render nothing while redirecting
  }

  return (
    <div
      className="relative min-h-screen bg-cover bg-center"
      style={{ backgroundImage: 'url(/bk-samskrtam.jpg)' }}
    >
      <div className="absolute top-0 right-0 p-4">
        <Link to="/login">
          <Button label={t('auth.login')} className="p-button-primary" />
        </Link>
      </div>
      {/* You can add more content here later if needed */}
      <div className="flex flex-column align-items-center justify-content-center min-h-screen text-white">
        {/* Example content */}
        <h1 className="text-6xl font-bold mb-3">Akshara Mārga</h1>
        <p className="text-xl mb-5">Learn Sanskrit with interactive quizzes and tools.</p>
      </div>
    </div>
  );
};

export default HomePage;

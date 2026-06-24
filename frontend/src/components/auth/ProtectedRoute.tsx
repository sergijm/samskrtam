import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated } = useAuthStore();
  const location = useLocation();

  const currentPath = location.pathname + location.search;

  console.log('ProtectedRoute:', currentPath);

  if (!isAuthenticated) {
    console.log('ProtectedRoute: User not authenticated. Saving redirect path:', currentPath);
    useAuthStore.getState().setRedirectPath(currentPath);

    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;

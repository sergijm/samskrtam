import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom'; // Import useLocation
import { useAuthStore } from '../../store/authStore';

interface ProtectedRouteProps {
  allowedRoles?: string[];
}

const ProtectedRoute = ({ allowedRoles }: ProtectedRouteProps) => {
  const { isAuthenticated, user, setRedirectPath } = useAuthStore(); // Get setRedirectPath
  const location = useLocation(); // Get current location

  if (!isAuthenticated) {
    // Save the current path before redirecting to login
    setRedirectPath(location.pathname + location.search);
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user) {
    const userHasRequiredRole = allowedRoles.some(role => user.roles.includes(role));
    if (!userHasRequiredRole) {
      // User is authenticated but does not have any of the required roles
      return <Navigate to="/" replace />;
    }
  }

  return <Outlet />;
};

export default ProtectedRoute;

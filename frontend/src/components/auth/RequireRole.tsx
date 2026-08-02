import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

interface RequireRoleProps {
  role: string;
  children: React.ReactNode;
}

/**
 * Гейт по роли для роутов, доступных только определённой роли (например ADMIN).
 * Проверка идёт по `user.roles` из authStore (аналогично ProtectedRoute по auth).
 */
const RequireRole: React.FC<RequireRoleProps> = ({ role, children }) => {
  const user = useAuthStore((s) => s.user);
  const hasRole = user?.roles?.includes(role) ?? false;

  if (!hasRole) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};

export default RequireRole;

import { Navigate } from 'react-router-dom';

// Временная заглушка. В будущем здесь будет проверка токена.
const isAuthenticated = true; 

export default function ProtectedRoute({ children }: { children: JSX.Element }) {
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

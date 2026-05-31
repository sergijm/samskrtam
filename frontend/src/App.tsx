import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PrimeReactProvider } from 'primereact/api';

// Pages
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import AuthCallbackPage from './pages/AuthCallbackPage';
import DashboardPage from './pages/DashboardPage';
import SettingsPage from './pages/SettingsPage';
import ChangePasswordPage from './pages/ChangePasswordPage';
import UserProfilePage from './pages/UserProfilePage';
import GroupListPage from './pages/GroupListPage';
import GroupCreatePage from './pages/GroupCreatePage';
import GroupPage from './pages/GroupPage';
import GroupEditPage from './pages/GroupEditPage';

// Components
import ProtectedRoute from './components/auth/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';

// i18n
import './i18n';

const queryClient = new QueryClient();

export default function App() {
  return (
    <PrimeReactProvider>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            {/* Public routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/auth/callback" element={<AuthCallbackPage />} />

            {/* Protected routes */}
            <Route path="/" element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route index element={<DashboardPage />} />
                <Route path="settings" element={<SettingsPage />} />
                <Route path="settings/password" element={<ChangePasswordPage />} />
                <Route path="users/:id" element={<UserProfilePage />} />
                <Route path="groups/:id" element={<GroupPage />} />
                
                {/* Admin only routes */}
                <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                  <Route path="groups" element={<GroupListPage />} />
                  <Route path="groups/new" element={<GroupCreatePage />} />
                  <Route path="groups/:id/edit" element={<GroupEditPage />} />
                  {/* <Route path="admin" element={<AdminPage />} /> */}
                </Route>
              </Route>
            </Route>

            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </PrimeReactProvider>
  );
}

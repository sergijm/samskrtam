import React, { useEffect } from 'react'; // Import useEffect
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PrimeReactProvider } from 'primereact/api';

// Pages
import HomePage from './pages/HomePage';
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
import AdminUsersPage from './pages/AdminUsersPage';
import AdminHomePage from './pages/AdminHomePage'; // Import AdminHomePage
import AdminGroupsPage from './pages/AdminGroupsPage'; // Import AdminGroupsPage
import QuizzesPage from './pages/QuizzesPage';
import QuizPage from './pages/QuizPage';
import UserStatisticsPage from './pages/UserStatisticsPage'; // Import UserStatisticsPage
import UserQuizSessionsPage from './pages/UserQuizSessionsPage'; // Import UserQuizSessionsPage
import SessionHistoryPage from './pages/SessionHistoryPage'; // Import SessionHistoryPage

// Components
import ProtectedRoute from './components/auth/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';

// Stores
import { useThemeStore } from './store/themeStore'; // Import useThemeStore

// i18n
import './i18n';

const queryClient = new QueryClient();

export default function App() {
  const { theme, setTheme } = useThemeStore(); // Get theme and setTheme from store

  useEffect(() => {
    // Apply the theme from the store on initial load
    setTheme(theme);
  }, [theme, setTheme]); // Rerun if theme or setTheme changes (though setTheme is stable)

  return (
    <PrimeReactProvider>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            {/* Public Home Page */}
            <Route path="/" element={<HomePage />} />

            {/* Public authentication routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/auth/callback" element={<AuthCallbackPage />} />

            {/* Protected routes */}
            <Route path="/" element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="dashboard" element={<DashboardPage />} />
                <Route path="settings" element={<SettingsPage />} />
                <Route path="settings/password" element={<ChangePasswordPage />} />
                <Route path="users/:id" element={<UserProfilePage />} />
                <Route path="quiz-sessions" element={<UserQuizSessionsPage />} /> {/* New route for user quiz sessions */}
                <Route path="quiz-sessions/:sessionId/history" element={<SessionHistoryPage />} /> {/* New route for session history */}
                <Route path="statistics" element={<UserStatisticsPage />} /> {/* New route for user statistics */}
                <Route path="groups/:id" element={<GroupPage />} />
                <Route path="quizzes/:category" element={<QuizzesPage />} />
                <Route path="quiz/grammar/:slug" element={<QuizPage />} />
                <Route path="quiz/vocabulary/:slug" element={<QuizPage />} />
                
                {/* Admin only routes */}
                <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                  <Route path="admin" element={<AdminHomePage />} /> {/* New Admin Home Page */}
                  <Route path="admin/users" element={<AdminUsersPage />} />
                  <Route path="admin/groups" element={<AdminGroupsPage />} /> {/* New Admin Groups Page */}
                  <Route path="groups" element={<GroupListPage />} />
                  <Route path="groups/new" element={<GroupCreatePage />} />
                  <Route path="groups/:id/edit" element={<GroupEditPage />} />
                </Route>
              </Route>
            </Route>

            {/* Redirect any unmatched routes to the home page or dashboard if authenticated */}
            <Route path="*" element={<Navigate to="/" />} />
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </PrimeReactProvider>
  );
}

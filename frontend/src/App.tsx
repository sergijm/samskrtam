import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PrimeReactProvider } from 'primereact/api';

// Components
import ErrorBoundary from './components/common/ErrorBoundary';
import ProtectedRoute from './components/auth/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';

// Pages
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
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
import AdminHomePage from './pages/AdminHomePage';
import AdminGroupsPage from './pages/AdminGroupsPage';
import QuizzesPage from './pages/QuizzesPage';
import QuizPage from './pages/QuizPage';
import UserStatisticsPage from './pages/UserStatisticsPage';
import UserQuizSessionsPage from './pages/UserQuizSessionsPage';
import SessionHistoryPage from './pages/SessionHistoryPage';
import GrammarPage from './pages/grammar/GrammarPage';
import VocabularyPage from './pages/vocabulary/VocabularyPage';
import VocabularyBasicPage from './pages/vocabulary/VocabularyBasicPage';
import VocabularyTextsPage from './pages/vocabulary/VocabularyTextsPage';
import UnderConstructionPage from './pages/UnderConstructionPage';
import EmeneauRulesPage from './pages/EmeneauRulesPage';
import EmeneauExercisesPage from './pages/eamenau/EmeneauExercisesPage';
import EmeneauExerciseDetailPage from './pages/eamenau/EmeneauExerciseDetailPage';
import DictionaryPage from './pages/dictionary/DictionaryPage';
import VocabularyLessonPage from './pages/lessons/VocabularyLessonPage';
import GrammarLessonPage from './pages/lessons/GrammarLessonPage';

// Stores
import { useThemeStore } from './store/themeStore';

// i18n
import './i18n';

const queryClient = new QueryClient();

export default function App() {
    const { theme, setTheme } = useThemeStore();

    useEffect(() => {
        setTheme(theme);
    }, [theme, setTheme]);

    return (
        <ErrorBoundary>
            <PrimeReactProvider>
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <Routes>
                            {/* Public routes */}
                            <Route path="/" element={<HomePage />} />
                            <Route path="/login" element={<LoginPage />} />
                            <Route path="/register" element={<RegisterPage />} />
                            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                            <Route path="/reset-password" element={<ResetPasswordPage />} />
                            <Route path="/auth/callback" element={<AuthCallbackPage />} />

                            {/* Protected routes */}
                            <Route
                                path="/dashboard"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <DashboardPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/settings"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <SettingsPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/settings/password"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <ChangePasswordPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/users/:id"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <UserProfilePage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/quiz-sessions"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <UserQuizSessionsPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/quiz-sessions/:sessionId/history"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <SessionHistoryPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/statistics"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <UserStatisticsPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/groups/:id"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <GroupPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            {/* Grammar routes */}
                            <Route
                                path="/grammar"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <GrammarPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/grammar/declensions"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <QuizzesPage category="declensions" />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/grammar/conjugations"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <UnderConstructionPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/grammar/emeneau-exercises"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <EmeneauExercisesPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/grammar/emeneau-exercises/:id"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <EmeneauExerciseDetailPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/grammar/emeneau-quizzes"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <UnderConstructionPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/grammar/emeneau-rules"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <EmeneauRulesPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            {/* Vocabulary quizzes */}
                            <Route
                                path="/quizzes/vocabulary"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <VocabularyPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="quizzes/vocabulary/basic"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <VocabularyBasicPage/>
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="quizzes/vocabulary/texts"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <VocabularyTextsPage/>
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />


                            {/* Vocabulary main page */}
                            <Route
                                path="/vocabulary"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <VocabularyPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            {/* Lesson pages */}
                            <Route
                                path="/lessons/vocabulary/:slug"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <VocabularyLessonPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/lessons/grammar/:type"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <GrammarLessonPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            <Route
                                path="/quiz/:quizCategory/:slug/:sessionId?"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <QuizPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            {/* Dictionary route */}
                            <Route
                                path="/dictionary"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <DictionaryPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            {/* Admin only routes */}
                            <Route
                                path="/admin"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <AdminHomePage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/admin/users"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <AdminUsersPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/admin/groups"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <AdminGroupsPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/groups"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <GroupListPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/groups/new"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <GroupCreatePage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />
                            <Route
                                path="/groups/:id/edit"
                                element={
                                    <ProtectedRoute>
                                        <AppLayout>
                                            <GroupEditPage />
                                        </AppLayout>
                                    </ProtectedRoute>
                                }
                            />

                            {/* Redirect any unmatched routes */}
                            <Route path="*" element={
                                <ProtectedRoute>
                                    <Navigate to="/" />
                                </ProtectedRoute>
                            } />
                        </Routes>
                    </BrowserRouter>
                </QueryClientProvider>
            </PrimeReactProvider>
        </ErrorBoundary>
    );
}
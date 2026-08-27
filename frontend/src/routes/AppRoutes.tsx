import { Routes, Route, Navigate } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import PageLoader from '../components/common/PageLoader';

// Pages
const HomePage = lazy(() => import('../pages/HomePage'));
const LoginPage = lazy(() => import('../pages/LoginPage'));
const RegisterPage = lazy(() => import('../pages/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('../pages/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('../pages/ResetPasswordPage'));
const AuthCallbackPage = lazy(() => import('../pages/AuthCallbackPage'));
const LearnGraphPage = lazy(() => import('../pages/LearnGraphPage'));
const SettingsPage = lazy(() => import('../pages/SettingsPage'));
const ChangePasswordPage = lazy(() => import('../pages/ChangePasswordPage'));
const UserProfilePage = lazy(() => import('../pages/UserProfilePage'));
const GroupListPage = lazy(() => import('../pages/GroupListPage'));
const GroupCreatePage = lazy(() => import('../pages/GroupCreatePage'));
const GroupPage = lazy(() => import('../pages/GroupPage'));
const GroupEditPage = lazy(() => import('../pages/GroupEditPage'));
const AdminUsersPage = lazy(() => import('../pages/AdminUsersPage'));
const AdminHomePage = lazy(() => import('../pages/AdminHomePage'));
const AdminGroupsPage = lazy(() => import('../pages/AdminGroupsPage'));
const QuizzesPage = lazy(() => import('../pages/QuizzesPage'));
const QuizPage = lazy(() => import('../pages/QuizPage'));
const SessionHistoryPage = lazy(() => import('../pages/SessionHistoryPage'));
const GrammarPage = lazy(() => import('../pages/grammar/GrammarPage'));
const LexiconPage = lazy(() => import('../pages/lexicon/LexiconPage'));
const VocabularyPage = lazy(() => import('../pages/vocabulary/VocabularyPage'));
const VocabularyBasicPage = lazy(() => import('../pages/vocabulary/VocabularyBasicPage'));
const VocabularyTextsPage = lazy(() => import('../pages/vocabulary/VocabularyTextsPage'));
const UnderConstructionPage = lazy(() => import('../pages/UnderConstructionPage'));
const EmeneauRulesPage = lazy(() => import('../pages/EmeneauRulesPage'));
const EmeneauExercisesPage = lazy(() => import('../pages/eamenau/EmeneauExercisesPage'));
const EmeneauExerciseDetailPage = lazy(() => import('../pages/eamenau/EmeneauExerciseDetailPage'));
const DictionaryPage = lazy(() => import('../pages/dictionary/DictionaryPage'));
const AlphabetPage = lazy(() => import('../pages/writing/AlphabetPage'));
const TransliterationPracticePage = lazy(() => import('../pages/tools/TransliterationPracticePage'));
const VocabularyLessonPage = lazy(() => import('../pages/lessons/VocabularyLessonPage'));
const GrammarRouteResolver = lazy(() => import('../pages/lessons/GrammarRouteResolver'));
const SandhiLessonPage = lazy(() => import('../pages/lessons/SandhiLessonPage'));
const GrammarAllStemsPage = lazy(() => import('../pages/lessons/GrammarAllStemsPage'));
const WorksPage = lazy(() => import('../pages/sangraha/WorksPage'));
const WorkPage = lazy(() => import('../pages/sangraha/WorkPage'));
const ChapterPage = lazy(() => import('../pages/sangraha/ChapterPage'));
const VersePage = lazy(() => import('../pages/sangraha/VersePage'));
const VersesBatchPage = lazy(() => import('../pages/sangraha/VersesBatchPage'));
const AnalysisPage = lazy(() => import('../pages/analysis/AnalysisPage'));

// Components
import ProtectedRoute from '../components/auth/ProtectedRoute';
import RequireRole from '../components/auth/RequireRole';
import AppLayout from '../components/layout/AppLayout';
import { ReactNode } from 'react';

/** Helper: защищённый маршрут с общим лейаутом */
function ProtectedLayoutRoute({ children, hideSidebar }: { children: ReactNode; hideSidebar?: boolean }) {
  return (
    <ProtectedRoute>
      <AppLayout hideSidebar={hideSidebar}>
        {children}
      </AppLayout>
    </ProtectedRoute>
  );
}

/** Helper: защищённый маршрут + роль (ADMIN-only страницы) */
function RoleLayoutRoute({ role, children }: { role: string; children: ReactNode }) {
  return (
    <ProtectedRoute>
      <RequireRole role={role}>
        <AppLayout>
          {children}
        </AppLayout>
      </RequireRole>
    </ProtectedRoute>
  );
}

export default function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/auth/callback" element={<AuthCallbackPage />} />

      {/* Protected routes */}
      <Route path="/dashboard" element={<ProtectedLayoutRoute hideSidebar><LearnGraphPage /></ProtectedLayoutRoute>} />
      <Route path="/settings" element={<ProtectedLayoutRoute><SettingsPage /></ProtectedLayoutRoute>} />
      <Route path="/settings/password" element={<ProtectedLayoutRoute><ChangePasswordPage /></ProtectedLayoutRoute>} />
      <Route path="/users/:id" element={<ProtectedLayoutRoute><UserProfilePage /></ProtectedLayoutRoute>} />
      <Route path="/quiz-sessions/:sessionId/history" element={<ProtectedLayoutRoute><SessionHistoryPage /></ProtectedLayoutRoute>} />
      <Route path="/groups/:id" element={<ProtectedLayoutRoute><GroupPage /></ProtectedLayoutRoute>} />
      <Route path="/groups" element={<ProtectedLayoutRoute><GroupListPage /></ProtectedLayoutRoute>} />
      <Route path="/groups/new" element={<ProtectedLayoutRoute><GroupCreatePage /></ProtectedLayoutRoute>} />
      <Route path="/groups/:id/edit" element={<ProtectedLayoutRoute><GroupEditPage /></ProtectedLayoutRoute>} />

      {/* Grammar routes */}
      <Route path="/grammar" element={<ProtectedLayoutRoute><GrammarPage /></ProtectedLayoutRoute>} />
      <Route path="/grammar/declensions" element={<ProtectedLayoutRoute><QuizzesPage category="declensions" /></ProtectedLayoutRoute>} />
      <Route path="/grammar/conjugations" element={<ProtectedLayoutRoute><UnderConstructionPage /></ProtectedLayoutRoute>} />
      <Route path="/grammar/emeneau-exercises" element={<ProtectedLayoutRoute><EmeneauExercisesPage /></ProtectedLayoutRoute>} />
      <Route path="/grammar/emeneau-exercises/:id" element={<ProtectedLayoutRoute><EmeneauExerciseDetailPage /></ProtectedLayoutRoute>} />
      <Route path="/grammar/emeneau-quizzes" element={<ProtectedLayoutRoute><UnderConstructionPage /></ProtectedLayoutRoute>} />
      <Route path="/grammar/emeneau-rules" element={<ProtectedLayoutRoute><EmeneauRulesPage /></ProtectedLayoutRoute>} />

      {/* Vocabulary pages */}
      <Route path="/vocabulary" element={<ProtectedLayoutRoute><VocabularyPage /></ProtectedLayoutRoute>} />

      {/* Lexicon (стартовая страница лексики) */}
      <Route path="/lexicon" element={<ProtectedLayoutRoute hideSidebar><LexiconPage /></ProtectedLayoutRoute>} />
      <Route path="/vocabulary/lists" element={<ProtectedLayoutRoute><UnderConstructionPage /></ProtectedLayoutRoute>} />
      <Route path="/vocabulary/basic" element={<ProtectedLayoutRoute><VocabularyBasicPage /></ProtectedLayoutRoute>} />
      <Route path="/vocabulary/texts" element={<ProtectedLayoutRoute><VocabularyTextsPage /></ProtectedLayoutRoute>} />

            {/* Lesson pages */}
      <Route path="/lessons/grammar/declensions-all" element={<ProtectedLayoutRoute><GrammarAllStemsPage /></ProtectedLayoutRoute>} />
      <Route path="/lessons/grammar/sandhi-vowels-external" element={<ProtectedLayoutRoute><SandhiLessonPage /></ProtectedLayoutRoute>} />
      <Route path="/lessons/grammar/sandhi-consonants" element={<ProtectedLayoutRoute><SandhiLessonPage /></ProtectedLayoutRoute>} />
      <Route path="/lessons/grammar/sandhi-visarga" element={<ProtectedLayoutRoute><SandhiLessonPage /></ProtectedLayoutRoute>} />
      <Route path="/lessons/grammar/:slug" element={<ProtectedLayoutRoute><GrammarRouteResolver /></ProtectedLayoutRoute>} />
      <Route path="/lessons/vocabulary/:slug" element={<ProtectedLayoutRoute><VocabularyLessonPage /></ProtectedLayoutRoute>} />

      {/* Quiz page */}
      <Route path="/quiz/:quizCategory/:slug/:sessionId?" element={<ProtectedLayoutRoute><QuizPage /></ProtectedLayoutRoute>} />

            {/* Sangraha routes */}
      <Route path="/sangraha" element={<ProtectedLayoutRoute><WorksPage /></ProtectedLayoutRoute>} />
      <Route path="/sangraha/verses" element={<RoleLayoutRoute role="ADMIN"><VersesBatchPage /></RoleLayoutRoute>} />
      <Route path="/sangraha/:workSlug" element={<ProtectedLayoutRoute><WorkPage /></ProtectedLayoutRoute>} />
      <Route path="/sangraha/:workSlug/chapters/:chapterId" element={<ProtectedLayoutRoute><ChapterPage /></ProtectedLayoutRoute>} />
      <Route path="/sangraha/:workSlug/verses/:verseId" element={<ProtectedLayoutRoute><VersePage /></ProtectedLayoutRoute>} />

      {/* Dictionary route */}
      <Route path="/dictionary" element={<ProtectedLayoutRoute><DictionaryPage /></ProtectedLayoutRoute>} />

      {/* Admin only routes */}
      <Route path="/admin" element={<ProtectedLayoutRoute><AdminHomePage /></ProtectedLayoutRoute>} />
      <Route path="/admin/users" element={<ProtectedLayoutRoute><AdminUsersPage /></ProtectedLayoutRoute>} />
      <Route path="/admin/groups" element={<ProtectedLayoutRoute><AdminGroupsPage /></ProtectedLayoutRoute>} />

      {/* Writing routes */}
      <Route path="/alphabet" element={<ProtectedLayoutRoute><AlphabetPage /></ProtectedLayoutRoute>} />
      <Route path="/writing/alphabet" element={<ProtectedLayoutRoute><AlphabetPage /></ProtectedLayoutRoute>} />
      <Route path="/writing/transliteration" element={<ProtectedLayoutRoute><TransliterationPracticePage /></ProtectedLayoutRoute>} />

      {/* Analysis routes */}
      <Route path="/analysis" element={<ProtectedLayoutRoute><AnalysisPage /></ProtectedLayoutRoute>} />

      {/* Catch-all */}
      <Route path="*" element={<ProtectedRoute><Navigate to="/" /></ProtectedRoute>} />
    </Routes>
    </Suspense>
  );
}
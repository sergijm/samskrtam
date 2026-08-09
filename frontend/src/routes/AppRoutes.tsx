import { Routes, Route, Navigate } from 'react-router-dom';

// Pages
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import ForgotPasswordPage from '../pages/ForgotPasswordPage';
import ResetPasswordPage from '../pages/ResetPasswordPage';
import AuthCallbackPage from '../pages/AuthCallbackPage';
import LearnGraphPage from '../pages/LearnGraphPage';
import SettingsPage from '../pages/SettingsPage';
import ChangePasswordPage from '../pages/ChangePasswordPage';
import UserProfilePage from '../pages/UserProfilePage';
import GroupListPage from '../pages/GroupListPage';
import GroupCreatePage from '../pages/GroupCreatePage';
import GroupPage from '../pages/GroupPage';
import GroupEditPage from '../pages/GroupEditPage';
import AdminUsersPage from '../pages/AdminUsersPage';
import AdminHomePage from '../pages/AdminHomePage';
import AdminGroupsPage from '../pages/AdminGroupsPage';
import QuizzesPage from '../pages/QuizzesPage';
import QuizPage from '../pages/QuizPage';
import UserStatisticsPage from '../pages/UserStatisticsPage';
import UserQuizSessionsPage from '../pages/UserQuizSessionsPage';
import SessionHistoryPage from '../pages/SessionHistoryPage';
import GrammarPage from '../pages/grammar/GrammarPage';
import LexiconPage from '../pages/lexicon/LexiconPage';
import VocabularyPage from '../pages/vocabulary/VocabularyPage';
import VocabularyBasicPage from '../pages/vocabulary/VocabularyBasicPage';
import VocabularyTextsPage from '../pages/vocabulary/VocabularyTextsPage';
import UnderConstructionPage from '../pages/UnderConstructionPage';
import EmeneauRulesPage from '../pages/EmeneauRulesPage';
import EmeneauExercisesPage from '../pages/eamenau/EmeneauExercisesPage';
import EmeneauExerciseDetailPage from '../pages/eamenau/EmeneauExerciseDetailPage';
import DictionaryPage from '../pages/dictionary/DictionaryPage';
import AlphabetPage from '../pages/writing/AlphabetPage';
import TransliterationPracticePage from '../pages/tools/TransliterationPracticePage';
import VocabularyLessonPage from '../pages/lessons/VocabularyLessonPage';
import GrammarLessonPage from '../pages/lessons/GrammarLessonPage';
import GrammarAllStemsPage from '../pages/lessons/GrammarAllStemsPage';
import WorksPage from '../pages/sangraha/WorksPage';
import WorkPage from '../pages/sangraha/WorkPage';
import ChapterPage from '../pages/sangraha/ChapterPage';
import VersePage from '../pages/sangraha/VersePage';
import VersesBatchPage from '../pages/sangraha/VersesBatchPage';
import AnalysisPage from '../pages/analysis/AnalysisPage';

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
      <Route path="/quiz-sessions" element={<ProtectedLayoutRoute><UserQuizSessionsPage /></ProtectedLayoutRoute>} />
      <Route path="/quiz-sessions/:sessionId/history" element={<ProtectedLayoutRoute><SessionHistoryPage /></ProtectedLayoutRoute>} />
      <Route path="/statistics" element={<ProtectedLayoutRoute><UserStatisticsPage /></ProtectedLayoutRoute>} />
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
      <Route path="/lessons/vocabulary/:slug" element={<ProtectedLayoutRoute><VocabularyLessonPage /></ProtectedLayoutRoute>} />
      <Route path="/lessons/grammar/:slug" element={<ProtectedLayoutRoute><GrammarLessonPage /></ProtectedLayoutRoute>} />

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
  );
}
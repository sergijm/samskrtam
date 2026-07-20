# Аудит кнопок вне таблиц — фронтенд SamskrtamApp

> Агент 3 (Frontend Agent) · Дата: 2025
> Результат: все кнопки вне `<DataTable>` каталогизированы и отображены на варианты `PageButton`.

---

## Сводка

| Вариант | Количество | Описание |
|---|---|---|
| `page-action` | 5 | Главное действие страницы (Создать, Добавить) |
| `form-submit` | 10 | Отправка формы (type="submit") |
| `form-cancel` | 5 | Отмена в форме (p-button-text) |
| `icon-only` | 10 | Только иконка (toggle, logout, delete, edit) |
| `filter-reset` | 2 | Сброс фильтров (pi-filter-slash + outlined) |
| `navigation` | 2 | Переход на другой маршрут |
| `cta-primary` | 5 | Большой CTA |
| `danger` | 3 | Деструктивное действие |
| `dialog-action` | 11 | Кнопки внутри Dialog.footer |

**Всего: ~53 кнопки**

---

## Постраничный каталог

### 1. HomePage
- `page-action` — «Login» → `/login`

### 2. LoginPage
- `cta-primary` — «Google» (social)
- `cta-primary` — «Mail.ru» (social)
- `navigation` — «Login with Password»
- `form-submit` — «Login»
- `form-cancel` — «Back»

### 3. RegisterPage
- `form-submit` — «Register»

### 4. ForgotPasswordPage
- `form-submit` — «Send reset link»

### 5. ResetPasswordPage
- `form-submit` — «Reset password»

### 6. ChangePasswordPage
- `form-submit` — «Change password»

### 7. DashboardPage
- `cta-primary` — «Continue» (ContinueCta)
- `navigation` — «Progress Map» (ProgressMapLink)
- `navigation` — WeakSpots practice links
- `navigation` — ReadingPath open text

### 8. SettingsPage
- `form-submit` — «Save»
- `navigation` — «Change password» (Link styled as button)

### 9. Header (layout)
- `icon-only` — Toggle sidebar
- `icon-only` — Logout

### 10. GrammarPage — нет кнопок (только карточки-ссылки)

### 11. GrammarLessonPage — нет кнопок вне таблиц

### 12. VocabularyLessonPage — нет кнопок вне таблиц

### 13. QuizPage — нет кнопок вне таблиц

### 14. QuizQuestionPanel
- `form-submit` — Выбор варианта ответа (RadioButton/Button)

### 15. QuizCaseSelectPanel
- `form-submit` — Выбор варианта (RadioButton)

### 16. QuizEndingMatchPanel
- `form-submit` — «Submit»
- `form-submit` — Выбор вариантов (Checkbox)

### 17. QuizFeedbackPanel
- `cta-primary` — «Next» / «Complete Quiz»

### 18. QuizSessionFilters
- `filter-reset` — Сброс фильтров

### 19. SessionHistoryActions
- `cta-primary` — «Resume»
- `navigation` — «Retake»
- `page-action` — «Start New»

### 20. AdminUsersPage
- `filter-reset` — Сброс фильтров

### 21. AdminGroupsPage
- `page-action` — «Create group» (в header таблицы)

### 22. GroupListPage
- `page-action` — «Create group» (в header таблицы)

### 23. GroupPage
- `page-action` — «Add member»
- `icon-only` — «Edit» (карандаш)
- `icon-only` — «Check» (подтверждение переименования)
- `form-cancel` — «Times» (отмена переименования)

### 24. GroupCreatePage
- `form-submit` — «Create»
- `form-cancel` — «Cancel»

### 25. GroupEditPage
- `form-submit` — «Save»
- `form-cancel` — «Cancel»

### 26. DictionaryPage
- `page-action` — «Search» (иконка в InputGroup)

### 27. EmeneauRulesPage
- `navigation` — Кнопки пагинации страниц

### 28. EmeneauExerciseDetailPage
- `icon-only` — «Prev» (chevron-left)
- `icon-only` — «Up» (chevron-up)
- `icon-only` — «Next» (chevron-right)

### 29. WorksPage (sangraha)
- `page-action` — «Add Work»

### 30. WorkPage (sangraha)
- `icon-only` — «Back» (arrow-left)
- `page-action` — «Edit» (pencil)
- `page-action` — «Add Chapter»

### 31. VersePage (sangraha)
- `icon-only` — «Back» (arrow-left)
- `cta-primary` — «Analyze» (robot, success)
- `page-action` — «Edit» (pencil)

### 32. WorkCard (sangraha)
- `icon-only` — «Delete» (trash, danger)

### 33. ChapterTreeBrowser
- `icon-only` — «Add verse» (plus)
- `icon-only` — «Delete chapter» (trash, danger)
- `icon-only` — «Delete verse» (trash, danger)

### 34. WorkFormDialog
- `dialog-action` — «Create»

### 35. DeleteConfirmDialog
- `dialog-action` — «Cancel»
- `dialog-action` — «Delete» (danger)

### 36. ChapterDialog
- `dialog-action` — «Cancel»
- `dialog-action` — «Save»

### 37. VerseDialog
- `dialog-action` — «Cancel»
- `dialog-action` — «Save»

### 38. WorkEditDialog
- `dialog-action` — «Save»

### 39. AddMemberDialog
- `dialog-action` — «Cancel»
- `dialog-action` — «Add»

### 40. LessonStatsTab
- `cta-primary` — «Study» / «Continue» / «Review»

### 41. AvatarUploadSection
- `page-action` — «Upload» (outlined)

### 42. SessionsTab (внутри таблицы)
- `cta-primary` — «Continue» (в колонке действий таблицы — НЕ выделяем)

---

## Компоненты БЕЗ кнопок вне таблиц

- AppLayout, Sidebar, LocaleSwitcher, ThemeSwitcher, PreferencesSection, ProfileFieldsSection
- LessonHeader, LessonStatsBadges, LessonStatusSummary, WordStatusIcon
- UserAvatar, UserGroupChips, GroupCuratorBadge, GroupMembersTable
- WorkCard (кнопка только в админском footer)
- SessionHistoryTable, QuizSessionsTable, CaseAggregationTable, NumberAggregationTable, GrammarDetailsTable
- SandhiSplitsList, VerseWordsList, VerseEditor, SolutionPanel
- ProtectedRoute, ErrorBoundary, AuthCallbackPage, UnderConstructionPage
- UserProfilePage, UserStatisticsPage, SessionHistoryPage, UserQuizSessionsPage
- CategoryTiles, StreakProgress (нет кнопок)

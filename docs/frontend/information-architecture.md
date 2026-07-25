# UX и информационная архитектура платформы

> Модуль: `frontend/` (+ форвард-требования к content-service, statistics-service, sangraha-service)
> Status: **DRAFT**
> Связанные файлы: [frontend-overview.md](frontend-overview.md) · [lesson-pages-spec.md](pages/lesson-pages-spec.md) · [content-service.md](../services/content-service.md) · [sangraha-service.md](../services/sangraha-service.md) · [statistics-service.md](../services/statistics-service.md)

---

Документ полностью заменяет предыдущую (неформальную) версию обсуждения
IA — объединяет структуру меню, каталожные разделы, онбординг, дашборд,
страницу урока, личные списки слов, карту прогресса и вопрос источников
текстов. Разбит на файлы по паттерну «индекс + подпапка»
(`docs/conventions.md` §9), т.к. тема естественно распадается на
подтемы и общий объём превышает удобный для одного файла размер.

## Содержание

| Файл | Содержание |
|---|---|
| [information-architecture/01-curriculum-vs-catalog.md](information-architecture/01-curriculum-vs-catalog.md) | §1 Ключевой принцип: курикулум vs каталог · §2 Дерево курикулума: §2.1 Грамматика (родитель) · §2.2 Письмо и произношение · §2.3 Лексика |
| [information-architecture/02-catalog.md](information-architecture/02-catalog.md) | §3 Каталожные разделы: лексика (браузер словаря, личные списки), тексты (библиотека) |
| [information-architecture/03-onboarding-dashboard.md](information-architecture/03-onboarding-dashboard.md) | §4 Онбординг первого захода · §5 Dashboard |
| [information-architecture/04-progress-map.md](information-architecture/04-progress-map.md) | §6 Карта прогресса и формула агрегации `successRate` · §8 Почему не единая диаграмма навыков |
| [information-architecture/05-text-sources.md](information-architecture/05-text-sources.md) | §7 Тексты для уроков: источники и авторские права |
| [information-architecture/06-open-questions.md](information-architecture/06-open-questions.md) | §9 Открытые вопросы, требующие решения перед реализацией |

## Как этим пользоваться агентам

- **Агент 3 (Frontend):** основной потребитель §1, §2, §3, §4, §5, §6 —
  дерево курикулума в `Sidebar.tsx`, каталожные страницы, `DashboardPage`,
  полноэкранная карта прогресса. Конкретные задачи см.
  `docs/agents/tasks/task-01-curriculum-tree.md` и
  `docs/agents/tasks/task-02-dashboard.md`.
- **Агент 2 (Domain Services):** §3.1 (модель `user_word_lists` /
  `user_word_list_items` в content-service, режим выбора основ квиза из
  списка), §3.2 (`source_ref`/адрес строфы, поиск по сегментированному
  тексту — sangraha-service/content-service), §6 (формула агрегации
  `successRate` — новая логика в statistics-service, ещё не
  реализована), §7 (потенциальное поле `license`/`source_type` в модели
  произведения sangraha-service).
- **Агент 6 (Contracts):** контракты для новых endpoint'ов дашборда,
  каталога словаря/текстов и карты прогресса — до начала работы
  Агента 3, см. открытые вопросы §9 и в самих задачах фронтенда.

## Статус относительно кода

Обозначения ✅/⛔ в §2 (дерево курикулума) отражают состояние на момент
черновика v2 и должны сверяться с фактическим содержимым
content-service (`Quiz`/`VocabularyCategory`) при реализации — сам файл
не является источником истины по факту наличия урока в БД, а только по
целевой структуре меню.

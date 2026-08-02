# Задача: страница массового просмотра стихов + кнопка на вкладке «Примеры»

> Оркестратор: Агент 0. Контракт: Агент 6 (см. `docs/services/sangraha-service/
> batch-verse-review.md` и `docs/frontend/pages/grammar-lesson-page.md` §2.2а —
> источник истины для задач ниже). Зависит от `task-verse-batch-endpoints.md`
> (бэкенд-эндпоинты `GET /verse`/`POST /verse/analysis`) и от изменения
> контракта `GET /content/public/lessons/{slug}/examples` (поле
> `missingVerseIds`, см. `content-service/declension-examples.md` — если это
> изменение ещё не реализовано на content-service, задачу F3 ниже можно делать
> позже отдельным PR).
>
> Задачи разбиты на шаги ~30 сек работы модели (DeepSeek V4 Flash / Qwen3 Coder
> 30B A3B Instruct) — каждый шаг самодостаточен.

---

## Агент 3 — Frontend, sangraha-раздел

**F1. API-клиент и хуки.** В `frontend/src/api/sangraha.ts` — новые методы:
`getVersesBatch(ids: string[])` → `GET /api/v1/sangraha/verse?id=...&id=...`
(собрать query-строку вручную или через `URLSearchParams` с повторным `id`);
`analyzeVerses(verseIds: string[])` → `POST /api/v1/sangraha/verse/analysis` с
телом `{ verseIds }`. В `frontend/src/hooks/useSangraha.ts` — `useVersesBatch
(ids: string[])` (React Query, `queryKey: ['sangraha', 'verse-batch', ids]`,
`enabled: ids.length > 0`) и `useAnalyzeVerses()` (мутация по аналогии с
`useAnalyzeAllVerses`, `onSuccess` — `invalidateQueries({ queryKey: ['sangraha',
'verse-batch'] })`, без указания конкретных `ids` в ключе инвалидации, чтобы
задело любой открытый список).

**F2. Страница `VersesBatchPage`.** Новый файл
`frontend/src/pages/sangraha/VersesBatchPage.tsx`, роут `/sangraha/verses`
(добавить в роутер, ADMIN-guard — по аналогии с тем, как уже гейтятся
ADMIN-only роуты в проекте, найти существующий паттерн, например
`RequireRole`/аналог). Читает список id через `useSearchParams().getAll('id')`,
вызывает `useVersesBatch(ids)`. Таблица (PrimeReact `DataTable`, как на других
страницах sangraha): колонки — произведение/глава (`workTitleRu`/
`chapterTitleRu` по локали), номер стиха (`verseOrderIndex`), превью текста
(`textIastPreview`), статус (переиспользовать `STATUS_COLORS`/иконки из
`ChapterPage.tsx` — не дублировать маппинг). Клик по строке —
`navigate(`/sangraha/${row.workSlug}/verses/${row.id}`)`. Кнопка
«Анализировать все» сверху таблицы — всегда активна (не только при наличии
DRAFT/FAILED, см. `batch-verse-review.md`), по клику —
`useAnalyzeVerses().mutate(ids)` (весь список из query-параметров).

**F3. Кнопка на вкладке «Примеры» урока склонений.** В компоненте
`DeclensionExamplesTab` (content-service-фронтенд, см.
`grammar-lesson-page.md` §2.2а) — если ответ `GET /lessons/{slug}/examples`
содержит непустой `missingVerseIds` (поле есть только для роли `ADMIN` — если
пользователь не ADMIN, поля не будет вовсе, проверка `response.missingVerseIds
?.length > 0`), над списком групп рендерить кнопку
`«Проанализировать недостающие примеры ({missingVerseIds.length})»`. По клику
— `navigate('/sangraha/verses?' + missingVerseIds.map(id => `id=${id}`).join
('&'))` (обычный переход внутри того же SPA, не открытие в новой вкладке).

**F4. i18n.** Новые строки (`ru`/`en` locale-файлы, найти существующий
namespace для sangraha-страниц): заголовок страницы `/sangraha/verses`
(например «Массовый просмотр стихов» / «Bulk verse review»), текст кнопки
«Анализировать все» (если ещё нет общей строки — на `ChapterPage.tsx` она уже
есть, `sangraha.action.analyzeAll`, переиспользовать тот же ключ), текст
кнопки «Проанализировать недостающие примеры» на вкладке «Примеры» (новый
ключ, например `grammar.examples.analyzeMissing`).

---

## Критерии готовности

- [ ] F1: хуки/API-клиент готовы, `useVersesBatch`/`useAnalyzeVerses`
- [ ] F2: страница `/sangraha/verses` работает, ADMIN-only, клик по строке
      уводит на существующую `VersePage`, кнопка «Анализировать все» шлёт
      весь список id из query-параметров безусловно
- [ ] F3: кнопка на вкладке «Примеры» появляется только когда есть
      `missingVerseIds` (и только у ADMIN), ведёт на `/sangraha/verses` с
      правильным списком id
- [ ] F4: обе локали (ru/en) заполнены, старые ключи не задублированы

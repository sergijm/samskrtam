# Задача: `nominal_lemmas` — замена `noun_stems`, классификация на уровне леммы

> Оркестратор: Агент 0. Контракт: Агент 6 (см. `docs/services/sangraha-service/
> verse-word-grammar.md` §1а/§1б и `docs/services/sangraha-service.md` §9 —
> источник истины для задач ниже). Затронут только sangraha-service.
>
> **Заменяет `task-noun-stems-table.md` — та задача уже выполнена** (миграция
> `V3__create_noun_stems.sql` + JPA применены), но дизайн `noun_stems` признан
> неоптимальным (дублирование классификации на каждое вхождение слова вместо
> одной строки на лемму) и заменяется `nominal_lemmas`. `noun_stems` **не
> удаляется** ни в БД, ни в Java-коде — остаётся для отката/сверки, но поиск
> примеров переключается на новую таблицу.
>
> Задачи разбиты на шаги ~30 сек работы модели (DeepSeek V4 Flash / Qwen3 Coder
> 30B A3B Instruct) — каждый шаг самодостаточен.

---

## Агент 2 — Backend (sangraha-service)

**B1. Применить миграцию `V4__replace_noun_stems_with_nominal_lemmas.sql`**
(SQL прислан оркестратору пользователем целиком, применить как есть — файл не
воспроизводится здесь текстом ради читаемости доки, содержимое ниже описано
по частям, с одним изменением из B2). Миграция в транзакции (`BEGIN`/`COMMIT`):
создаёт таблицу `sangraha.nominal_lemmas` (поля — см. `verse-word-grammar.md`
§1б: id BIGSERIAL PK, lemma_iast TEXT NOT NULL UNIQUE, stem_iast/stem_class/
model TEXT nullable, confidence TEXT nullable, created_at/updated_at
TIMESTAMPTZ NOT NULL DEFAULT now()); создаёт индексы на `stem_class` и
`confidence`; переносит данные из `noun_stems` в `nominal_lemmas` одним
`INSERT ... SELECT DISTINCT ON (verse_words.lemma_iast)` (join `noun_stems` →
`verse_words` по `verse_word_id`, дедупликация по `lemma_iast`, при нескольких
кандидатах на одну лемму — предпочтение строке с наивысшим `confidence`
через `CASE WHEN confidence = 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW'
THEN 3 ELSE 4 END` в `ORDER BY`); проверяет консистентность блоком `DO $$ ...
END $$` — сравнивает `COUNT(DISTINCT lemma_iast)` в исходных данных с
`COUNT(*)` в новой таблице и прерывает миграцию через `RAISE EXCEPTION`, если
новых строк больше, чем различных лемм в источнике (сигнал дублирования).
`id` — `BIGSERIAL` (единственная числовая PK в схеме sangraha, остальные —
`UUID`; сознательное отклонение, не исправлять без отдельного решения).
`noun_stems` таблица в этой миграции **не удаляется** — сохранена для
отката/сверки.

**B2. Добавить к присланному SQL (перед `COMMIT`):**
- `CONSTRAINT ck_nominal_lemma_confidence CHECK (confidence IS NULL OR confidence IN ('HIGH', 'MEDIUM', 'LOW'))`
  на `nominal_lemmas` (добавить прямо в `CREATE TABLE`, как отдельный `CONSTRAINT` внутри скобок) —
  `stem_class` намеренно без `CHECK` (см. `verse-word-grammar.md` §1б — задел под
  будущие части речи), но `confidence` — устоявшийся в проекте паттерн `HIGH|
  MEDIUM|LOW`, стоит валидировать.
- `CREATE INDEX idx_verse_words_lemma_iast ON sangraha.verse_words(lemma_iast);`
  — индекса на этой колонке ещё нет (в `V1__create_schema.sql` не создавался),
  а после этой миграции она становится ключом join'а при каждом поиске
  примеров (см. B4) — без индекса это seq scan по `verse_words`.
- Явные имена constraint'ов по конвенции проекта (`pk_nominal_lemmas`,
  `uq_nominal_lemmas_lemma_iast`) вместо implicit-имён у `PRIMARY KEY`/`UNIQUE`.

**B3. JPA-сущность `NominalLemma`.** Новый класс в пакете `model/`. Поля: id
(Long, `@Id`, `@GeneratedValue(strategy = IDENTITY)` — под `BIGSERIAL`),
lemmaIast (String), stemIast (String, nullable), stemClass (String, nullable
— не энум, так как без `CHECK` намеренно открытый набор значений, ремапить в
Java-энум `StemClass` не нужно, эту сущность трактовать как "сырые" данные),
confidence (переиспользовать существующий `AnalysisConfidence` энум, nullable),
model (String, nullable), createdAt/updatedAt (Instant/OffsetDateTime). **Без**
`@ManyToOne`/`@OneToMany` к `VerseWord` — связи по факту нет (см. B4). Простой
Spring Data репозиторий `NominalLemmaRepository` с методом
`findByLemmaIastIn(Collection<String> lemmaIasts): List<NominalLemma>`.

**B4. Переключить поиск в `/declension-examples` с `noun_stems`/`NounStem` на
`nominal_lemmas`/`NominalLemma`.** Место в коде — там же, где в прошлой
задаче (`task-noun-stems-table.md`, шаг B4) был подключён `NounStem`-лукап,
заменить его целиком: для набора слов-кандидатов собрать `Set<String>` их
`lemmaIast`, одним запросом `findByLemmaIastIn(...)` получить все подходящие
`NominalLemma`, сложить в `Map<String lemmaIast, NominalLemma>`; для каждого
кандидата смотреть `map.get(candidate.getLemmaIast())` — если найдена и
`stemClass != null`, это и есть `vowelType`; если не найдена (или
`stemClass == null`) — fallback на эвристику по последней букве `stem` (код
эвристики не менять, он уже есть). Логику выбора "лучшей строки по
confidence" (была нужна для `noun_stems` из-за 1:N) убрать — в
`nominal_lemmas` одна строка на лемму, выбирать не из чего.

**B5. Обновить/заменить тесты из предыдущей задачи.** Юнит-тест на выбор
"лучшей строки" (был `task-noun-stems-table.md` B5) — удалить или
переписать: логика выбора по confidence в новом дизайне не нужна (см. B4).
Интеграционный тест на fallback (был B6) — адаптировать под
`nominal_lemmas`: (a) лемма без строки в `nominal_lemmas` → fallback на
эвристику по букве; (b) лемма со строкой, где `stemClass` расходится с
эвристикой (например, эвристика по букве дала бы `A_STEM`, а
`nominal_lemmas.stem_class = U_STEM`) → побеждает `nominal_lemmas`; (c)
новый кейс, специфичный для лемма-уровня: два разных `VerseWord` с одной и
той же `lemmaIast` (например, `rāma` в двух разных стихах) — оба находят
`vowelType` через один и тот же ряд `nominal_lemmas` (без дублирования
данных, но результат поиска идентичен для обоих вхождений).

**B6. Проверить данные после миграции (ручной шаг, не автоматизировать).**
После применения B1 на копии прод-данных (или staging) сравнить
`SELECT COUNT(*) FROM sangraha.noun_stems` и
`SELECT COUNT(*) FROM sangraha.nominal_lemmas` — второе число должно быть
существенно меньше первого (дедупликация сработала), не равно и не больше.
Зафиксировать оба числа в комментарии к PR.

---

## Критерии готовности

- [ ] B1–B2: миграция `V4` применяется без ошибок, включает `ck_nominal_lemma_
      confidence` и индекс `idx_verse_words_lemma_iast`
- [ ] B3: `NominalLemma` + репозиторий с `findByLemmaIastIn`, без лишней
      JPA-связи с `VerseWord`
- [ ] B4: поиск в `/declension-examples` использует `nominal_lemmas` вместо
      `noun_stems`, старая эвристика по букве — fallback, как раньше
- [ ] B5: тесты на выбор лучшей строки убраны/не нужны, тесты на fallback и
      на дедупликацию (один лемма-ряд на несколько вхождений) — проходят
- [ ] B6: числа `noun_stems`/`nominal_lemmas` после миграции зафиксированы в PR
- [ ] `noun_stems` таблица и Java-код `NounStem` не удалены (оставлены как есть)

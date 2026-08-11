# Вкладка «Примеры» на странице шага склонений

На странице одного шага карусели склонений (`GET /lessons/{slug}/declension-paradigms?index=N`, §5а — одна `DeclensionParadigmDto`: `stem` + все его `DeclensionForm[]`, т.е. все ячейки `case×number`) добавляется вкладка «Примеры»: для каждой ячейки парадигмы — несколько реальных цитат из проанализированных стихов, где эта словоформа встречается. Источник цитат — sangraha-service (`sangraha-service.md` §9); curriculum-service кэширует у себя только список `verseId` по группам, сами цитаты (текст/перевод/атрибуция) не кэширует — забирает их у sangraha-service заново при каждом открытии вкладки (см. шаг 3 ниже).

Группа — не по конкретному `stem`, а по его словоизменительному классу `(vowelType, gender)` × ячейке `(caseType, numberType)`: примеры для класса `A_STEM MASCULINE` в творительном единственного числа одни и те же для любого `stem` этого класса (`rāma`, `deva`, ...), поэтому кэш и запрос к sangraha-service строятся по классу, а не по конкретной основе — переиспользуются между разными уроками/стемами одного класса.

### Таблица `declension_example_groups` (новая, схема `content`)

```
id                UUID PK
vowel_type        VARCHAR NOT NULL   -- значения = declension_stems.vowel_type (ck_vowel_type, см. V13)
gender            VARCHAR NOT NULL   -- значения = declension_stems.gender (ck_gender)
case_type         VARCHAR NOT NULL   -- content.CaseType
number_type       VARCHAR NOT NULL   -- content.NumberType
verse_ids         JSONB NOT NULL     -- [UUID, ...], может быть пустым массивом — это ЗНАЧИМЫЙ
                                     -- результат («искали, ничего не нашли»), не «ещё не искали»
created_at        TIMESTAMPTZ NOT NULL DEFAULT now()

UNIQUE (vowel_type, gender, case_type, number_type)
```

Пустой `verse_ids` — валидное закэшированное значение (защищает от повторного бесполезного похода в sangraha-service за классами/ячейками, для которых примеров в проанализированных текстах пока нет). Строки этой таблицы не инвалидируются автоматически — если в sangraha-service появятся новые проанализированные стихи с подходящими словоформами, старый (в том числе пустой) кэш не обновится сам; это принятое ограничение первой итерации (ручная инвалидация — TRUNCATE или DELETE по `(vowel_type, gender)` при необходимости), а не открытый вопрос требующий решения сейчас.

### `GET /content/public/lessons/{slug}/examples`

Публичный, STUDENT — та же авторизация, что у `GET /content/public/lessons/{slug}/declension-paradigms` (`curriculum-service.md` §5а), но без query-параметра `index`: адресация только по `slug` урока. Ленивая загрузка по клику на вкладку «Примеры», не часть ответа основного эндпоинта парадигмы.

**Обработка:**
1. Резолвить `(vowelType, gender)` для `slug` — загрузить стемы урока (тот же `DeclensionStemRepository`, что в `getDeclensionParadigmForLesson`, §5а), включая ошибку `LESSON_NOT_FOUND`; все стемы одного урока склонений принадлежат одному словоизменительному классу `(vowelType, gender)` (см. выше — группировка идёт по классу, а не по конкретному стему), поэтому достаточно взять класс любого стема урока (например, первого по той же стабильной сортировке, что в §5а).
2. Для полного набора ячеек `(caseType, numberType)` (все падежи × оба числа, единый для всех уроков склонений) найти в `declension_example_groups` строки по `(vowelType, gender, caseType, numberType)`. Ячейки, для которых строка уже есть (в том числе с пустым `verse_ids`) — взять `verse_ids` из кэша, дальше не переспрашивать.
3. Для ячеек без строки в кэше — один батч-вызов `POST {SANGRAHA_SERVICE_URL}/sangraha/internal/content/declension-examples` (`sangraha-service.md` §9) сразу на все недостающие ячейки этой парадигмы (`vowelType`/`gender` фиксированы на весь запрос, `cells[]` — только недостающие `(caseType, numberType)`, `limitPerGroup` — константа, число примеров на ячейку, значение определяет Агент 2 при реализации). Ответ `groups[]` (`caseType`, `numberType`, `verseIds[]`, `verseIds` может быть пустым) — upsert каждой группы в `declension_example_groups` (в том числе пустых — это тоже кэшируемый результат, см. описание таблицы выше).
4. Собрать множество всех `verseId` по всем ячейкам (кэш + только что дозаполненные), убрать дубли (одна и та же цитата может иллюстрировать одновременно, например, и nominative singular, и accusative singular, если формы совпадают). Один батч-вызов `POST {SANGRAHA_SERVICE_URL}/sangraha/internal/content/verses/batch` (`sangraha-service.md` §9) с этим списком `verseId` → получить `workSlug`/`textIast`/`textDevanagari`/`translationRu/En`/`workTitleRu/En`/`chapterTitleRu/En`/`verseOrderIndex` для каждого найденного `verseId`. `verseId`, не найденные в sangraha-service (например, стих был удалён/деанализирован после того, как попал в кэш, или ещё не `ANALYZED`, см. `sangraha-service.md` §9) — просто отсутствуют в ответе, без ошибки; соответствующая цитата тихо пропускается при сборке ответа фронтенду, `declension_example_groups` при этом не чистится (см. ограничение кэша выше).

4а. **`missingVerseIds` (только для роли `ADMIN`).** Разница множеств: все `verseId` из шага 4 минус те, что реально вернулись из `/verses/batch` — это стихи-кандидаты, которые есть в sangraha-service и грамматически подходят, но ещё не `ANALYZED` (поэтому не попали в `examples[]`). Для `STUDENT`/анонимного запроса это поле не считается и не попадает в ответ вовсе — не только не показывается на фронте, а именно не вычисляется и не сериализуется (список внутренних `verseId` неанализированных стихов не должен светиться в публичном ответе для не-ADMIN). Используется фронтендом для кнопки «Проанализировать недостающие примеры», см. `docs/frontend/pages/grammar-lesson-page.md` §2.2а.

5. Собрать ответ фронтенду: список групп `(caseType, numberType, examples[])`, `examples[]` — уже с полным текстом/переводом/атрибуцией, в том порядке `verseId`, в котором они пришли от sangraha-service, плюс (только для `ADMIN`) `missingVerseIds[]` из шага 4а.

Поля ответа (`DeclensionExamplesResponseDto`): `groups` — массив, каждый элемент: `caseType`, `numberType`, `examples` — массив, каждый элемент: `verseId`, `workSlug`, `textIast`, `textDevanagari`, `translationRu`, `translationEn`, `workTitleRu`, `workTitleEn`, `chapterTitleRu`, `chapterTitleEn`, `verseOrderIndex` (`workSlug` нужен фронтенду для перехода на страницу стиха `/sangraha/{workSlug}/verses/{verseId}` по клику на цитату). `missingVerseIds` — массив UUID, только для `ADMIN` (для остальных ролей поле отсутствует в JSON, не `null`/пустой массив — именно отсутствует, чтобы не путать «нет недостающих» с «не считали для этой роли»).

### Открытые вопросы (для Агента 2 при реализации)

- `limitPerGroup` (число примеров на ячейку) — конкретное значение не зафиксировано, решает Агент 2 (ориентир: 2-3, чтобы не перегружать вкладку).
- Инвалидация `declension_example_groups` при появлении новых проанализированных стихов — вне рамок первой итерации (см. описание таблицы выше), не блокирует реализацию.
- `gender = null` от sangraha (для indeclinable-слов) — как мапится в `VocabularyWord.gender` (там `nullable = false`)? Вероятно `UNSPECIFIED` — подтвердить при реализации.

**Отбор и ранжирование примеров (реализуется в sangraha-service, не в curriculum-service)** — curriculum-service просто принимает уже отранжированные и ограниченные `≤ limitPerGroup` `verseIds` в ответе `/declension-examples`, никакой дополнительной пост-обработки на своей стороне не делает. Поиск не фильтрует по `Verse.status` (часть корпуса загружена внешним скриптом в обход штатного анализа, без простановки `ANALYZED`, но с уже готовой морфологией слов). Алгоритм ранжирования (по количеству слов в стихе, с мягким приоритетом ≥3 слова) — см. `sangraha-service.md` §9.
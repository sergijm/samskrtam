# Lexical Curriculum — таксономии наполнения и учебные Lexical Topics

> Связанные файлы: [lexicon.md](./lexicon.md) (домен), [lexical-quizzes.md](./lexical-quizzes.md)
> (типы квизов), [curriculum-service.md](./curriculum-service.md) (Topic/LearningLevel/ComplexQuiz —
> переиспользуются, не дублируются), [curriculum.md](./curriculum.md) (grammar-curriculum).

---

## 1. LexicalTopic = запись curriculum.topic (domain=LEXICON) + композиция в той же схеме

**Главное архитектурное решение этого документа:** Lexical Topic не заводит
собственную таблицу «тема» — он **регистрируется как обычная строка**
`curriculum.topic` (тот же сервис и та же схема, что и grammar-темы, см.
`curriculum-service.md`), с новым полем-дискриминатором:

`curriculum.topic.domain` (VARCHAR 10, NOT NULL, DEFAULT `GRAMMAR` — `GRAMMAR`|`LEXICON`; **миграция V2** к уже существующей `V1__create_curriculum_schema.sql`, добавляет колонку с DEFAULT для обратной совместимости с уже созданными grammar-темами)

Это даёт lexical-темам **бесплатно**, без дублирования кода:
- участие в общем графе `TopicPrerequisite` (например, «Vocabulary: Animals» может
  иметь prerequisite «Declension: a-основы», если это педагогически осмысленно —
  теперь это ребро в той же таблице `curriculum.topic_prerequisite`, что и связи
  между grammar-темами, без каких-либо кросс-сервисных допущений);
- `learningLevel` (`L0`…`L6`) — та же шкала, что и у grammar-тем, физически одна
  шкала на весь учебный план, не две параллельных;
- участие в `ComplexQuiz` (`curriculum-service.md` §4) — **важно:** `ComplexQuiz`
  остаётся общим механизмом «2–7 тем любого домена»; для чисто лексических
  интегрированных квизов (§13 задачи, «Mixed Vocabulary Practice») она тоже
  подходит **как структура-контейнер** (какие Topic входят), но фактические
  слова внутри резолвятся отдельным модулем lexicon (§2 `lexical-quizzes.md`),
  не общей бизнес-логикой Topic/ComplexQuiz — вот она, «интеграция без слияния
  моделей», требуемая задачей: один сервис и одна БД, но `Topic`-механика и
  `Lexeme`-механика остаются разным кодом/разными таблицами, соединёнными
  явными, узкими связями, а не одной моделью.

**Что не смешивается с механикой Topic/ComplexQuiz:** сама композиция «какие
Lexeme входят в эту Topic» — это `curriculum.lexical_topic_binding`:

lexicalTopicId (UUID, FK → curriculum.topic.id, ON DELETE CASCADE — теперь
обычный внутрибазовый FK, т.к. `Topic` и `Lexeme` живут в одной схеме),
lexemeId (UUID, FK → curriculum.lexeme.id, ON DELETE CASCADE), PRIMARY KEY (lexicalTopicId, lexemeId)

**Материализовано, не вычисляется на лету:** привязка курируется (обычно
изначально заполняется массово — «взять все Lexeme с `semanticTopicId = Animals`
и `posCode = noun`», см. `lexicon-content-pipeline.md` §2 — а затем можно вручную
скорректировать), а не пересчитывается каждый раз по фильтру. Причина: состав
учебной темы должен быть стабилен для педагогической прогрессии (нельзя, чтобы
тема «Animals» тихо потеряла слово из-за правки семантической таксономии) — в
отличие от `frequency`/`semanticTopic`/`source`-фильтров, которые остаются живыми
запросами (см. `lexical-quizzes.md` §3, `MIXED_TOPIC`/`FREQUENCY_BAND` виды
квизов).

Таким образом ответ на пример из задачи («Animals» / «Animals × Top 500» /
«Animals × Nouns») получается без копий: `Animals` — фиксированный набор через
`lexical_topic_binding`; `Animals × Top 500` и `Animals × Nouns` — тот же набор,
дополнительно отфильтрованный `curriculum.lexeme_frequency`/`curriculum.lexeme_pos`
на чтение, тем же модулем внутри curriculum-service (`lexical-quizzes.md` §3).

---

## 2. Frequency bands — проверка предложенной разбивки

Предложенная в задаче разбивка (1–100 / 101–250 / 251–500 / 501–1000 / 1001–1500 /
1501–2000) взята под сомнение намеренно — она пропорционально почти удваивает
размер полосы на первых трёх шагах (100→150→250), но дальше идёт **одинаковыми**
шагами по 500. Для языка с Zipf-распределением частотности (типично и для
санскрита) полезность каждого следующего слова убывает нелинейно: топ-100 слов
покрывает непропорционально большую долю употреблений в тексте, а разница между
«1001–1500-м» и «1501–2000-м» словом по факту исчезающе мала — учебной ценности
в разделении **двух** последних полос почти нет, только лишняя мелкая категория
(прямо то, чего просит избегать задача, п.4, применительно и к frequency).

**Рекомендация — 5 полос вместо 6**, справочник `curriculum.frequency_band`
(таблица, не enum — границы можно менять без миграции схемы, см. `lexicon.md` §3.1):

| code | Диапазон | Label RU | Обоснование |
|---|---|---|---|
| `CORE` | 1–100 | Ядро | Служебные слова, базовые глаголы/местоимения — необходимый минимум для любого текста |
| `ESSENTIAL` | 101–250 | Существенный минимум | Всё ещё очень высокая частота, отдельная полоса оправдана — сюда попадает основная бытовая/природная лексика |
| `FOUNDATIONAL` | 251–500 | Базовый словарь | Расширение до уровня, достаточного для упрощённых текстов |
| `INTERMEDIATE` | 501–1000 | Средний уровень | Тематическая лексика (§3), необходимая для оригинальных, но не сложных текстов |
| `EXTENDED` | 1001–2000 | Расширенный словарь | Слияние двух исходных «хвостовых» полос — педагогически это один уровень «читаю оригинал со словарём», внутреннее разделение не даёт различимой пользы |

Frequency quiz'ы (`Core Vocabulary 1/2/…`, задача §14) строятся кумулятивно поверх
этих полос (`rank ≤ 100`, `rank ≤ 250`, …), а не изолированно по одной полосе —
кумулятивный тест лучше проверяет «реальное узнавание наиболее частотной
лексики», как и требует задача, потому что не даёт забыть более частотный слой
при проверке следующего.

**Перемешивание по POS/теме внутри frequency quiz** — обязательное требование
задачи §3 реализуется не на уровне модели (frequency остаётся чистым измерением),
а на уровне алгоритма отбора сессии в `lexical-quizzes.md` §4 (сортировка pool'а
случайно с последующим contiguous-запретом на 2 подряд одного `posCode`).

---

## 3. Semantic taxonomy — 9 корневых категорий, ~42 листовых узла

Итоговая таксономия (заменяет черновой пример из задачи, с поправками на
частотность и педагогическую пользу санскритской лексики: добавлены «Society &
Ritual» и «Speech & Communication» как отдельные корни — эпос/дхарма-тексты
делают эту лексику высокочастотной, недостаточно раскрытой в исходном примере;
«Movement/Action» выделен в отдельный корень, а не подкатегория, т.к. глаголы
движения — одна из плотнейших зон санскритского словаря):

**Nature** — Animals · Plants · Landscape · Water · Weather & Sky · Agriculture
**People & Body** — Family · Body parts · Occupations · Social relations · Character & Personality · Royalty & Social hierarchy
**Everyday life** — Food & Drink · House & Dwelling · Clothing & Ornament · Travel & Vehicles · Objects & Tools · Materials
**Movement & Action** — Motion verbs · Physical action · Rest & Stillness
**Speech & Communication** — Speech acts · Naming & Address · Question & Answer
**Perception & Cognition** — Senses · Thought & Memory · Knowledge & Learning
**Emotion & Character** — Positive emotions · Negative emotions · Desire & Will
**Abstract** — Time · Quantity & Number · Space & Direction · Cause & Purpose · Comparison
**Society & Ritual** — Ritual & Worship · Ethics & Duty (dharma) · Governance & Law · War & Conflict · Philosophy & Liberation

Итого: 9 корней + 33 листа = 42 узла — в целевом диапазоне 30–50 (задача §4).
Каждый лист рассчитан минимум на ~15–20 лемм из базовых 2000 (проверяется на
этапе наполнения, `lexicon-content-pipeline.md` §4 — категория с итоговым
наполнением < 10 слов подлежит объединению с соседней, чтобы не плодить мелкие
категории).

**M:N подтверждается на уровне схемы** (`curriculum.lexeme_semantic_topic`, §3.2
`lexicon.md`) — `गजः` может быть одновременно в `Animals`, `Nature` (родитель,
если решено размечать и на уровне родителя — решение принимается при наполнении,
не запрещено схемой) и `Agriculture` (рабочее животное).

---

## 4. Part-of-speech taxonomy

Плоский справочник (не дерево, но с группой-меткой для UI-фильтров):

**Nominal** — noun · adjective · pronoun · numeral
**Verbal** — finite-verb · participle · infinitive · absolutive · gerund
**Indeclinable** — adverb · particle · conjunction · preverb · interjection · preposition

14 кодов. Хранится как справочник `curriculum.part_of_speech`, не enum в коде
приложения — то же соображение расширяемости, что и `frequency_band`.

---

## 5. Morphology taxonomy

**NOUN:**
a-stem-masc · a-stem-neut · ā-stem-fem · i-stem · u-stem · ṛ-stem · consonant-stem · irregular

**VERB:**
class-1 (bhū) · class-2 (ad) · class-3 (hu) · class-4 (div) · class-5 (su) ·
class-6 (tud) · class-7 (rudh) · class-8 (tan) · class-9 (krī) · class-10 (cur) ·
irregular-verb

18 кодов. Прямое пересечение словаря с grammar-curriculum по `code`
(`curriculum-service` Topic `a-stem-masculine` ↔ `morphology_class.code = a-stem-masc`)
описано в `lexicon.md` §3.4 — совпадение имён кодов даёт агрегации фронтенда
показать «вот лексика, иллюстрирующая эту грамматическую тему» без FK.

---

## 6. Начальный список Lexical Topics (68)

Формируется как: (а) один Topic на «крупный» лист таксономии §3 (там, где
ожидаемое наполнение позволяет самостоятельный quiz, обычно 25–80 слов), (б) один
Topic на несколько мелких смежных листьев, объединённых, если по отдельности они
были бы слишком малы, (в) несколько чисто POS-ориентированных Topic
(«Common verbs of motion», «Basic adjectives») — задача прямо разрешает и просит
не ограничиваться semantic-делением. `learningLevel` расставлен по той же шкале
`L0`–`L6`, что и grammar-curriculum (`curriculum-service.md`), независимо друг от
друга — переиспользуется шкала, не переиспользуется конкретное распределение.

**L0 — фундамент (9 тем):** Basic function words · Numbers 1–10 · Personal pronoun
vocabulary · Family core · Body core · Common nouns (objects) · Common verbs
(basic actions) · Greetings & address · Yes/no & basic questions

**L1 — база (10 тем):** Animals · Plants & trees · Food & drink · House & dwelling
· Clothing & ornament · Colors · Common adjectives (quality) · Common adjectives
(size/quantity) · Landscape & terrain · Water & rivers

**L2 — расширение бытовой лексики (10 тем):** Weather & sky · Time — parts of day
& seasons · Occupations & professions · Travel & vehicles · Tools & objects ·
Motion verbs (basic) · Speech verbs · Perception verbs (see/hear/know) · Materials
& substances · Agriculture & village life

**L3 — абстракция и социум (10 тем):** Emotions — positive · Emotions — negative
· Desire & will · Character & personality traits · Social relations & kinship
terms (extended) · Royalty & social hierarchy · Quantity & measurement · Space &
direction · Comparison & degree · Cause & purpose vocabulary

**L4 — религия, этика, познание (9 тем):** Ritual & worship · Ethics & duty
(dharma vocabulary) · Knowledge & learning · Thought & memory · Philosophy &
liberation (mokṣa vocabulary) · Governance & law · Naming & address forms ·
Question & answer vocabulary · Rest & stillness verbs

**L5 — расширенный словарь (12 тем):** War & conflict · Music & arts · Advanced
motion verbs (compound-prone) · Advanced adjectives (abstract quality) · Nature —
extended (celestial bodies, natural phenomena) · Mythology & epithets (common
epic vocabulary) · Advanced particles & conjunctions · Verbs of giving/taking/
exchange · Verbs of creation & destruction · Body — extended (internal organs,
actions) · Comparative & superlative expressions · Idiomatic verb-noun collocations

**L6 — по частям речи / морфологии, интеграционные (8 тем):** a-stem nouns
(vocabulary cross-section) · ā-stem nouns (vocabulary cross-section) · Verb class
1 vocabulary · Verb class 6 vocabulary · Participle vocabulary (common
participles as lexemes) · Absolutive & infinitive vocabulary · Numerals 1–100 ·
Core vocabulary review (evergreen, `isEvergreen=true`, не привязан к уровню)

Итого 68 (без evergreen) + 1 evergreen = наглядно в целевом диапазоне 50–80.
Точный целевой объём слов на тему — 15–80, наибольшие («Common nouns», «Common
verbs») ожидаемо на верхней границе, узкоспециализированные («Idiomatic
verb-noun collocations») — на нижней; конкретные числа фиксируются на этапе
наполнения (`lexicon-content-pipeline.md` §4), не заранее в этом документе.

---

## 7. LearningMaterial для Lexical Topic

Переиспользуется существующая модель `LearningMaterial` (`learning-materials.md`,
`topicId` → `curriculum.topic.id`, физически хранится в content-service, см.
`curriculum-service.md` §5) без изменений схемы. Для lexical Topic обязательный
минимальный набор материалов (не «энциклопедия», задача §11 явно просит
краткость):

1. **TEXT** — краткое введение (2–4 предложения: зачем эта группа слов, что
   объединяет);
2. **TEXT** — сам vocabulary list темы: лемма (IAST + Devanāgarī) + перевод +
   POS + (если применимо) морфологический класс — рендерится из
   `lexical_topic_binding` join `curriculum.lexeme`, не хранится отдельно как
   застывший текст (иначе дублирование данных, живущих в таблицах §1–§5 `lexicon.md`);
3. **TEXT** — 3–5 примеров употребления в контексте (по возможности —
   реальные атрестованные `WordForm`/`SourceOccurrence` из §4 `lexicon.md`,
   не выдуманные предложения).

Грамматические сведения (п.11 задачи, «там где полезны») добавляются только для
тем §6 уровня L6 (морфологически ориентированных) — для остальных не нужны,
чтобы не раздувать материал.

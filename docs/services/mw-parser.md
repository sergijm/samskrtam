# MWParser — Спецификация парсера Monier-Williams

> Компонент: `sm.selflearn.samskrtam.dictionary.parser`
> Связанные файлы: [dictionary-service.md](dictionary-service.md)
> Status: **DRAFT**

---

## 1. Назначение

Парсер извлекает структурированные лингвистические данные из HTML словарных статей
Monier-Williams (CSL API). Является отдельным компонентом внутри dictionary-service.

---

## 2. Пример словарной статьи

```
(H1) [Printed book page 492,2]
देव mf(ई)n. (fr. 3. दिव्) heavenly, divine, RV.; AV.; VS. [ID=95518]
देव m. (according to Pāṇ. iii, 3, 120) a deity, god, RV. &c. [ID=95519]
(rarely applied to) evil demons, AV. iii, 15, 5 [ID=95520]
(pl. the gods as the heavenly or shining ones...) [ID=95521]

(H1B) [Printed book page 492,3]
देवा (आ), f. Hibiscus Mutabilis [ID=95539.01]
देवी (ई), f. See s.v. [ID=95539.02]

(H1E) [Printed book page 492,3]
देव [cf. Lat. dīvus, deus; Lit. dë́vas] [ID=95540.55]
```

---

## 3. Структура статьи

| Маркер | Назначение |
|---|---|
| `(H1)` | Главная статья |
| `(H1B)` | Производные формы (देवा f., देवी f.) |
| `(H1E)` | Этимология и сравнительная лингвистика |
| `[ID=NNNN]` | Уникальный ID каждой записи в CSL |
| `[Printed book page X,Y]` | Ссылка на страницу печатного словаря |

---

## 4. Грамматические пометы

### Род и часть речи

| Помета | WordClass | Gender |
|---|---|---|
| `m.` | NOUN | MASCULINE |
| `f.` | NOUN | FEMININE |
| `n.` | NOUN | NEUTER |
| `mfn.` | ADJECTIVE | MASCULINE + FEMININE + NEUTER |
| `mf(ई)n.` | ADJECTIVE | все три, ж.р. → ई |
| `mf(आ)n.` | ADJECTIVE | все три, ж.р. → आ |
| `ind.` | PARTICLE | — |

### Глагольные пометы

| Помета | Значение |
|---|---|
| `cl.1 P.` | 1-й класс, Parasmaipada |
| `cl.4 Ā.` | 4-й класс, Ātmanepada |
| `cl.10 U.` | 10-й класс, Ubhayapada |
| `(Dhātup.)` | ссылка на Dhātupāṭha |

### Сокращения источников

| Сокращение | Источник |
|---|---|
| `RV.` | Rigveda |
| `AV.` | Atharvaveda |
| `MBh.` | Mahābhārata |
| `R.` | Rāmāyaṇa |
| `Pāṇ.` | Pāṇini (Ashtadhyayi) |
| `L.` | Lexicographers only |
| `W.` | Wilson's dictionary |

---

## 5. Модели данных парсера

```kotlin
// sm/selflearn/samskrtam/dictionary/model/Gender.kt
enum class Gender { MASCULINE, FEMININE, NEUTER }

// sm/selflearn/samskrtam/dictionary/model/WordClass.kt
enum class WordClass { NOUN, ADJECTIVE, VERB, PARTICLE, UNKNOWN }

// sm/selflearn/samskrtam/dictionary/model/VerbPada.kt
enum class VerbPada { PARASMAIPADA, ATMANEPADA, UBHAYAPADA }

// sm/selflearn/samskrtam/dictionary/parser/ParsedEntry.kt
data class ParsedEntry(

    // v1 — обязательные
    val wordDevanagari:    String,
    val gender:            Set<Gender>  = emptySet(),
    val feminineEnding:    String?      = null,    // "ई" из mf(ई)n.
    val wordClass:         WordClass    = WordClass.UNKNOWN,
    val senses:            List<Sense>  = emptyList(),
    val cslId:             String?      = null,    // первый [ID=...] в статье

    // v2 — расширенные
    val etymology:         String?      = null,    // "(fr. 3. दिव्)"
    val derivedForms:      List<DerivedForm> = emptyList(),
    val paniniRef:         String?      = null,    // "Pāṇ. iii, 3, 120"

    // v3 — глагольные
    val verbClass:         Int?         = null,    // 1, 4, 10
    val verbPada:          VerbPada?    = null,
    val verbRoot:          String?      = null,    // dhātu в IAST
)

data class Sense(
    val id:       String,           // "95519"
    val text:     String,           // очищенный текст значения
    val sources:  List<String>,     // ["RV", "AV"]
    val isRare:   Boolean = false,  // помечено "(rarely)"
    val note:     String? = null,   // дополнительные пометы в скобках
)

data class DerivedForm(
    val wordDevanagari: String,     // "देवा"
    val ending:         String,     // "(आ)"
    val gender:         Gender,     // FEMININE
    val meaning:        String?,    // "Hibiscus Mutabilis"
    val cslId:          String,     // "95539.01"
)
```

---

## 6. Регексы (MWPatterns.kt)

```kotlin
object MWPatterns {

    // Грамматические пометы
    val GENDER_FULL  = Regex("""^(mf\([^)]+\)n|mfn|m|f|n)\.""")
    val FEM_ENDING   = Regex("""mf\(([^)]+)\)n""")

    // Структура статьи
    val ENTRY_ID     = Regex("""\[ID=(\d+(?:\.\d+)*)\s*\]""")
    val HEADER_LEVEL = Regex("""^\(H(\d+[A-Z]?)\)""")
    val PAGE_REF     = Regex("""\[Printed book page \d+,\d+\]""")

    // Источники (RV., AV., MBh. и т.д.)
    // Исключаем одиночные заглавные буквы чтобы не ловить лишнее
    val SOURCES      = Regex("""\b([A-ZŚĀĪŪ][a-zāīūśṣṭḍṇñṃḥ]{1,8}\.)+""")

    // Этимология
    val ETYMOLOGY    = Regex("""\(fr\.\s*\d*\.?\s*[^\)]+\)""")

    // Panini
    val PANINI_REF   = Regex("""Pāṇ\.\s*[ivx\d,\s\.]+""")

    // Глагольный класс: "cl.1 P." или "cl.10 U."
    val VERB_CLASS   = Regex("""cl\.(\d+)\s*([PĀU])\.""")

    // Производные формы в (H1B): "देवा (आ), f."
    val DERIVED_FORM = Regex("""^(\S+)\s+\(([^)]+)\),\s*(m|f|n)\.""")

    // "(rarely ...)" пометы
    val RARELY       = Regex("""\(rarely\b""")
}
```

---

## 7. Алгоритм парсинга (MWParser.kt)

```kotlin
@Component
class MWParser {

    fun parse(key: String, html: String): DictionaryEntry {
        val doc    = Jsoup.parse(html)
        val text   = doc.text()               // plain text для регексов
        val parsed = parseStructure(text)

        return DictionaryEntry(
            key              = key,
            word             = key,           // SLP1 → IAST конвертация в v2
            wordDevanagari   = parsed.wordDevanagari,
            meanings         = Json.encodeToString(
                                 parsed.senses.map { it.text }.take(5)
                               ),
            partOfSpeech     = parsed.wordClass.name.lowercase(),
            grammaticalGender = parsed.gender.firstOrNull()?.name?.lowercase(),
            feminineEnding   = parsed.feminineEnding,
            verbRoot         = parsed.verbRoot,
            verbClass        = parsed.verbClass,
            cslId            = parsed.cslId,
            rawHtml          = html,
        )
    }

    private fun parseStructure(text: String): ParsedEntry {
        val lines = text.lines()

        return ParsedEntry(
            wordDevanagari = extractDevanagari(lines),
            gender         = extractGender(lines),
            feminineEnding = extractFeminineEnding(lines),
            wordClass      = extractWordClass(lines),
            senses         = extractSenses(lines),
            cslId          = extractFirstId(lines),
        )
    }
}
```

---

## 8. Стратегия итеративного парсинга

```
v1 (текущий):
  ✓ wordDevanagari  — первое слово в деванагари
  ✓ gender          — из первой пометы (m/f/n/mfn/mf(X)n)
  ✓ wordClass       — NOUN / ADJECTIVE / VERB / PARTICLE
  ✓ senses          — первые 5 значений (текст до источников)
  ✓ cslId           — первый [ID=...] в статье
  ✓ rawHtml         — всегда сохраняется

v2:
  + все значения с ID, sources, isRare
  + feminineEnding из mf(X)n.
  + derivedForms из блока (H1B)
  + etymology
  + paniniRef

v3:
  + verbClass, verbPada, verbRoot для глаголов
  + SLP1 → IAST конвертация для поля word
  + перекрёстные ссылки между статьями
```

---

## 9. Тестирование

Парсер покрывается тестами на реальных статьях — по одной на каждый тип слова:

| Тест | Слово | Тип |
|---|---|---|
| `parseNounMasculine` | deva | существительное м.р. |
| `parseNounFeminine` | devī | существительное ж.р. |
| `parseAdjective` | divya | прилагательное mfn. |
| `parseAdjectiveFemEnding` | sundara | прилагательное mf(ā)n. |
| `parseVerb` | bhū | глагол cl.1 P. |
| `parseParticle` | ca | частица ind. |
| `parseMultipleSenses` | deva | статья с 10+ значениями |

```kotlin
// Пример теста (Kotest)
class MWParserTest : StringSpec({

    val parser = MWParser()

    "parse deva - masculine noun" {
        val html = loadTestResource("deva.html")
        val entry = parser.parse("deva", html)

        entry.wordDevanagari    shouldBe "देव"
        entry.wordClass         shouldBe "noun"
        entry.grammaticalGender shouldBe "masculine"
        entry.meanings.isNotEmpty() shouldBe true
        entry.rawHtml           shouldNotBe null
    }
})
```

> **Тестовые ресурсы:** сохранить реальные HTML ответы от CSL API в
> `src/test/resources/parser/` — это защитит от изменений структуры API.

---

## 10. Endpoint перепарсинга

При улучшении парсера — перепарсинг всех сохранённых статей из `rawHtml` без
повторных запросов к CSL:

```kotlin
// AdminDictionaryService.kt
suspend fun reparseAll(): ReparseResult {
    var success = 0; var failed = 0

    repository.findAll()
        .filter { it.rawHtml != null }
        .collect { entry ->
            runCatching { parser.parse(entry.key, entry.rawHtml!!) }
                .onSuccess { repository.save(it); success++ }
                .onFailure { failed++ }
        }

    return ReparseResult(success, failed)
}
```

---

## 11. Открытые вопросы

- [ ] HTML структура CSL — проверить реальные ответы API перед написанием парсера
- [ ] Как извлекать деванагари — из HTML тегов или plain text?
- [ ] SLP1 → IAST конвертация для поля `word` (v2)
- [ ] Обработка статей с множественными значениями (50+ senses)
- [ ] Парсинг ведических акцентов (`accent=yes`) — нужно ли?

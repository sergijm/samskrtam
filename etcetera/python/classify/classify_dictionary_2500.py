#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
classify_dictionary_2500.py
=============================
Максимально подробная классификация словаря (raw_data.dictionary_2500)
по колонке translation_en.

Источники категорий:
  1. Расширенная санскритская таксономия (sanskrit_taxonomy.py, 200+ категорий,
     организована в иерархию доменов GRAM/MYTH/NAT/BODY/SOC/ACT/ABSTR/MAT/QUAL/SCI)
  2. WordNet:
       - lexname верхнего уровня (noun.act, verb.motion, adj.all, ...)
       - цепочка гиперонимов (до 2 уровней вверх от прямого синсета),
         что даёт более глубокую и специфичную категоризацию,
         чем плоский lexname.

Результат записывается:
  - raw_data.categories_claude            — справочник категорий (upsert)
  - raw_data.dictionary_2500_categories    — связи слово <-> категория (many-to-many),
                                              classification_method = CLASSIFICATOR_METHOD

Запуск:
    pip install psycopg2-binary nltk
    python classify_dictionary_2500.py
"""

import csv
import re
import sys
import time
import logging
from collections import Counter
from typing import Optional

import psycopg2
import psycopg2.extras
import nltk
from nltk.corpus import wordnet as wn
from nltk.tokenize import word_tokenize
from nltk.tag import pos_tag

from sanskrit_taxonomy import TAXONOMY

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger(__name__)

# ─── DB config ────────────────────────────────────────────────────────────
DB_HOST     = "mdm-dev"
DB_PORT     = "5432"
DB_NAME     = "samskrtam"
DB_USER     = "postgres"
DB_PASSWORD = "postgres"
DB_SCHEMA   = "raw_data"

DB_SRC_TABLE            = "dictionary_2500"
DB_SRC_TABLE_KEY_COLUMN = "id"
DB_SRC_TABLE_TR_COLUMN  = "fri_en"   # как в задании (опечатка в исходном имени колонки)

CLASSIFICATOR_METHOD = "claude1"

BATCH_SIZE = 200

# ─── NLTK setup ───────────────────────────────────────────────────────────
for pkg in ("wordnet", "omw-1.4", "punkt", "punkt_tab",
            "averaged_perceptron_tagger", "averaged_perceptron_tagger_eng"):
    try:
        nltk.download(pkg, quiet=True)
    except Exception:
        pass

# ─── WordNet: lexname -> (code, name_en, name_ru) ──────────────────────────
# lexname коды НЕ зависят от санскритской таксономии -> отдельное пространство WN.*
LEXNAME_INFO = {
    "noun.act":           ("Action/Event (n)",        "Действие/событие (сущ.)"),
    "noun.animal":        ("Animal (WN)",              "Животное (WN)"),
    "noun.artifact":      ("Artifact/Object (WN)",     "Артефакт/объект (WN)"),
    "noun.attribute":     ("Attribute/Quality (WN)",   "Атрибут/качество (WN)"),
    "noun.body":          ("Body part (WN)",           "Часть тела (WN)"),
    "noun.cognition":     ("Cognition/Thought (WN)",   "Познание/мысль (WN)"),
    "noun.communication": ("Communication (WN)",       "Коммуникация (WN)"),
    "noun.event":         ("Event/Occurrence (WN)",    "Событие (WN)"),
    "noun.feeling":       ("Feeling/Emotion (WN)",     "Чувство/эмоция (WN)"),
    "noun.food":          ("Food (WN)",                "Еда (WN)"),
    "noun.group":         ("Group/Collection (WN)",    "Группа/коллекция (WN)"),
    "noun.location":      ("Location/Place (WN)",      "Место/локация (WN)"),
    "noun.motive":        ("Motive/Goal (WN)",         "Мотив/цель (WN)"),
    "noun.natural_object":("Natural Object (WN)",      "Природный объект (WN)"),
    "noun.person":        ("Person (WN)",              "Человек/персона (WN)"),
    "noun.phenomenon":    ("Phenomenon (WN)",          "Явление (WN)"),
    "noun.plant":         ("Plant (WN)",               "Растение (WN)"),
    "noun.possession":    ("Possession (WN)",          "Обладание/имущество (WN)"),
    "noun.process":       ("Process (WN)",             "Процесс (WN)"),
    "noun.quantity":      ("Quantity/Measure (WN)",    "Количество/мера (WN)"),
    "noun.relation":      ("Relation (WN)",            "Отношение (WN)"),
    "noun.shape":         ("Shape (WN)",                "Форма (WN)"),
    "noun.state":         ("State/Condition (WN)",     "Состояние/условие (WN)"),
    "noun.substance":     ("Substance/Matter (WN)",    "Вещество (WN)"),
    "noun.time":          ("Time (WN)",                "Время (WN)"),
    "noun.tops":          ("Abstract Entity (WN)",     "Абстрактная сущность (WN)"),
    "verb.body":          ("Verb: Body action (WN)",   "Глагол: действие тела (WN)"),
    "verb.change":        ("Verb: Change (WN)",        "Глагол: изменение (WN)"),
    "verb.cognition":     ("Verb: Cognition (WN)",     "Глагол: познание (WN)"),
    "verb.communication": ("Verb: Communication (WN)", "Глагол: коммуникация (WN)"),
    "verb.competition":   ("Verb: Competition (WN)",   "Глагол: соревнование (WN)"),
    "verb.consumption":   ("Verb: Consumption (WN)",   "Глагол: потребление (WN)"),
    "verb.contact":       ("Verb: Contact (WN)",       "Глагол: контакт (WN)"),
    "verb.creation":      ("Verb: Creation (WN)",      "Глагол: создание (WN)"),
    "verb.emotion":       ("Verb: Emotion (WN)",       "Глагол: эмоция (WN)"),
    "verb.motion":        ("Verb: Motion (WN)",        "Глагол: движение (WN)"),
    "verb.perception":    ("Verb: Perception (WN)",    "Глагол: восприятие (WN)"),
    "verb.possession":    ("Verb: Possession (WN)",    "Глагол: обладание (WN)"),
    "verb.social":        ("Verb: Social (WN)",        "Глагол: социальное (WN)"),
    "verb.stative":       ("Verb: State (WN)",         "Глагол: состояние (WN)"),
    "verb.weather":       ("Verb: Weather (WN)",       "Глагол: погода (WN)"),
    "adj.all":            ("Adjective (WN)",           "Прилагательное (WN)"),
    "adj.pert":           ("Relational adjective (WN)","Относительное прилагательное (WN)"),
    "adv.all":            ("Adverb (WN)",              "Наречие (WN)"),
}

WN_ROOT_CODE = "WN"


def slugify_synset_name(name: str) -> str:
    """'overpower.v.01' -> 'OVERPOWER_V01' (для построения кода гиперонима)."""
    base = name.split(".")[0]
    pos  = name.split(".")[1] if "." in name else ""
    base = re.sub(r"[^a-zA-Z0-9]+", "_", base).strip("_").upper()
    return f"{base}_{pos.upper()}" if pos else base


def clean_translation(text: str) -> str:
    """Удаляет грамматические пометы вида (acc.), (p.pf. act. ≺ bhī) и т.п."""
    if not text:
        return ""
    text = re.sub(r"\(.*?\)", " ", text)
    text = re.sub(r"≺\s*\S+", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text.lower()


def extract_content_words(text: str) -> list:
    """Токенизация + оставляем только значимые части речи."""
    try:
        tokens = word_tokenize(text)
        tagged = pos_tag(tokens)
    except Exception:
        return [w for w in text.split() if len(w) > 2]
    keep_tags = {"NN", "NNS", "NNP", "NNPS", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ",
                 "JJ", "JJR", "JJS", "RB", "RBR", "RBS"}
    return [w.lower() for w, t in tagged if t in keep_tags and len(w) > 2]


# ─── динамически собираемые WordNet-категории (заполняются по ходу классификации) ──
# code -> {"name_en", "name_ru", "parent", "level", "domain"}
DYNAMIC_WN_CATEGORIES = {
    WN_ROOT_CODE: {
        "name_en": "WordNet Semantic Category",
        "name_ru": "Семантическая категория WordNet",
        "parent": None,
        "level": 0,
        "domain": "WN",
    }
}


def get_wordnet_categories(words: list) -> set:
    """
    Для каждого значимого слова берём топ-2 синсета.
    Для каждого синсета:
      - код уровня lexname (WN.<LEXNAME>)             — широкая категория
      - код уровня прямого гиперонима (WN.<LEXNAME>.<HYPERNYM>) — более узкая
    Динамически регистрируем найденные коды в DYNAMIC_WN_CATEGORIES.
    """
    codes = set()
    for word in words:
        synsets = wn.synsets(word)
        if not synsets:
            continue
        for syn in synsets[:2]:
            lexname = syn.lexname()
            if lexname not in LEXNAME_INFO:
                continue
            name_en, name_ru = LEXNAME_INFO[lexname]
            lex_code = f"WN.{lexname.upper().replace('.', '_')}"

            if lex_code not in DYNAMIC_WN_CATEGORIES:
                DYNAMIC_WN_CATEGORIES[lex_code] = {
                    "name_en": name_en,
                    "name_ru": name_ru,
                    "parent": WN_ROOT_CODE,
                    "level": 1,
                    "domain": "WN",
                }
            codes.add(lex_code)

            # более глубокий уровень: прямой гипероним (если есть)
            hypernyms = syn.hypernyms()
            if hypernyms:
                hyp = hypernyms[0]
                hyp_slug = slugify_synset_name(hyp.name())
                hyp_code = f"{lex_code}.{hyp_slug}"
                if hyp_code not in DYNAMIC_WN_CATEGORIES and len(hyp_code) <= 250:
                    hyp_gloss = hyp.lemmas()[0].name().replace("_", " ")
                    DYNAMIC_WN_CATEGORIES[hyp_code] = {
                        "name_en": f"{hyp_gloss} (WN hypernym)",
                        "name_ru": f"{hyp_gloss} (гипероним WN)",
                        "parent": lex_code,
                        "level": 2,
                        "domain": "WN",
                    }
                if len(hyp_code) <= 250:
                    codes.add(hyp_code)
    return codes


def _compile_keyword_patterns():
    """
    Предкомпилирует regex для каждого ключевого слова с учётом границ слова.
    Для keyword'ов, содержащих небуквенные символы (точки в "adj.", "p.p." и т.п.)
    или пробелы (составные фразы "to speak"), используется частичная граница:
    \\b слева, где применимо, и не-буквенная граница справа (или конец строки).
    Это предотвращает 'ear' срабатывая внутри 'year', 'loud' внутри 'cloud' и т.п.,
    но допускает 'adj.' как отдельный токен и многословные фразы целиком.

    Дополнительно: для keyword вида "to <verb>" генерируется альтернативный
    паттерн без "to " — словарные статьи часто перечисляют инфинитивы через
    запятую без повторения "to" перед каждым (например "surpass, overpower,
    humiliate"), так что одно "to be against, surpass. overpower, humiliate"
    должно матчить категорию ACT.CONFLICT по "overpower" тоже.
    """
    compiled = {}
    for code, data in TAXONOMY.items():
        if not data["is_leaf"]:
            continue
        patterns = []
        seen_raw = set()
        for kw in data["keywords_en"]:
            kw_l = kw.lower().strip()
            if not kw_l:
                continue
            variants = {kw_l}
            if kw_l.startswith("to "):
                variants.add(kw_l[3:])
            for variant in variants:
                if variant in seen_raw:
                    continue
                seen_raw.add(variant)
                escaped = re.escape(variant)
                left = r"\b" if re.match(r"^[a-zA-Z0-9]", variant) else ""
                right = r"\b" if re.search(r"[a-zA-Z0-9]$", variant) else ""
                patterns.append(re.compile(left + escaped + right))
        if patterns:
            compiled[code] = patterns
    return compiled


_KEYWORD_PATTERNS = _compile_keyword_patterns()


def get_sanskrit_categories(translation_lower: str) -> set:
    """Сопоставление с расширенной санскритской таксономией по ключевым словам,
    с учётом границ слова (regex \\b), чтобы избежать ложных подстроковых
    совпадений вида 'ear' в 'year' или 'loud' в 'cloud'.
    Берём только листовые (is_leaf=True) категории — промежуточные узлы
    используются исключительно для построения иерархии в categories_claude."""
    codes = set()
    for code, patterns in _KEYWORD_PATTERNS.items():
        for pattern in patterns:
            if pattern.search(translation_lower):
                codes.add(code)
                break
    return codes


def categorize(translation_en: Optional[str]) -> tuple:
    """
    Возвращает (set кодов категорий, matched_text_summary).
    """
    if not translation_en or not translation_en.strip():
        return ({"UNCATEGORIZED"}, "")

    cleaned = clean_translation(translation_en)
    words   = extract_content_words(cleaned)

    skt_cats = get_sanskrit_categories(cleaned)
    wn_cats  = get_wordnet_categories(words)

    all_cats = skt_cats | wn_cats
    if not all_cats:
        all_cats = {"UNCATEGORIZED"}

    return (all_cats, cleaned[:200])


# ════════════════════════════════════════════════════════════════════════
#  DB helpers
# ════════════════════════════════════════════════════════════════════════

def get_connection():
    return psycopg2.connect(
        host=DB_HOST, port=DB_PORT, dbname=DB_NAME,
        user=DB_USER, password=DB_PASSWORD
    )


def ensure_schema_objects(conn):
    """Подстраховка: убедиться, что нужные таблицы/констрейнты существуют.
    Не пересоздаёт таблицы — предполагается, что DDL уже выполнен
    (create_categories_claude_table.sql, create_dictionary_2500_categories_table.sql)."""
    with conn.cursor() as cur:
        cur.execute(f"""
            SELECT to_regclass('{DB_SCHEMA}.categories_claude') IS NOT NULL,
                   to_regclass('{DB_SCHEMA}.dictionary_2500_categories') IS NOT NULL,
                   to_regclass('{DB_SCHEMA}.{DB_SRC_TABLE}') IS NOT NULL;
        """)
        cat_ok, link_ok, src_ok = cur.fetchone()
        if not src_ok:
            raise RuntimeError(f"Таблица {DB_SCHEMA}.{DB_SRC_TABLE} не найдена.")
        if not cat_ok:
            raise RuntimeError(
                f"Таблица {DB_SCHEMA}.categories_claude не найдена. "
                f"Сначала выполните create_categories_claude_table.sql"
            )
        if not link_ok:
            raise RuntimeError(
                f"Таблица {DB_SCHEMA}.dictionary_2500_categories не найдена. "
                f"Сначала выполните create_dictionary_2500_categories_table.sql"
            )
    log.info("Все необходимые таблицы найдены.")


def upsert_categories(conn):
    """Заливает в categories_claude:
       1. все категории из статической санскритской таксономии
       2. все динамически найденные WordNet-категории (после прогона классификации)
    """
    rows = []

    # санскритская таксономия
    for code, data in TAXONOMY.items():
        method = "sanskrit_specific" if data["parent"] is not None or code != "UNCATEGORIZED" else "manual"
        if code == "UNCATEGORIZED":
            method = "manual"
        rows.append((
            code,
            data["parent"],
            data["name_en"],
            data["name_ru"],
            data["desc_en"][:1000],
            data["desc_ru"][:1000],
            "sanskrit_specific",
            data["level"],
            data["domain"],
            0,
            data["is_leaf"],
            ", ".join(data["keywords_en"][:30]) or None,
            ", ".join(data["keywords_ru"][:30]) or None,
        ))

    # WordNet (динамические)
    for code, data in DYNAMIC_WN_CATEGORIES.items():
        is_leaf = data["level"] >= 2 or code == WN_ROOT_CODE and False
        # лист — это коды глубже корня (level>=1 с реальным гиперонимом тоже валидны как метки)
        is_leaf = True if code != WN_ROOT_CODE else False
        method = "wordnet_hypernym" if data["level"] >= 2 else "wordnet_lexname"
        rows.append((
            code,
            data["parent"],
            data["name_en"][:255],
            data["name_ru"][:255],
            f"Auto-derived WordNet category (lexname/hypernym chain)"[:1000],
            f"Автоматически выведенная категория WordNet (lexname/цепочка гиперонимов)"[:1000],
            method,
            data["level"],
            data["domain"],
            0,
            is_leaf,
            None,
            None,
        ))

    upsert_sql = f"""
        INSERT INTO {DB_SCHEMA}.categories_claude
            (category_code, parent_code, name_en, name_ru, description_en, description_ru,
             classification_method, level, domain, sort_order, is_leaf, keywords_en, keywords_ru)
        VALUES %s
        ON CONFLICT (category_code) DO UPDATE SET
            parent_code            = EXCLUDED.parent_code,
            name_en                = EXCLUDED.name_en,
            name_ru                = EXCLUDED.name_ru,
            description_en         = EXCLUDED.description_en,
            description_ru         = EXCLUDED.description_ru,
            classification_method  = EXCLUDED.classification_method,
            level                  = EXCLUDED.level,
            domain                 = EXCLUDED.domain,
            is_leaf                = EXCLUDED.is_leaf,
            keywords_en            = EXCLUDED.keywords_en,
            keywords_ru            = EXCLUDED.keywords_ru;
    """

    # сначала корни (parent IS NULL), потом остальные — чтобы FK не падал на порядке вставки.
    # Делаем 2 прохода по сортировке "глубины", т.к. в одной транзакции с deferred FK
    # порядок неважен только если констрейнт DEFERRABLE; у нас нет — поэтому сортируем сами.
    rows_by_level = sorted(rows, key=lambda r: (r[7] if r[7] is not None else 0))

    with conn.cursor() as cur:
        psycopg2.extras.execute_values(cur, upsert_sql, rows_by_level, page_size=200)
    conn.commit()
    log.info("categories_claude: upsert завершён, всего категорий записано: %d", len(rows))


def fetch_source_rows(conn):
    sql = f"""
        SELECT "{DB_SRC_TABLE_KEY_COLUMN}", "{DB_SRC_TABLE_TR_COLUMN}"
        FROM {DB_SCHEMA}.{DB_SRC_TABLE}
        ORDER BY "{DB_SRC_TABLE_KEY_COLUMN}";
    """
    with conn.cursor() as cur:
        cur.execute(sql)
        return cur.fetchall()


def clear_previous_run(conn):
    """Удаляем предыдущие результаты этого же classification_method,
    чтобы повторный запуск не плодил дубликаты/устаревшие связи."""
    sql = f"""
        DELETE FROM {DB_SCHEMA}.dictionary_2500_categories
        WHERE classification_method = %s;
    """
    with conn.cursor() as cur:
        cur.execute(sql, (CLASSIFICATOR_METHOD,))
        deleted = cur.rowcount
    conn.commit()
    log.info("Удалено старых связей метода '%s': %d", CLASSIFICATOR_METHOD, deleted)


def insert_links(conn, links: list):
    """links: список кортежей (word, category_code, classification_method, matched_text)"""
    sql = f"""
        INSERT INTO {DB_SCHEMA}.dictionary_2500_categories
            (dictionary_word_id, category_code, classification_method, matched_text)
        VALUES %s
        ON CONFLICT (dictionary_word_id, category_code, classification_method) DO UPDATE SET
            matched_text = EXCLUDED.matched_text,
            updated_at   = now();
    """
    with conn.cursor() as cur:
        psycopg2.extras.execute_values(cur, sql, links, page_size=BATCH_SIZE)
    conn.commit()


# ════════════════════════════════════════════════════════════════════════
#  MAIN
# ════════════════════════════════════════════════════════════════════════

def main():
    log.info("Подключение к БД %s@%s:%s/%s ...", DB_USER, DB_HOST, DB_PORT, DB_NAME)
    conn = get_connection()

    try:
        ensure_schema_objects(conn)

        log.info("Читаю исходные данные из %s.%s ...", DB_SCHEMA, DB_SRC_TABLE)
        rows = fetch_source_rows(conn)
        log.info("Прочитано строк: %d", len(rows))

        clear_previous_run(conn)

        all_links = []
        cat_usage = Counter()
        uncategorized = 0

        for i, (id, translation_en) in enumerate(rows, 1):
            cats, matched_text = categorize(translation_en)

            for code in cats:
                all_links.append((id, code, CLASSIFICATOR_METHOD, matched_text))
                cat_usage[code] += 1
            if cats == {"UNCATEGORIZED"}:
                uncategorized += 1

            if i % 250 == 0:
                log.info("  обработано %d / %d слов", i, len(rows))

            if len(all_links) >= BATCH_SIZE * 5:
                insert_links(conn, all_links)
                log.info("  записано %d связей (накопительно)", len(all_links))
                all_links.clear()

        if all_links:
            insert_links(conn, all_links)

        log.info("Классификация слов завершена. Без категории: %d / %d", uncategorized, len(rows))
        log.info("Уникальных категорий использовано: %d", len(cat_usage))

        # после того как DYNAMIC_WN_CATEGORIES полностью собраны прогоном — заливаем справочник
        log.info("Заливаю справочник категорий (categories_claude) ...")
        upsert_categories(conn)

        # топ-20 самых частых категорий — для контроля
        log.info("Топ-20 категорий по частоте использования:")
        for code, cnt in cat_usage.most_common(20):
            name = TAXONOMY.get(code, {}).get("name_en") or DYNAMIC_WN_CATEGORIES.get(code, {}).get("name_en", "?")
            log.info("  %-30s %5d   %s", code, cnt, name)

        log.info("Готово.")

    finally:
        conn.close()
        log.info("Соединение закрыто.")


if __name__ == "__main__":
    main()

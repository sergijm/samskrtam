# -*- coding: utf-8 -*-
"""
sanskrit_taxonomy.py
=====================
Расширенная иерархическая таксономия категорий для классификации
санскритского словаря по полю translation_en.

Структура записи:
    CODE: {
        "parent": <код родителя или None>,
        "name_en": "...",
        "name_ru": "...",
        "desc_en": "...",
        "desc_ru": "...",
        "domain": "<корневой домен>",
        "level": <0..3>,
        "keywords_en": [...],   # слова/фразы для поиска в translation_en (lowercase)
        "keywords_ru": [...],   # слова/фразы для поиска в translation_ru (lowercase)
        "is_leaf": True/False,  # используется ли для непосредственной разметки
    }

Коды организованы по доменам:
    GRAM   — грамматические категории (часть речи, залог, число и т.п.)
    SEM    — общая семантика (предметные области)
    MYTH   — мифология и религия
    NAT    — природа
    BODY   — тело и психика
    SOC    — общество
    ACT    — действия/процессы
    ABSTR  — абстрактные понятия
    MAT    — материальные объекты/артефакты
    QUAL   — качества/свойства
"""

TAXONOMY = {}

def _add(code, parent, name_en, name_ru, desc_en, desc_ru, domain, level,
         kw_en=None, kw_ru=None, is_leaf=True):
    TAXONOMY[code] = {
        "parent": parent,
        "name_en": name_en,
        "name_ru": name_ru,
        "desc_en": desc_en,
        "desc_ru": desc_ru,
        "domain": domain,
        "level": level,
        "keywords_en": kw_en or [],
        "keywords_ru": kw_ru or [],
        "is_leaf": is_leaf,
    }

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: GRAM — грамматические категории
# ════════════════════════════════════════════════════════════════════════
_add("GRAM", None, "Grammar", "Грамматика",
     "Grammatical classification of the word", "Грамматическая классификация слова",
     "GRAM", 0, is_leaf=False)

# ── части речи ──
_add("GRAM.NOUN", "GRAM", "Noun", "Существительное",
     "Substantive / noun form", "Имя существительное",
     "GRAM", 1, kw_en=["noun"], is_leaf=False)
_add("GRAM.NOUN.MASC", "GRAM.NOUN", "Masculine noun", "Существительное мужского рода",
     "Masculine gender noun", "Существительное мужского рода", "GRAM", 2)
_add("GRAM.NOUN.FEM", "GRAM.NOUN", "Feminine noun", "Существительное женского рода",
     "Feminine gender noun", "Существительное женского рода", "GRAM", 2)
_add("GRAM.NOUN.NEUT", "GRAM.NOUN", "Neuter noun", "Существительное среднего рода",
     "Neuter gender noun", "Существительное среднего рода", "GRAM", 2)

_add("GRAM.VERB", "GRAM", "Verb", "Глагол",
     "Verbal form / root", "Глагольная форма / корень",
     "GRAM", 1, is_leaf=False)
_add("GRAM.VERB.ROOT", "GRAM.VERB", "Verbal root (dhātu)", "Глагольный корень (дхату)",
     "Sanskrit verbal root", "Санскритский глагольный корень", "GRAM", 2)
_add("GRAM.VERB.CAUS", "GRAM.VERB", "Causative", "Каузатив",
     "Causative verbal form", "Каузативная форма глагола", "GRAM", 2,
     kw_en=["caus.", "causative"])
_add("GRAM.VERB.PASS", "GRAM.VERB", "Passive", "Пассив (страдательный залог)",
     "Passive voice", "Страдательный залог", "GRAM", 2,
     kw_en=["pass.", "passive"])
_add("GRAM.VERB.DESID", "GRAM.VERB", "Desiderative", "Дезидератив",
     "Desiderative verbal form", "Дезидеративная форма (желательное наклонение)", "GRAM", 2,
     kw_en=["desid.", "desiderative"])
_add("GRAM.VERB.INTENS", "GRAM.VERB", "Intensive/Frequentative", "Интенсив/фреквентатив",
     "Intensive or frequentative verbal form", "Интенсивная/многократная форма глагола", "GRAM", 2,
     kw_en=["intens.", "intensive", "freq.", "frequentative"])
_add("GRAM.VERB.DENOM", "GRAM.VERB", "Denominative", "Деноминатив",
     "Denominative verb (formed from a noun)", "Глагол, образованный от существительного", "GRAM", 2,
     kw_en=["denom.", "denominative"])

_add("GRAM.PART", "GRAM", "Participle", "Причастие",
     "Participial form", "Причастная форма",
     "GRAM", 1, is_leaf=False)
_add("GRAM.PART.PRES", "GRAM.PART", "Present participle", "Причастие настоящего времени",
     "Present participle", "Причастие настоящего времени", "GRAM", 2,
     kw_en=["pr. part.", "present participle"])
_add("GRAM.PART.PAST", "GRAM.PART", "Past participle", "Причастие прошедшего времени",
     "Past (passive) participle", "Причастие прошедшего времени (страдательное)", "GRAM", 2,
     kw_en=["p.p.", "past participle", "pp."])
_add("GRAM.PART.FUT", "GRAM.PART", "Future participle / gerundive", "Причастие будущего времени / герундив",
     "Future participle / gerundive (kṛtya)", "Причастие будущего времени / герундив", "GRAM", 2,
     kw_en=["fut. part.", "gerundive", "ger."])
_add("GRAM.PART.PERF", "GRAM.PART", "Perfect participle", "Причастие перфекта",
     "Perfect active participle", "Причастие перфекта действительного залога", "GRAM", 2,
     kw_en=["p.pf.", "perfect participle"])

_add("GRAM.ADJ", "GRAM", "Adjective", "Прилагательное",
     "Adjective", "Имя прилагательное",
     "GRAM", 1, kw_en=["adj."], is_leaf=True)

_add("GRAM.PRON", "GRAM", "Pronoun", "Местоимение",
     "Pronoun", "Местоимение",
     "GRAM", 1, kw_en=["pron."], is_leaf=False)
_add("GRAM.PRON.PERS", "GRAM.PRON", "Personal pronoun", "Личное местоимение",
     "Personal pronoun", "Личное местоимение", "GRAM", 2)
_add("GRAM.PRON.DEM", "GRAM.PRON", "Demonstrative pronoun", "Указательное местоимение",
     "Demonstrative pronoun", "Указательное местоимение", "GRAM", 2)
_add("GRAM.PRON.REL", "GRAM.PRON", "Relative pronoun", "Относительное местоимение",
     "Relative pronoun", "Относительное местоимение", "GRAM", 2)
_add("GRAM.PRON.INTERR", "GRAM.PRON", "Interrogative pronoun", "Вопросительное местоимение",
     "Interrogative pronoun", "Вопросительное местоимение", "GRAM", 2)

_add("GRAM.NUM", "GRAM", "Numeral", "Числительное",
     "Numeral", "Числительное",
     "GRAM", 1, kw_en=["num.", "numeral"], is_leaf=True)

_add("GRAM.IND", "GRAM", "Indeclinable", "Неизменяемое слово",
     "Indeclinable word", "Неизменяемое слово",
     "GRAM", 1, kw_en=["ind."], is_leaf=False)
_add("GRAM.IND.ADV", "GRAM.IND", "Adverb", "Наречие",
     "Adverb", "Наречие", "GRAM", 2, kw_en=["adv."])
_add("GRAM.IND.PREP", "GRAM.IND", "Preposition", "Предлог",
     "Preposition / preverb", "Предлог / превербий", "GRAM", 2,
     kw_en=["prep."])
_add("GRAM.IND.CONJ", "GRAM.IND", "Conjunction", "Союз",
     "Conjunction", "Союз", "GRAM", 2, kw_en=["conj."])
_add("GRAM.IND.INTERJ", "GRAM.IND", "Interjection", "Междометие",
     "Interjection", "Междометие", "GRAM", 2, kw_en=["interj."])
_add("GRAM.IND.PARTICLE", "GRAM.IND", "Particle", "Частица",
     "Particle", "Частица", "GRAM", 2, kw_en=["particle"])
_add("GRAM.IND.PREVERB", "GRAM.IND", "Verbal prefix (upasarga)", "Глагольная приставка (упасарга)",
     "Verbal prefix attached to verbal roots", "Приставка, присоединяемая к глагольным корням", "GRAM", 2)

_add("GRAM.GERUND", "GRAM", "Gerund (absolutive)", "Деепричастие (абсолютив)",
     "Gerund / absolutive (ktvā, lyap)", "Деепричастие / абсолютив", "GRAM", 1,
     kw_en=["ger.", "gerund", "absol."])
_add("GRAM.INF", "GRAM", "Infinitive", "Инфинитив",
     "Infinitive", "Неопределённая форма глагола", "GRAM", 1, kw_en=["inf."])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: MYTH — мифология и религия
# ════════════════════════════════════════════════════════════════════════
_add("MYTH", None, "Mythology & Religion", "Мифология и религия",
     "Mythological and religious concepts", "Мифологические и религиозные понятия",
     "MYTH", 0, is_leaf=False)

_add("MYTH.DEITY", "MYTH", "Deity", "Божество",
     "Gods and goddesses", "Боги и богини",
     "MYTH", 1, is_leaf=False)
_add("MYTH.DEITY.VEDIC", "MYTH.DEITY", "Vedic deity", "Ведийское божество",
     "Deity of the Vedic pantheon", "Божество ведийского пантеона", "MYTH", 2,
     kw_en=["indra", "agni", "varuna", "mitra", "soma", "vayu", "surya", "usas",
            "rudra", "marut", "asvin", "vedic god", "vedic deity"])
_add("MYTH.DEITY.PURANIC", "MYTH.DEITY", "Puranic/epic deity", "Пуранический/эпический бог",
     "Deity of the Puranic or epic pantheon", "Божество пуранического или эпического пантеона", "MYTH", 2,
     kw_en=["vishnu", "shiva", "brahma", "krishna", "rama", "ganesha", "hanuman",
            "durga", "kali", "lakshmi", "parvati", "saraswati", "skanda", "kartikeya"])
_add("MYTH.DEITY.EPITHET", "MYTH.DEITY", "Divine epithet", "Эпитет божества",
     "Epithet or attribute name of a deity", "Эпитет или атрибутивное имя божества", "MYTH", 2,
     kw_en=["epithet of", "name of vishnu", "name of shiva", "name of krishna",
            "name of indra", "name of the sun", "name of brahma"])
_add("MYTH.DEITY.GODDESS", "MYTH.DEITY", "Goddess", "Богиня",
     "Female deity", "Женское божество", "MYTH", 2,
     kw_en=["goddess"])

_add("MYTH.DEMON", "MYTH", "Demon / Spirit", "Демон / дух",
     "Demons, asuras, rakshasas, evil spirits", "Демоны, асуры, ракшасы, злые духи",
     "MYTH", 1, kw_en=["demon", "asura", "rakshasa", "rakṣas", "evil spirit", "daitya", "danava"])
_add("MYTH.SPIRIT.BENEV", "MYTH", "Benevolent spirit/being", "Благое мифическое существо",
     "Beneficent mythical or semi-divine being", "Благое мифическое или полубожественное существо",
     "MYTH", 1, kw_en=["yaksha", "gandharva", "apsaras", "kinnara", "nymph", "celestial musician"])

_add("MYTH.COSMOLOGY", "MYTH", "Cosmology", "Космология",
     "Mythological cosmology, worlds, ages", "Мифологическая космология, миры, эпохи",
     "MYTH", 1, is_leaf=False)
_add("MYTH.COSMOLOGY.WORLD", "MYTH.COSMOLOGY", "World/Realm", "Мир/сфера",
     "Cosmic world or realm (loka)", "Космический мир или сфера (лока)", "MYTH", 2,
     kw_en=["world", "heaven", "hell", "netherworld", "realm", "loka", "paradise"])
_add("MYTH.COSMOLOGY.AGE", "MYTH.COSMOLOGY", "Cosmic age (yuga)", "Космическая эпоха (юга)",
     "Cosmic time cycle / age", "Космический временной цикл / эпоха", "MYTH", 2,
     kw_en=["yuga", "kalpa", "cosmic age", "world age"])
_add("MYTH.COSMOLOGY.MOUNTAIN", "MYTH.COSMOLOGY", "Mythical mountain", "Мифическая гора",
     "Sacred or mythical mountain", "Священная или мифическая гора", "MYTH", 2,
     kw_en=["meru", "mandara", "kailasa", "mythical mountain"])

_add("MYTH.RITUAL", "MYTH", "Ritual", "Ритуал",
     "Religious ritual and ceremony", "Религиозный ритуал и церемония",
     "MYTH", 1, is_leaf=False)
_add("MYTH.RITUAL.SACRIFICE", "MYTH.RITUAL", "Sacrifice (yajña)", "Жертвоприношение (яджна)",
     "Sacrificial rite", "Жертвенный обряд", "MYTH", 2,
     kw_en=["sacrifice", "sacrificial", "oblation", "offering", "yajna", "yajña"])
_add("MYTH.RITUAL.FIRE", "MYTH.RITUAL", "Sacred fire / fire ritual", "Священный огонь / огненный ритуал",
     "Sacred fire and fire-related ritual", "Священный огонь и связанный с ним ритуал", "MYTH", 2,
     kw_en=["sacred fire", "fire altar", "homa", "agnihotra", "fire-pit"])
_add("MYTH.RITUAL.MANTRA", "MYTH.RITUAL", "Mantra / sacred formula", "Мантра / священная формула",
     "Mantra, sacred verbal formula, hymn", "Мантра, священная словесная формула, гимн", "MYTH", 2,
     kw_en=["mantra", "sacred formula", "incantation", "hymn", "chant"])
_add("MYTH.RITUAL.PRIEST", "MYTH.RITUAL", "Priest / officiant", "Жрец / служитель культа",
     "Priestly office or officiant", "Жреческая должность или служитель ритуала", "MYTH", 2,
     kw_en=["priest", "officiating priest", "hotri", "brahmin priest", "purohita"])
_add("MYTH.RITUAL.WORSHIP", "MYTH.RITUAL", "Worship / devotion", "Почитание / поклонение",
     "Worship, veneration, devotion", "Поклонение, почитание, преданность", "MYTH", 2,
     kw_en=["worship", "veneration", "devotion", "adoration", "homage", "reverence"])
_add("MYTH.RITUAL.PURIFICATION", "MYTH.RITUAL", "Purification rite", "Очистительный обряд",
     "Ritual purification", "Ритуальное очищение", "MYTH", 2,
     kw_en=["purification", "purify", "ablution", "consecration"])

_add("MYTH.PHIL", "MYTH", "Philosophical/Soteriological concept", "Философское/сотериологическое понятие",
     "Core philosophical-religious concepts", "Ключевые философско-религиозные понятия",
     "MYTH", 1, is_leaf=False)
_add("MYTH.PHIL.DHARMA", "MYTH.PHIL", "Dharma / cosmic law", "Дхарма / космический закон",
     "Duty, righteousness, cosmic law", "Долг, праведность, космический закон", "MYTH", 2,
     kw_en=["dharma", "duty", "righteousness", "righteous law", "religious duty"])
_add("MYTH.PHIL.KARMA", "MYTH.PHIL", "Karma / action and consequence", "Карма / действие и следствие",
     "Action and its moral consequence", "Действие и его нравственное следствие", "MYTH", 2,
     kw_en=["karma", "deed", "act and consequence", "fruit of action"])
_add("MYTH.PHIL.MOKSHA", "MYTH.PHIL", "Liberation (moksha)", "Освобождение (мокша)",
     "Spiritual liberation, release from rebirth", "Духовное освобождение, избавление от перерождений", "MYTH", 2,
     kw_en=["liberation", "moksha", "mukti", "release from rebirth", "salvation", "emancipation"])
_add("MYTH.PHIL.ATMAN", "MYTH.PHIL", "Self / soul (ātman)", "Душа / Я (атман)",
     "Individual self or soul, atman/brahman", "Индивидуальное Я или душа, атман/брахман", "MYTH", 2,
     kw_en=["soul", "self", "atman", "spirit (metaphysical)", "individual self"])
_add("MYTH.PHIL.MAYA", "MYTH.PHIL", "Illusion (māyā)", "Иллюзия (майя)",
     "Cosmic illusion, appearance vs reality", "Космическая иллюзия, видимость в противовес реальности", "MYTH", 2,
     kw_en=["illusion", "maya", "māyā", "magic power", "deceptive appearance"])
_add("MYTH.PHIL.YOGA", "MYTH.PHIL", "Yoga / spiritual discipline", "Йога / духовная практика",
     "Yogic discipline and meditative practice", "Йогическая дисциплина и медитативная практика", "MYTH", 2,
     kw_en=["yoga", "meditation", "concentration", "spiritual discipline", "ascetic practice"])
_add("MYTH.PHIL.REBIRTH", "MYTH.PHIL", "Rebirth / transmigration", "Перерождение / переселение душ",
     "Cycle of rebirth, transmigration of souls", "Цикл перерождений, переселение душ", "MYTH", 2,
     kw_en=["rebirth", "transmigration", "samsara", "reincarnation", "cycle of existence"])

_add("MYTH.TEXT", "MYTH", "Sacred text / scripture", "Священный текст / писание",
     "Scripture, sacred literature, treatise", "Писание, священная литература, трактат",
     "MYTH", 1, kw_en=["veda", "scripture", "sacred text", "treatise", "sutra", "upanishad",
                         "sacred book", "vedic text", "shastra", "śāstra"])

_add("MYTH.ASCETIC", "MYTH", "Ascetic / renunciant", "Аскет / отшельник",
     "Ascetic, hermit, renunciate practitioner", "Аскет, отшельник, практикующий отречение",
     "MYTH", 1, kw_en=["ascetic", "hermit", "sage", "seer", "renunciate", "mendicant", "anchorite",
                         "muni", "rishi", "ṛṣi", "yogin", "penance", "austerity"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: NAT — природа
# ════════════════════════════════════════════════════════════════════════
_add("NAT", None, "Nature", "Природа",
     "Natural world: phenomena, flora, fauna, geography", "Природный мир: явления, флора, фауна, география",
     "NAT", 0, is_leaf=False)

_add("NAT.ANIMAL", "NAT", "Animal", "Животное",
     "Animal", "Животное", "NAT", 1, is_leaf=False)
_add("NAT.ANIMAL.MAMMAL", "NAT.ANIMAL", "Mammal", "Млекопитающее",
     "Mammal", "Млекопитающее", "NAT", 2,
     kw_en=["cow", "bull", "ox", "horse", "elephant", "lion", "tiger", "deer", "antelope",
            "monkey", "dog", "jackal", "buffalo", "goat", "sheep", "camel", "boar", "rat", "mouse"])
_add("NAT.ANIMAL.BIRD", "NAT.ANIMAL", "Bird", "Птица",
     "Bird", "Птица", "NAT", 2,
     kw_en=["bird", "peacock", "swan", "goose", "crow", "parrot", "eagle", "hawk", "vulture",
            "cuckoo", "pigeon", "dove", "owl"])
_add("NAT.ANIMAL.REPTILE", "NAT.ANIMAL", "Reptile / Serpent", "Рептилия / Змея",
     "Reptile, snake, serpent-being", "Рептилия, змея, змеиное существо", "NAT", 2,
     kw_en=["snake", "serpent", "naga", "nāga", "lizard", "crocodile", "tortoise", "turtle"])
_add("NAT.ANIMAL.FISH", "NAT.ANIMAL", "Fish / Aquatic animal", "Рыба / Водное животное",
     "Fish or aquatic creature", "Рыба или водное существо", "NAT", 2,
     kw_en=["fish", "aquatic animal", "crab", "shell", "mollusc"])
_add("NAT.ANIMAL.INSECT", "NAT.ANIMAL", "Insect", "Насекомое",
     "Insect or small invertebrate", "Насекомое или мелкое беспозвоночное", "NAT", 2,
     kw_en=["insect", "bee", "ant", "fly", "worm", "scorpion", "spider", "butterfly"])
_add("NAT.ANIMAL.MYTHIC", "NAT.ANIMAL", "Mythical animal", "Мифическое животное",
     "Mythical or composite animal", "Мифическое или составное животное", "NAT", 2,
     kw_en=["garuda", "mythical bird", "mythical beast", "mythical creature", "vahana"])
_add("NAT.ANIMAL.GENERIC", "NAT.ANIMAL", "Animal (generic)", "Животное (общее)",
     "Generic animal term", "Общее обозначение животного", "NAT", 2,
     kw_en=["animal", "beast", "creature", "quadruped", "wild animal", "domestic animal", "cattle"])

_add("NAT.PLANT", "NAT", "Plant", "Растение",
     "Plant", "Растение", "NAT", 1, is_leaf=False)
_add("NAT.PLANT.TREE", "NAT.PLANT", "Tree", "Дерево",
     "Tree", "Дерево", "NAT", 2,
     kw_en=["tree", "banyan", "fig tree", "palm", "bamboo", "wood (tree)"])
_add("NAT.PLANT.FLOWER", "NAT.PLANT", "Flower", "Цветок",
     "Flower", "Цветок", "NAT", 2,
     kw_en=["flower", "lotus", "blossom", "bloom", "jasmine"])
_add("NAT.PLANT.HERB", "NAT.PLANT", "Herb / grass", "Трава / злак",
     "Herb, grass, plant (small)", "Трава, злак, мелкое растение", "NAT", 2,
     kw_en=["herb", "grass", "grain", "barley", "rice", "wheat", "sacred grass", "kusha"])
_add("NAT.PLANT.FRUIT", "NAT.PLANT", "Fruit", "Плод",
     "Fruit", "Плод", "NAT", 2,
     kw_en=["fruit", "mango", "berry", "seed pod"])
_add("NAT.PLANT.ROOT", "NAT.PLANT", "Root / tuber", "Корень / клубень",
     "Plant root or tuber", "Корень или клубень растения", "NAT", 2,
     kw_en=["root (plant)", "tuber", "bulb"])
_add("NAT.PLANT.GENERIC", "NAT.PLANT", "Plant (generic)", "Растение (общее)",
     "Generic plant term", "Общее обозначение растения", "NAT", 2,
     kw_en=["plant", "shrub", "creeper", "vine", "vegetation", "foliage"])

_add("NAT.METEOR", "NAT", "Weather & Atmosphere", "Погода и атмосфера",
     "Meteorological phenomena", "Метеорологические явления",
     "NAT", 1, kw_en=["cloud", "rain", "thunder", "lightning", "storm", "wind", "mist",
                        "fog", "dew", "frost", "snow", "weather"])

_add("NAT.ASTRO", "NAT", "Astronomy & Sky", "Астрономия и небо",
     "Celestial bodies and phenomena", "Небесные тела и явления",
     "NAT", 1, is_leaf=False)
_add("NAT.ASTRO.SUN", "NAT.ASTRO", "Sun", "Солнце",
     "Sun and solar phenomena", "Солнце и солнечные явления", "NAT", 2,
     kw_en=["sun", "solar"])
_add("NAT.ASTRO.MOON", "NAT.ASTRO", "Moon", "Луна",
     "Moon and lunar phenomena", "Луна и лунные явления", "NAT", 2,
     kw_en=["moon", "lunar"])
_add("NAT.ASTRO.STAR", "NAT.ASTRO", "Star / constellation", "Звезда / созвездие",
     "Star, planet, constellation", "Звезда, планета, созвездие", "NAT", 2,
     kw_en=["star", "constellation", "planet", "asterism", "nakshatra"])
_add("NAT.ASTRO.SKY", "NAT.ASTRO", "Sky / firmament", "Небо / небосвод",
     "Sky, firmament, heavens (physical)", "Небо, небосвод (физический)", "NAT", 2,
     kw_en=["sky", "firmament", "heaven (sky)", "atmosphere"])

_add("NAT.TIME", "NAT", "Time", "Время",
     "Temporal concepts", "Временные понятия",
     "NAT", 1, is_leaf=False)
_add("NAT.TIME.UNIT", "NAT.TIME", "Time unit", "Единица времени",
     "Unit of time measurement", "Единица измерения времени", "NAT", 2,
     kw_en=["year", "month", "day", "night", "hour", "moment", "instant", "season", "fortnight"])
_add("NAT.TIME.SEASON", "NAT.TIME", "Season", "Сезон/время года",
     "Season of the year", "Время года", "NAT", 2,
     kw_en=["spring", "summer", "autumn", "winter", "monsoon", "rainy season"])
_add("NAT.TIME.PERIOD", "NAT.TIME", "Period / Era", "Период / эпоха",
     "Extended period or era", "Продолжительный период или эпоха", "NAT", 2,
     kw_en=["age", "era", "epoch", "period of time"])

_add("NAT.GEO", "NAT", "Geography & Landscape", "География и ландшафт",
     "Geographical and landscape features", "Географические и ландшафтные объекты",
     "NAT", 1, is_leaf=False)
_add("NAT.GEO.MOUNTAIN", "NAT.GEO", "Mountain / hill", "Гора / холм",
     "Mountain or hill", "Гора или холм", "NAT", 2,
     kw_en=["mountain", "hill", "peak", "cliff", "rock"])
_add("NAT.GEO.WATER", "NAT.GEO", "Water body", "Водоём",
     "River, ocean, lake, water body", "Река, океан, озеро, водоём", "NAT", 2,
     kw_en=["river", "ocean", "sea", "lake", "pond", "stream", "water (body)", "spring (water)"])
_add("NAT.GEO.FOREST", "NAT.GEO", "Forest / wilderness", "Лес / дикая природа",
     "Forest or wilderness", "Лес или дикая местность", "NAT", 2,
     kw_en=["forest", "wood (forest)", "jungle", "wilderness", "grove"])
_add("NAT.GEO.LAND", "NAT.GEO", "Land / terrain", "Земля / местность",
     "Land, ground, terrain feature", "Земля, почва, элемент рельефа", "NAT", 2,
     kw_en=["earth (ground)", "land", "soil", "ground", "field", "plain", "desert", "valley"])

_add("NAT.ELEMENT", "NAT", "Element (bhūta)", "Стихия (бхута)",
     "Classical element (earth, water, fire, air, ether)", "Классическая стихия (земля, вода, огонь, воздух, эфир)",
     "NAT", 1, kw_en=["element", "ether", "fire (element)", "water (element)", "air (element)",
                        "earth (element)"])

_add("NAT.SUBSTANCE", "NAT", "Natural substance", "Природное вещество",
     "Mineral, metal, natural material", "Минерал, металл, природный материал",
     "NAT", 1, is_leaf=False)
_add("NAT.SUBSTANCE.METAL", "NAT.SUBSTANCE", "Metal", "Металл",
     "Metal", "Металл", "NAT", 2,
     kw_en=["gold", "silver", "iron", "copper", "bronze", "metal", "tin", "lead"])
_add("NAT.SUBSTANCE.GEM", "NAT.SUBSTANCE", "Gem / precious stone", "Драгоценный камень",
     "Gem, jewel, precious stone", "Драгоценный камень, самоцвет", "NAT", 2,
     kw_en=["gem", "jewel", "precious stone", "pearl", "diamond", "ruby", "crystal"])
_add("NAT.SUBSTANCE.MINERAL", "NAT.SUBSTANCE", "Mineral / stone", "Минерал / камень",
     "Stone, mineral, mineral substance", "Камень, минерал, минеральное вещество", "NAT", 2,
     kw_en=["stone", "mineral", "salt", "clay"])
_add("NAT.SUBSTANCE.LIQUID", "NAT.SUBSTANCE", "Liquid substance", "Жидкое вещество",
     "Liquid or fluid substance", "Жидкое или текучее вещество", "NAT", 2,
     kw_en=["water (substance)", "milk", "honey", "oil", "juice", "nectar", "blood", "liquid"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: BODY — тело и психика
# ════════════════════════════════════════════════════════════════════════
_add("BODY", None, "Body & Mind", "Тело и разум",
     "Human/animal body and mental phenomena", "Тело человека/животного и психические явления",
     "BODY", 0, is_leaf=False)

_add("BODY.PART", "BODY", "Body part", "Часть тела",
     "Anatomical body part", "Анатомическая часть тела",
     "BODY", 1, is_leaf=False)
_add("BODY.PART.HEAD", "BODY.PART", "Head / face", "Голова / лицо",
     "Head and facial features", "Голова и черты лица", "BODY", 2,
     kw_en=["head", "face", "forehead", "eye", "ear", "nose", "mouth", "lip", "tooth", "chin", "cheek", "hair"])
_add("BODY.PART.LIMB", "BODY.PART", "Limb", "Конечность",
     "Arm, leg, hand, foot", "Рука, нога, кисть, стопа", "BODY", 2,
     kw_en=["hand", "arm", "leg", "foot", "finger", "toe", "knee", "elbow", "shoulder"])
_add("BODY.PART.TORSO", "BODY.PART", "Torso / trunk", "Туловище",
     "Chest, back, belly, torso", "Грудь, спина, живот, туловище", "BODY", 2,
     kw_en=["chest", "back", "belly", "stomach", "waist", "trunk (body)", "breast", "hip"])
_add("BODY.PART.INTERNAL", "BODY.PART", "Internal organ", "Внутренний орган",
     "Internal organ", "Внутренний орган", "BODY", 2,
     kw_en=["heart (organ)", "liver", "lung", "bone", "marrow", "vein", "artery", "internal organ"])
_add("BODY.PART.SKIN", "BODY.PART", "Skin / hide", "Кожа / шкура",
     "Skin, hide, covering of the body", "Кожа, шкура, покров тела", "BODY", 2,
     kw_en=["skin", "hide", "fur"])

_add("BODY.FUNCTION", "BODY", "Bodily function/process", "Телесная функция/процесс",
     "Physiological process", "Физиологический процесс",
     "BODY", 1, kw_en=["breath", "breathing", "digestion", "growth (bodily)", "birth", "death",
                         "sleep", "hunger", "thirst", "fatigue", "illness", "disease", "wound", "injury"])

_add("BODY.MIND", "BODY", "Mind / cognition", "Разум / познание",
     "Mental faculties and cognitive processes", "Психические способности и познавательные процессы",
     "BODY", 1, is_leaf=False)
_add("BODY.MIND.THOUGHT", "BODY.MIND", "Thought / reasoning", "Мысль / рассуждение",
     "Thinking, reasoning, intellect", "Мышление, рассуждение, интеллект", "BODY", 2,
     kw_en=["thought", "thinking", "mind", "intellect", "reason", "reasoning", "understanding",
            "consideration", "reflection", "judgment"])
_add("BODY.MIND.MEMORY", "BODY.MIND", "Memory", "Память",
     "Memory and recollection", "Память и воспоминание", "BODY", 2,
     kw_en=["memory", "remembrance", "recollection", "forgetting", "forgetfulness"])
_add("BODY.MIND.PERCEPTION", "BODY.MIND", "Perception", "Восприятие",
     "Sensory perception", "Чувственное восприятие", "BODY", 2,
     kw_en=["perception", "seeing", "sight", "hearing", "sense", "vision", "observation"])
_add("BODY.MIND.WILL", "BODY.MIND", "Will / intention", "Воля / намерение",
     "Volition, intention, desire", "Воля, намерение, желание", "BODY", 2,
     kw_en=["will", "intention", "desire", "wish", "purpose", "resolve", "determination"])
_add("BODY.MIND.KNOWLEDGE", "BODY.MIND", "Knowledge", "Знание",
     "Knowledge and learning", "Знание и обучение", "BODY", 2,
     kw_en=["knowledge", "learning", "wisdom", "science", "lore", "erudition"])

_add("BODY.EMOTION", "BODY", "Emotion / feeling", "Эмоция / чувство",
     "Emotional states", "Эмоциональные состояния",
     "BODY", 1, is_leaf=False)
_add("BODY.EMOTION.JOY", "BODY.EMOTION", "Joy / happiness", "Радость / счастье",
     "Joy, happiness, delight", "Радость, счастье, восторг", "BODY", 2,
     kw_en=["joy", "happiness", "delight", "pleasure", "gladness", "bliss", "cheerfulness"])
_add("BODY.EMOTION.SORROW", "BODY.EMOTION", "Sorrow / grief", "Печаль / горе",
     "Sorrow, grief, sadness", "Печаль, горе, грусть", "BODY", 2,
     kw_en=["sorrow", "grief", "sadness", "distress", "misery", "lamentation", "suffering", "pain"])
_add("BODY.EMOTION.FEAR", "BODY.EMOTION", "Fear", "Страх",
     "Fear, terror, anxiety", "Страх, ужас, тревога", "BODY", 2,
     kw_en=["fear", "fright", "terror", "dread", "afraid", "fearless", "anxiety"])
_add("BODY.EMOTION.ANGER", "BODY.EMOTION", "Anger", "Гнев",
     "Anger, wrath", "Гнев, ярость", "BODY", 2,
     kw_en=["anger", "wrath", "rage", "fury", "irritation"])
_add("BODY.EMOTION.LOVE", "BODY.EMOTION", "Love / affection", "Любовь / привязанность",
     "Love, affection, desire (romantic)", "Любовь, привязанность, желание (романтическое)", "BODY", 2,
     kw_en=["love", "affection", "passion", "lust", "longing", "attachment", "fondness"])
_add("BODY.EMOTION.PRIDE", "BODY.EMOTION", "Pride / arrogance", "Гордость / высокомерие",
     "Pride, arrogance, vanity", "Гордость, высокомерие, тщеславие", "BODY", 2,
     kw_en=["pride", "arrogance", "vanity", "haughtiness", "conceit"])
_add("BODY.EMOTION.SHAME", "BODY.EMOTION", "Shame / modesty", "Стыд / скромность",
     "Shame, modesty, embarrassment", "Стыд, скромность, смущение", "BODY", 2,
     kw_en=["shame", "modesty", "embarrassment", "bashfulness"])
_add("BODY.EMOTION.COMPASSION", "BODY.EMOTION", "Compassion / pity", "Сострадание / жалость",
     "Compassion, pity, mercy", "Сострадание, жалость, милосердие", "BODY", 2,
     kw_en=["compassion", "pity", "mercy", "sympathy", "kindness"])
_add("BODY.EMOTION.ENVY", "BODY.EMOTION", "Envy / jealousy", "Зависть / ревность",
     "Envy, jealousy, malice", "Зависть, ревность, злоба", "BODY", 2,
     kw_en=["envy", "jealousy", "malice", "spite"])
_add("BODY.EMOTION.HATE", "BODY.EMOTION", "Hatred / hostility", "Ненависть / враждебность",
     "Hatred, hostility, aversion", "Ненависть, враждебность, неприязнь", "BODY", 2,
     kw_en=["hate", "hatred", "hostility", "aversion", "enmity", "dislike"])
_add("BODY.EMOTION.SURPRISE", "BODY.EMOTION", "Surprise / wonder", "Удивление",
     "Surprise, wonder, astonishment", "Удивление, изумление", "BODY", 2,
     kw_en=["surprise", "wonder", "astonishment", "amazement"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: SOC — общество
# ════════════════════════════════════════════════════════════════════════
_add("SOC", None, "Society & Culture", "Общество и культура",
     "Social institutions, roles, and practices", "Социальные институты, роли и практики",
     "SOC", 0, is_leaf=False)

_add("SOC.ROLE", "SOC", "Social role / title", "Социальная роль / титул",
     "Social position, title, occupation", "Социальное положение, титул, профессия",
     "SOC", 1, is_leaf=False)
_add("SOC.ROLE.ROYAL", "SOC.ROLE", "King / royalty", "Царь / царская особа",
     "King, queen, royal title", "Царь, царица, царский титул", "SOC", 2,
     kw_en=["king", "queen", "prince", "princess", "monarch", "sovereign", "royal", "ruler"])
_add("SOC.ROLE.WARRIOR", "SOC.ROLE", "Warrior / soldier", "Воин / солдат",
     "Warrior, soldier, fighter", "Воин, солдат, боец", "SOC", 2,
     kw_en=["warrior", "soldier", "fighter", "hero", "champion", "kshatriya"])
_add("SOC.ROLE.BRAHMIN", "SOC.ROLE", "Brahmin / scholar", "Брахман / учёный",
     "Brahmin, priestly scholar caste", "Брахман, учёная жреческая каста", "SOC", 2,
     kw_en=["brahmin", "brāhmaṇa", "scholar (caste)"])
_add("SOC.ROLE.SERVANT", "SOC.ROLE", "Servant / attendant", "Слуга / прислужник",
     "Servant, attendant, slave", "Слуга, прислужник, раб", "SOC", 2,
     kw_en=["servant", "attendant", "slave", "maid", "menial"])
_add("SOC.ROLE.TEACHER", "SOC.ROLE", "Teacher / preceptor", "Учитель / наставник",
     "Teacher, preceptor, guru", "Учитель, наставник, гуру", "SOC", 2,
     kw_en=["teacher", "preceptor", "guru", "instructor", "tutor"])
_add("SOC.ROLE.STUDENT", "SOC.ROLE", "Student / disciple", "Ученик / последователь",
     "Student, disciple, pupil", "Ученик, последователь", "SOC", 2,
     kw_en=["student", "disciple", "pupil"])
_add("SOC.ROLE.MERCHANT", "SOC.ROLE", "Merchant / trader", "Купец / торговец",
     "Merchant, trader, craftsman caste", "Купец, торговец, ремесленная каста", "SOC", 2,
     kw_en=["merchant", "trader", "vaishya", "craftsman", "artisan"])
_add("SOC.ROLE.OUTCAST", "SOC.ROLE", "Outcast / low caste", "Изгой / низшая каста",
     "Outcast, low-caste person", "Изгой, представитель низшей касты", "SOC", 2,
     kw_en=["outcast", "shudra", "śūdra", "low caste"])

_add("SOC.FAMILY", "SOC", "Family / kinship", "Семья / родство",
     "Kinship terms", "Термины родства",
     "SOC", 1, kw_en=["father", "mother", "son", "daughter", "brother", "sister", "wife",
                        "husband", "child", "parent", "ancestor", "kinsman", "relative", "family",
                        "grandfather", "grandmother", "uncle", "aunt", "cousin", "nephew", "niece"])

_add("SOC.MARRIAGE", "SOC", "Marriage / relationship", "Брак / отношения",
     "Marriage and romantic relationships", "Брак и романтические отношения",
     "SOC", 1, kw_en=["marriage", "wedding", "bride", "bridegroom", "spouse", "wedlock"])

_add("SOC.POLITY", "SOC", "Polity / governance", "Государство / управление",
     "Political and administrative concepts", "Политические и административные понятия",
     "SOC", 1, is_leaf=False)
_add("SOC.POLITY.LAW", "SOC.POLITY", "Law / justice", "Закон / правосудие",
     "Law, justice, legal matters", "Закон, правосудие, юридические вопросы", "SOC", 2,
     kw_en=["law", "justice", "judge", "punishment", "court", "verdict", "legal", "rule (law)"])
_add("SOC.POLITY.WAR", "SOC.POLITY", "War / battle", "Война / битва",
     "War, battle, combat", "Война, битва, бой", "SOC", 2,
     kw_en=["war", "battle", "combat", "fight", "conflict", "army", "troop", "victory", "defeat (war)"])
_add("SOC.POLITY.GOVERNANCE", "SOC.POLITY", "Governance / administration", "Управление / администрация",
     "Administration and governance", "Администрация и управление", "SOC", 2,
     kw_en=["government", "administration", "council", "assembly", "minister", "kingdom", "realm (political)"])
_add("SOC.POLITY.TAX", "SOC.POLITY", "Tax / tribute", "Налог / дань",
     "Tax, tribute, revenue", "Налог, дань, доход казны", "SOC", 2,
     kw_en=["tax", "tribute", "revenue", "toll"])

_add("SOC.ECON", "SOC", "Economy / commerce", "Экономика / торговля",
     "Trade, wealth, commerce", "Торговля, богатство, коммерция",
     "SOC", 1, kw_en=["wealth", "riches", "money", "gold (currency)", "trade", "commerce", "price",
                        "purchase", "sale", "debt", "loan", "property", "possession (wealth)"])

_add("SOC.ART", "SOC", "Art / music / literature", "Искусство / музыка / литература",
     "Artistic and literary culture", "Художественная и литературная культура",
     "SOC", 1, is_leaf=False)
_add("SOC.ART.MUSIC", "SOC.ART", "Music / sound (artistic)", "Музыка / звучание (художественное)",
     "Music, musical instrument, song", "Музыка, музыкальный инструмент, песня", "SOC", 2,
     kw_en=["music", "song", "melody", "musical instrument", "drum", "lute", "flute", "singing", "dance"])
_add("SOC.ART.POETRY", "SOC.ART", "Poetry / literature", "Поэзия / литература",
     "Poetic and literary composition", "Поэтическое и литературное произведение", "SOC", 2,
     kw_en=["poem", "poetry", "verse", "literature", "story", "narrative", "drama", "play (drama)"])
_add("SOC.ART.VISUAL", "SOC.ART", "Visual/decorative art", "Изобразительное/декоративное искусство",
     "Painting, sculpture, ornament", "Живопись, скульптура, орнамент", "SOC", 2,
     kw_en=["painting", "sculpture", "ornament", "decoration", "image (art)", "picture"])

_add("SOC.CUSTOM", "SOC", "Custom / etiquette", "Обычай / этикет",
     "Social custom and etiquette", "Социальный обычай и этикет",
     "SOC", 1, kw_en=["custom", "tradition", "etiquette", "manner", "habit", "convention"])

_add("SOC.SPEECH", "SOC", "Speech / communication", "Речь / коммуникация",
     "Speaking, language, communication", "Говорение, язык, коммуникация",
     "SOC", 1, is_leaf=False)
_add("SOC.SPEECH.ACT", "SOC.SPEECH", "Act of speaking", "Акт речи",
     "To speak, say, tell, declare", "Говорить, сказать, поведать, объявить", "SOC", 2,
     kw_en=["to speak", "to say", "to tell", "to declare", "to address", "to converse",
            "to confess", "speaking", "to utter", "to proclaim", "to announce"])
_add("SOC.SPEECH.QUESTION", "SOC.SPEECH", "Question / inquiry", "Вопрос / расспрос",
     "To ask, question, inquire", "Спрашивать, расспрашивать", "SOC", 2,
     kw_en=["to ask", "question", "inquiry", "to inquire", "to interrogate"])
_add("SOC.SPEECH.LANGUAGE", "SOC.SPEECH", "Language / word", "Язык / слово",
     "Language, word, name, term", "Язык, слово, имя, термин", "SOC", 2,
     kw_en=["language", "word", "name", "term", "appellation", "designation", "speech (language)"])
_add("SOC.SPEECH.PRAISE", "SOC.SPEECH", "Praise / blame", "Похвала / порицание",
     "Praise, blame, censure", "Похвала, порицание, осуждение", "SOC", 2,
     kw_en=["praise", "blame", "censure", "reproach", "eulogy", "commendation"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: ACT — действия и процессы
# ════════════════════════════════════════════════════════════════════════
_add("ACT", None, "Action & Process", "Действие и процесс",
     "Verbal actions and processes (non-speech)", "Глагольные действия и процессы (кроме речи)",
     "ACT", 0, is_leaf=False)

_add("ACT.MOTION", "ACT", "Motion / movement", "Движение / перемещение",
     "Physical movement", "Физическое перемещение",
     "ACT", 1, is_leaf=False)
_add("ACT.MOTION.GO", "ACT.MOTION", "Go / move forward", "Идти / двигаться вперёд",
     "To go, move, proceed", "Идти, двигаться, продвигаться", "ACT", 2,
     kw_en=["to go", "to move", "to proceed", "to walk", "to travel", "to advance", "to wander"])
_add("ACT.MOTION.COME", "ACT.MOTION", "Come / arrive", "Приходить / прибывать",
     "To come, arrive, approach, reach", "Приходить, прибывать, приближаться, достигать", "ACT", 2,
     kw_en=["to come", "to arrive", "to approach", "to reach", "arrival"])
_add("ACT.MOTION.RUN", "ACT.MOTION", "Run / flee", "Бежать / спасаться бегством",
     "To run, flee, rush", "Бежать, спасаться бегством, мчаться", "ACT", 2,
     kw_en=["to run", "to flee", "to rush", "to escape", "to hasten"])
_add("ACT.MOTION.FALL", "ACT.MOTION", "Fall / descend", "Падать / опускаться",
     "To fall, descend, sink", "Падать, опускаться, погружаться", "ACT", 2,
     kw_en=["to fall", "to descend", "to sink", "to drop", "fall (descent)"])
_add("ACT.MOTION.RISE", "ACT.MOTION", "Rise / ascend", "Подниматься / восходить",
     "To rise, ascend, climb", "Подниматься, восходить, взбираться", "ACT", 2,
     kw_en=["to rise", "to ascend", "to climb", "to mount", "to soar"])
_add("ACT.MOTION.FLY", "ACT.MOTION", "Fly", "Летать",
     "To fly", "Летать", "ACT", 2,
     kw_en=["to fly", "flight", "flying"])
_add("ACT.MOTION.SWIM", "ACT.MOTION", "Swim / float", "Плавать",
     "To swim, float", "Плавать, держаться на воде", "ACT", 2,
     kw_en=["to swim", "to float", "swimming"])
_add("ACT.MOTION.TURN", "ACT.MOTION", "Turn / rotate", "Поворачивать / вращать",
     "To turn, rotate, revolve", "Поворачивать, вращать, кружить", "ACT", 2,
     kw_en=["to turn", "to rotate", "to revolve", "to whirl", "to circle"])
_add("ACT.MOTION.STAND", "ACT.MOTION", "Stand / stay", "Стоять / оставаться",
     "To stand, remain, stay", "Стоять, оставаться, пребывать", "ACT", 2,
     kw_en=["to stand", "to remain", "to stay", "to abide", "to dwell"])
_add("ACT.MOTION.SIT", "ACT.MOTION", "Sit / lie down", "Сидеть / лежать",
     "To sit, lie down, recline", "Сидеть, лежать, возлежать", "ACT", 2,
     kw_en=["to sit", "to lie down", "to recline", "sitting"])

_add("ACT.CONTACT", "ACT", "Contact / manipulation", "Контакт / манипуляция",
     "Physical contact, holding, striking", "Физический контакт, удержание, удар",
     "ACT", 1, is_leaf=False)
_add("ACT.CONTACT.HOLD", "ACT.CONTACT", "Hold / grasp", "Держать / хватать",
     "To hold, grasp, seize", "Держать, хватать, схватывать", "ACT", 2,
     kw_en=["to hold", "to grasp", "to seize", "to grip", "to catch", "to clutch"])
_add("ACT.CONTACT.STRIKE", "ACT.CONTACT", "Strike / hit", "Ударять / бить",
     "To strike, hit, beat", "Ударять, бить, колотить", "ACT", 2,
     kw_en=["to strike", "to hit", "to beat", "to slay", "to kill", "to wound", "blow (hit)"])
_add("ACT.CONTACT.TOUCH", "ACT.CONTACT", "Touch", "Касаться",
     "To touch", "Касаться", "ACT", 2,
     kw_en=["to touch", "touching", "contact (touch)"])
_add("ACT.CONTACT.BIND", "ACT.CONTACT", "Bind / tie", "Связывать / привязывать",
     "To bind, tie, fasten", "Связывать, привязывать, закреплять", "ACT", 2,
     kw_en=["to bind", "to tie", "to fasten", "to fetter", "bond", "fetter"])
_add("ACT.CONTACT.BREAK", "ACT.CONTACT", "Break / cut", "Ломать / резать",
     "To break, cut, split, tear", "Ломать, резать, раскалывать, рвать", "ACT", 2,
     kw_en=["to break", "to cut", "to split", "to tear", "to pierce", "to cleave"])
_add("ACT.CONTACT.PRESS", "ACT.CONTACT", "Press / squeeze", "Давить / сжимать",
     "To press, squeeze, crush", "Давить, сжимать, давить", "ACT", 2,
     kw_en=["to press", "to squeeze", "to crush", "to compress"])
_add("ACT.CONTACT.RUB", "ACT.CONTACT", "Rub / wipe", "Тереть / вытирать",
     "To rub, wipe, polish", "Тереть, вытирать, полировать", "ACT", 2,
     kw_en=["to rub", "to wipe", "to polish", "to scrub"])

_add("ACT.POSSESSION", "ACT", "Possession / exchange", "Обладание / обмен",
     "Acquiring, giving, taking", "Приобретение, отдача, взятие",
     "ACT", 1, is_leaf=False)
_add("ACT.POSSESSION.GIVE", "ACT.POSSESSION", "Give", "Давать",
     "To give, bestow, grant", "Давать, дарить, жаловать", "ACT", 2,
     kw_en=["to give", "to bestow", "to grant", "to present", "to offer (give)", "gift"])
_add("ACT.POSSESSION.TAKE", "ACT.POSSESSION", "Take / receive", "Брать / получать",
     "To take, receive, accept", "Брать, получать, принимать", "ACT", 2,
     kw_en=["to take", "to receive", "to accept", "to obtain", "to acquire", "to seize (take)"])
_add("ACT.POSSESSION.HAVE", "ACT.POSSESSION", "Have / own", "Иметь / владеть",
     "To have, own, possess", "Иметь, владеть, обладать", "ACT", 2,
     kw_en=["to have", "to own", "to possess", "possession", "ownership"])
_add("ACT.POSSESSION.STEAL", "ACT.POSSESSION", "Steal / rob", "Красть / грабить",
     "To steal, rob, plunder", "Красть, грабить, разорять", "ACT", 2,
     kw_en=["to steal", "to rob", "to plunder", "theft", "robbery"])
_add("ACT.POSSESSION.LOSE", "ACT.POSSESSION", "Lose", "Терять",
     "To lose", "Терять", "ACT", 2,
     kw_en=["to lose", "loss", "deprived of"])

_add("ACT.CREATE", "ACT", "Creation / production", "Создание / производство",
     "Making, building, creating", "Создание, строительство, изготовление",
     "ACT", 1, is_leaf=False)
_add("ACT.CREATE.MAKE", "ACT.CREATE", "Make / do", "Делать / совершать",
     "To make, do, perform", "Делать, совершать, выполнять", "ACT", 2,
     kw_en=["to make", "to do", "to perform", "to act", "to accomplish", "action (deed)"])
_add("ACT.CREATE.BUILD", "ACT.CREATE", "Build / construct", "Строить / возводить",
     "To build, construct, erect", "Строить, возводить, сооружать", "ACT", 2,
     kw_en=["to build", "to construct", "to erect", "construction"])
_add("ACT.CREATE.GROW", "ACT.CREATE", "Grow / produce", "Расти / производить",
     "To grow, produce, generate", "Расти, производить, порождать", "ACT", 2,
     kw_en=["to grow", "to produce", "to generate", "to germinate", "growth"])
_add("ACT.CREATE.DESTROY", "ACT.CREATE", "Destroy", "Разрушать",
     "To destroy, ruin, demolish", "Разрушать, губить, сносить", "ACT", 2,
     kw_en=["to destroy", "to ruin", "to demolish", "destruction", "to annihilate"])

_add("ACT.CONSUME", "ACT", "Eat / drink / consume", "Есть / пить / потреблять",
     "Eating and drinking", "Еда и питьё",
     "ACT", 1, kw_en=["to eat", "to drink", "to consume", "to devour", "to feed", "eating", "food (verb context)"])

_add("ACT.COVER", "ACT", "Cover / fill / surround", "Покрывать / наполнять / окружать",
     "Covering, filling, enclosing", "Покрытие, наполнение, ограждение",
     "ACT", 1, kw_en=["to cover", "to fill", "to surround", "to envelop", "to enclose", "to wrap"])

_add("ACT.CONFLICT", "ACT", "Conflict / power", "Конфликт / власть",
     "Overcoming, conquering, opposing", "Преодоление, покорение, противостояние",
     "ACT", 1, kw_en=["to overpower", "to conquer", "to defeat", "to overcome", "to surpass",
                        "to humiliate", "to oppose", "to resist", "to subdue", "victory over",
                        "to vanquish", "to triumph"])

_add("ACT.PROTECT", "ACT", "Protect / guard", "Защищать / охранять",
     "Protection, guarding, defense", "Защита, охрана, оборона",
     "ACT", 1, kw_en=["to protect", "to guard", "to defend", "to save", "to shelter", "protection"])

_add("ACT.HELP", "ACT", "Help / serve", "Помогать / служить",
     "Helping, serving, assisting", "Помощь, служение, содействие",
     "ACT", 1, kw_en=["to help", "to serve", "to assist", "to aid", "service", "to support"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: ABSTR — абстрактные понятия
# ════════════════════════════════════════════════════════════════════════
_add("ABSTR", None, "Abstract Concepts", "Абстрактные понятия",
     "Non-concrete relational and conceptual entities", "Неконкретные относительные и понятийные сущности",
     "ABSTR", 0, is_leaf=False)

_add("ABSTR.QUANT", "ABSTR", "Quantity / measure", "Количество / мера",
     "Quantity, measure, amount", "Количество, мера, объём",
     "ABSTR", 1, kw_en=["quantity", "measure", "amount", "number", "multitude", "abundance",
                          "many", "few", "all", "whole", "part", "half"])

_add("ABSTR.SPACE", "ABSTR", "Space / direction", "Пространство / направление",
     "Spatial concepts and directions", "Пространственные понятия и направления",
     "ABSTR", 1, kw_en=["place", "direction", "side", "region", "north", "south", "east", "west",
                          "above", "below", "near", "far", "distance", "space (location)"])

_add("ABSTR.RELATION", "ABSTR", "Relation / connection", "Отношение / связь",
     "Abstract relations and connections", "Абстрактные отношения и связи",
     "ABSTR", 1, kw_en=["relation", "connection", "union", "separation", "distinction",
                          "identity", "difference", "similarity", "comparison"])

_add("ABSTR.CAUSE", "ABSTR", "Cause / reason", "Причина / основание",
     "Causation and reasoning", "Причинность и обоснование",
     "ABSTR", 1, kw_en=["cause", "reason", "purpose", "motive", "sake", "result", "effect", "consequence"])

_add("ABSTR.STATE", "ABSTR", "State / condition", "Состояние / условие",
     "General state or condition", "Общее состояние или условие",
     "ABSTR", 1, kw_en=["state", "condition", "situation", "circumstance", "status"])

_add("ABSTR.COLOR", "ABSTR", "Color", "Цвет",
     "Color terms", "Обозначения цвета",
     "ABSTR", 1, kw_en=["color", "colour", "white", "black", "red", "blue", "yellow", "green", "dark", "bright"])

_add("ABSTR.NUMBER", "ABSTR", "Number (numeral concept)", "Число (числовое понятие)",
     "Numerical concept", "Числовое понятие",
     "ABSTR", 1, kw_en=["one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                          "hundred", "thousand", "first", "second", "third"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: MAT — материальные объекты и артефакты
# ════════════════════════════════════════════════════════════════════════
_add("MAT", None, "Material Objects & Artifacts", "Материальные объекты и артефакты",
     "Man-made objects and structures", "Рукотворные объекты и сооружения",
     "MAT", 0, is_leaf=False)

_add("MAT.WEAPON", "MAT", "Weapon", "Оружие",
     "Weapon or military equipment", "Оружие или военное снаряжение",
     "MAT", 1, kw_en=["weapon", "sword", "bow", "arrow", "spear", "shield", "club", "mace",
                        "dagger", "axe", "armor", "armour"])

_add("MAT.VEHICLE", "MAT", "Vehicle / conveyance", "Транспортное средство",
     "Chariot, cart, boat, conveyance", "Колесница, повозка, лодка, средство передвижения",
     "MAT", 1, kw_en=["chariot", "cart", "wagon", "boat", "ship", "vehicle", "conveyance"])

_add("MAT.DWELLING", "MAT", "Dwelling / building", "Жилище / строение",
     "House, building, dwelling, settlement", "Дом, здание, жилище, поселение",
     "MAT", 1, kw_en=["house", "home", "dwelling", "building", "palace", "temple", "hut",
                        "city", "village", "town", "settlement"])

_add("MAT.CONTAINER", "MAT", "Container / vessel", "Сосуд / ёмкость",
     "Pot, vessel, container", "Горшок, сосуд, ёмкость",
     "MAT", 1, kw_en=["pot", "vessel", "vase", "container", "jar", "bowl", "basket", "bag"])

_add("MAT.CLOTHING", "MAT", "Clothing / adornment", "Одежда / украшение",
     "Clothing, garment, ornament worn", "Одежда, одеяние, носимое украшение",
     "MAT", 1, kw_en=["clothing", "garment", "robe", "cloth", "veil", "ornament (worn)",
                        "jewelry", "necklace", "bracelet", "crown", "garland"])

_add("MAT.TOOL", "MAT", "Tool / implement", "Инструмент / орудие",
     "Tool or implement", "Инструмент или орудие труда",
     "MAT", 1, kw_en=["tool", "implement", "instrument (tool)", "plough", "axe (tool)", "needle"])

_add("MAT.SEAT", "MAT", "Seat / furniture", "Сиденье / мебель",
     "Seat, throne, bed, furniture", "Сиденье, трон, ложе, мебель",
     "MAT", 1, kw_en=["seat", "throne", "bed", "couch", "chair", "furniture"])

_add("MAT.WRITING", "MAT", "Writing / document", "Письмо / документ",
     "Writing material or document", "Письменный материал или документ",
     "MAT", 1, kw_en=["letter (document)", "document", "writing", "book", "manuscript", "inscription"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: QUAL — качества и свойства
# ════════════════════════════════════════════════════════════════════════
_add("QUAL", None, "Qualities & Properties", "Качества и свойства",
     "Descriptive qualities and properties", "Описательные качества и свойства",
     "QUAL", 0, is_leaf=False)

_add("QUAL.SIZE", "QUAL", "Size / dimension", "Размер / измерение",
     "Size and dimensional qualities", "Размерные качества",
     "QUAL", 1, kw_en=["big", "large", "small", "great", "huge", "tiny", "tall", "short",
                         "long", "wide", "narrow", "thick", "thin", "deep", "high", "low"])

_add("QUAL.PHYSICAL", "QUAL", "Physical property", "Физическое свойство",
     "Physical/material qualities", "Физические/материальные свойства",
     "QUAL", 1, kw_en=["hard", "soft", "heavy", "light (weight)", "smooth", "rough",
                         "hot", "cold", "warm", "wet", "dry", "sharp", "blunt", "strong", "weak"])

_add("QUAL.MORAL", "QUAL", "Moral quality", "Нравственное качество",
     "Ethical/moral character", "Этический/нравственный характер",
     "QUAL", 1, kw_en=["good", "evil", "wicked", "virtuous", "righteous", "sinful", "noble",
                         "vile", "pure", "impure", "honest", "deceitful", "truthful", "false"])

_add("QUAL.BEAUTY", "QUAL", "Beauty / appearance", "Красота / внешность",
     "Aesthetic and appearance qualities", "Эстетические качества и внешний вид",
     "QUAL", 1, kw_en=["beautiful", "beauty", "handsome", "lovely", "ugly", "fair (beautiful)",
                         "splendid", "radiant", "shining", "bright (appearance)"])

_add("QUAL.QUANTITY_DEGREE", "QUAL", "Degree / intensity", "Степень / интенсивность",
     "Degree, intensity, completeness", "Степень, интенсивность, полнота",
     "QUAL", 1, kw_en=["complete", "whole (complete)", "perfect", "excessive", "extreme",
                         "moderate", "intense", "mild"])

_add("QUAL.SOCIAL", "QUAL", "Social quality", "Социальное качество",
     "Social status / reputation quality", "Качество социального статуса / репутации",
     "QUAL", 1, kw_en=["famous", "renowned", "glorious", "noble (rank)", "humble", "lowly",
                         "respected", "honored", "honoured", "despised"])

_add("SOC.RELIGION_OFFICE", "SOC.ROLE", "Religious/ascetic office", "Религиозная/аскетическая должность",
     "Religious office distinct from deity worship (monk, pilgrim)", "Религиозная должность, не относящаяся напрямую к культу божества",
     "SOC", 2, kw_en=["monk", "pilgrim", "devotee", "follower (religious)"])

_add("SOC.GUEST", "SOC", "Guest / hospitality", "Гость / гостеприимство",
     "Guest, host, hospitality", "Гость, хозяин, гостеприимство",
     "SOC", 1, kw_en=["guest", "host", "hospitality", "stranger"])

_add("SOC.ENEMY", "SOC", "Enemy / rival", "Враг / соперник",
     "Enemy, foe, rival, adversary", "Враг, недруг, соперник, противник",
     "SOC", 1, kw_en=["enemy", "foe", "rival", "adversary", "opponent"])

_add("SOC.FRIEND", "SOC", "Friend / companion", "Друг / спутник",
     "Friend, companion, ally", "Друг, спутник, союзник",
     "SOC", 1, kw_en=["friend", "companion", "ally", "comrade", "associate"])

# ── расширение MAT ──
_add("MAT.FOOD_PREP", "MAT", "Prepared food / dish", "Приготовленная еда / блюдо",
     "Prepared food item, dish, meal", "Приготовленное блюдо, кушанье",
     "MAT", 1, kw_en=["food", "meal", "dish (food)", "bread", "cake", "rice (cooked)", "porridge"])

_add("MAT.LIGHT_SOURCE", "MAT", "Light source", "Источник света",
     "Lamp, torch, light-producing object", "Лампа, факел, светильник",
     "MAT", 1, kw_en=["lamp", "torch", "light (object)", "candle"])

_add("MAT.MUSIC_INSTR", "MAT", "Musical instrument", "Музыкальный инструмент",
     "Physical musical instrument (object)", "Физический музыкальный инструмент (предмет)",
     "MAT", 1, kw_en=["drum (instrument)", "lute (instrument)", "flute (instrument)", "cymbal", "conch"])

_add("MAT.ROPE", "MAT", "Rope / cord / net", "Верёвка / шнур / сеть",
     "Rope, cord, net, snare (object)", "Верёвка, шнур, сеть, силок (предмет)",
     "MAT", 1, kw_en=["rope", "cord", "net (object)", "snare", "noose", "thread"])

_add("MAT.BOUNDARY", "MAT", "Boundary / wall / gate", "Граница / стена / ворота",
     "Wall, fence, gate, boundary marker", "Стена, ограда, ворота, межевой знак",
     "MAT", 1, kw_en=["wall", "fence", "gate", "boundary", "rampart", "fortress", "fort"])

# ── расширение QUAL ──
_add("QUAL.QUANTITY_ALL", "QUAL", "Totality / wholeness", "Целостность / полнота",
     "All, every, entire, whole", "Весь, каждый, целый",
     "QUAL", 1, kw_en=["all", "every", "entire", "whole (quality)", "total"])

_add("QUAL.NEWNESS", "QUAL", "New / old", "Новый / старый",
     "Newness or oldness", "Новизна или старость (давность)",
     "QUAL", 1, kw_en=["new", "old (age of object)", "ancient", "fresh", "young", "aged"])

_add("QUAL.SPEED", "QUAL", "Speed / pace", "Скорость / темп",
     "Fast, slow, speed quality", "Быстрый, медленный",
     "QUAL", 1, kw_en=["fast", "swift", "slow", "quick", "rapid", "speedy"])

_add("QUAL.QUIET", "QUAL", "Sound quality", "Звуковое качество",
     "Loud, quiet, silent qualities of sound", "Громкий, тихий, безмолвный",
     "QUAL", 1, kw_en=["loud", "quiet", "silent", "noisy", "sound (quality)"])

_add("QUAL.TASTE", "QUAL", "Taste / flavor", "Вкус",
     "Sweet, bitter, sour and other flavor qualities", "Сладкий, горький, кислый и иные вкусовые качества",
     "QUAL", 1, kw_en=["sweet", "bitter", "sour", "salty", "pungent", "taste", "flavor", "flavour"])

_add("QUAL.NUMBER_RANK", "QUAL", "Order / rank", "Порядок / ранг",
     "First, last, chief, principal", "Первый, последний, главный",
     "QUAL", 1, kw_en=["first", "last", "chief", "principal", "foremost", "highest (rank)", "supreme"])

# ── расширение ABSTR ──
_add("ABSTR.TRUTH", "ABSTR", "Truth / reality", "Истина / реальность",
     "Truth, reality, falsehood", "Истина, реальность, ложь",
     "ABSTR", 1, kw_en=["truth", "reality", "falsehood", "fact", "actual"])

_add("ABSTR.POSSIBILITY", "ABSTR", "Possibility / necessity", "Возможность / необходимость",
     "Modal concepts: possible, necessary, certain", "Модальные понятия: возможно, необходимо, точно",
     "ABSTR", 1, kw_en=["possible", "necessary", "certain", "doubtful", "probable"])

_add("ABSTR.BEGINNING_END", "ABSTR", "Beginning / end", "Начало / конец",
     "Beginning, origin, end, completion", "Начало, происхождение, конец, завершение",
     "ABSTR", 1, kw_en=["beginning", "origin", "end (conclusion)", "completion", "start", "finish"])

_add("ABSTR.NAME_LABEL", "ABSTR", "Name / label / sign", "Имя / обозначение / знак",
     "Names, signs, labels, marks (abstract)", "Имена, знаки, обозначения, метки",
     "ABSTR", 1, kw_en=["sign", "mark", "symbol", "token", "indication"])

# ════════════════════════════════════════════════════════════════════════
#  ДОМЕН: SCI — наука и техническое знание
# ════════════════════════════════════════════════════════════════════════
_add("SCI", None, "Science & Technical Knowledge", "Наука и техническое знание",
     "Sciences (grammar, medicine, mathematics, astrology)", "Науки (грамматика, медицина, математика, астрология)",
     "SCI", 0, is_leaf=False)

_add("SCI.MEDICINE", "SCI", "Medicine / healing", "Медицина / врачевание",
     "Medical and healing concepts", "Медицинские понятия и врачевание",
     "SCI", 1, kw_en=["medicine", "healer", "physician", "remedy", "cure", "poison", "antidote",
                        "drug", "disease (medical)", "treatment"])

_add("SCI.MATH", "SCI", "Mathematics / calculation", "Математика / вычисление",
     "Mathematical and computational concepts", "Математические и вычислительные понятия",
     "SCI", 1, kw_en=["calculation", "arithmetic", "counting", "measurement (math)", "geometry"])

_add("SCI.ASTROLOGY", "SCI", "Astrology", "Астрология",
     "Astrological concepts", "Астрологические понятия",
     "SCI", 1, kw_en=["astrology", "horoscope", "zodiac", "auspicious", "inauspicious", "omen"])

_add("SCI.GRAMMAR_SCIENCE", "SCI", "Grammar (as a science)", "Грамматика (как наука)",
     "Sanskrit grammatical science (vyākaraṇa) terminology", "Терминология науки санскритской грамматики (вьякарана)",
     "SCI", 1, kw_en=["grammar (science)", "grammatical rule", "declension", "conjugation",
                        "syntax", "phonetics", "etymology"])

_add("SCI.LOGIC", "SCI", "Logic / epistemology", "Логика / гносеология",
     "Logical and epistemological terms (nyāya)", "Логические и гносеологические термины (ньяя)",
     "SCI", 1, kw_en=["logic", "inference", "syllogism", "proof", "argument", "premise", "epistemology"])

_add("UNCATEGORIZED", None, "Uncategorized", "Без категории",
     "Entry could not be categorized by available methods",
     "Запись не удалось категоризировать доступными методами",
     "MISC", 0)

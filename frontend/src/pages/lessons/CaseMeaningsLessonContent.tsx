import { useTranslation } from 'react-i18next';

interface Example {
  text: string;
  transliteration: string;
  translation: string;
}

interface CaseSection {
  id: string;
  title: string;
  mainFunction: string;
  secondaryFunctions: string[];
  examples: Example[];
}

const sections: CaseSection[] = [
  {
    id: 'nominative',
    title: 'Именительный падеж (prathamā vibhakti / kartā)',
    mainFunction: 'маркировка грамматического субъекта (подлежащего) при глаголе в активном залоге.',
    secondaryFunctions: [
      'Именное сказуемое — идентификация или классификация субъекта (при глаголах-связках).',
      'Сравнительное уподобление — в конструкциях с частицей iva (подобно, как).',
    ],
    examples: [
      { text: 'रामः फलं खादति।', transliteration: 'Rāmaḥ phalaṃ khādati.', translation: 'Рама ест плод.' },
      { text: 'अयं राजा।', transliteration: 'Ayaṃ rājā.', translation: 'Это царь.' },
      { text: 'राजा इव भाति।', transliteration: 'Rājā iva bhāti.', translation: 'Сияет, как царь.' },
    ],
  },
  {
    id: 'accusative',
    title: 'Винительный падеж (dvitīyā vibhakti / karman)',
    mainFunction: 'маркировка прямого объекта действия (пациенса).',
    secondaryFunctions: [
      'Цель движения — обозначение конечной точки перемещения.',
      'Временной отрезок — указание на длительность действия.',
      'Преодолеваемое пространство — мера расстояния.',
    ],
    examples: [
      { text: 'रामः फलम् खादति।', transliteration: 'Rāmaḥ phalaṃ khādati.', translation: 'Рама ест плод.' },
      { text: 'रामः वनं गच्छति।', transliteration: 'Rāmaḥ vanaṃ gacchati.', translation: 'Рама идёт в лес.' },
      { text: 'सः वर्षम् अवसत्।', transliteration: 'Saḥ varṣam avasat.', translation: 'Он жил год.' },
      { text: 'सः मार्गम् अगच्छत्।', transliteration: 'Saḥ mārgam agacchat.', translation: 'Он прошёл путь.' },
    ],
  },
  {
    id: 'instrumental',
    title: 'Творительный падеж (tṛtīyā vibhakti / karaṇa)',
    mainFunction: 'маркировка инструмента или средства совершения действия.',
    secondaryFunctions: [
      'Совместность (соучастник) — указание на лицо, сопровождающее субъект.',
      'Образ действия — характеристика способа протекания действия.',
      'Внешняя причина — указание на фактор-посредник.',
      'Сравнение — уподобление без сравнительной частицы.',
      'Цена / мера обмена — указание на эквивалент стоимости.',
      'Агент в пассивной конструкции — субъект действия при страдательном залоге.',
    ],
    examples: [
      { text: 'रामः लेखन्या लिखति।', transliteration: "Rāmaḥ lekhan'yā likhati.", translation: 'Рама пишет пером.' },
      { text: 'रामः सीतया गच्छति।', transliteration: 'Rāmaḥ Sītayā gacchati.', translation: 'Рама идёт с Ситой.' },
      { text: 'सः शनैः गच्छति।', transliteration: 'Saḥ śanaiḥ gacchati.', translation: 'Он идёт медленно.' },
      { text: 'सः भूखेन मृतः।', transliteration: 'Saḥ bhukhena mṛtaḥ.', translation: 'Он умер от голода.' },
      { text: 'मुखं चन्द्रेण भाति।', transliteration: 'Mukhaṃ candreṇa bhāti.', translation: 'Лицо сияет подобно луне.' },
      { text: 'शतेन क्रीतम्।', transliteration: 'Śatena krītam.', translation: 'Куплено за сотню.' },
      { text: 'रामेण कृतम्।', transliteration: 'Rāmeṇa kṛtam.', translation: 'Сделано Рамой.' },
    ],
  },
  {
    id: 'dative',
    title: 'Дательный падеж (caturthī vibhakti / sampradāna)',
    mainFunction: 'маркировка адресата (получателя) при глаголах передачи.',
    secondaryFunctions: [
      'Цель / предназначение — указание на финальную причину или назначение.',
      'Объект эмоционального отношения (почтение, гнев, радость).',
      'Направление (архаичное употребление, характерное для ведийского санскрита).',
    ],
    examples: [
      { text: 'गुरुः शिष्याय पुस्तकं यच्छति।', transliteration: 'Guruḥ śiṣyāya pustakaṃ yacchati.', translation: 'Учитель даёт книгу ученику.' },
      { text: 'सः विजयाय यजति।', transliteration: 'Saḥ vijayāya yajati.', translation: 'Он жертвует ради победы.' },
      { text: 'सः देवाय नमति।', transliteration: 'Saḥ devāya namati.', translation: 'Он кланяется Богу.' },
      { text: 'सः गृहाय गच्छति। (архаика)', transliteration: 'Saḥ gṛhāya gacchati.', translation: 'Он идёт к дому.' },
    ],
  },
  {
    id: 'ablative',
    title: 'Аблатив (pañcamī vibhakti / apadāna)',
    mainFunction: 'маркировка исходной точки движения или отделения.',
    secondaryFunctions: [
      'Лишение, удаление — отделение от обладания или состояния.',
      'Внутренняя причина — причина, понимаемая как источник.',
      'Объект сравнения (при сравнительной степени).',
      'Указание на расстояние от точки отсчёта.',
      'Выделение из множества (при превосходной степени).',
    ],
    examples: [
      { text: 'पक्षी वृक्षात् पतति।', transliteration: 'Pakṣī vṛkṣāt patati.', translation: 'Птица падает с дерева.' },
      { text: 'शत्रुः राज्यात् हीनः।', transliteration: 'Śatruḥ rājyāt hīnaḥ.', translation: 'Враг лишён царства.' },
      { text: 'वृक्षः गृहात् दूरे।', transliteration: 'Vṛkṣaḥ gṛhāt dūre.', translation: 'Дерево далеко от дома.' },
      { text: 'सः सर्वेभ्यः श्रेष्ठः।', transliteration: 'Saḥ sarvebhyaḥ śreṣṭhaḥ.', translation: 'Он лучший из всех.' },
    ],
  },
  {
    id: 'genitive',
    title: 'Родительный падеж (ṣaṣṭhī vibhakti / sambandha)',
    mainFunction: 'маркировка принадлежности (посессивность).',
    secondaryFunctions: [
      'Партитив — указание на часть от целого.',
      'Объект при глаголах желания, памяти, страха.',
      'Агент в пассивных конструкциях.',
      'Объект при отрицании (генитив отрицания).',
      'Содержимое / материал (в конструкциях наполнения).',
    ],
    examples: [
      { text: 'इदं रथस्य चक्रम्।', transliteration: 'Idaṃ rathasya cakram.', translation: 'Это колесо колесницы.' },
      { text: 'एकः मनुष्याणाम्।', transliteration: 'Ekaḥ manuṣyāṇām.', translation: 'Один из людей.' },
      { text: 'सः यशसः काङ्क्षति।', transliteration: 'Saḥ yaśasaḥ kāṅkṣati.', translation: 'Он желает славы.' },
      { text: 'गुरोः उक्तम्।', transliteration: 'Guroḥ uktam.', translation: 'Сказано учителем.' },
      { text: 'जलस्य न अस्ति।', transliteration: 'Jalasya na asti.', translation: 'Нет воды.' },
    ],
  },
  {
    id: 'locative',
    title: 'Местный падеж (saptamī vibhakti / adhikaraṇa)',
    mainFunction: 'маркировка места нахождения (локатив).',
    secondaryFunctions: [
      'Время действия (темпоральный локатив).',
      'Тема речи или мысли (объект обсуждения).',
      'Сфера знаний / область компетенции.',
      'Причина или повод (каузальный локатив).',
      'Опора, основание (метафорический локатив).',
    ],
    examples: [
      { text: 'चक्रं रथे अस्ति।', transliteration: 'Cakraṃ rathe asti.', translation: 'Колесо находится в колеснице.' },
      { text: 'शनिवासरे बभूव।', transliteration: 'Śanivāsare babhūva.', translation: 'Случилось в субботу.' },
      { text: 'वयं धर्मे वदामः।', transliteration: 'Vayaṃ dharme vadāmaḥ.', translation: 'Мы говорим о дхарме.' },
      { text: 'सः वेदेषु निपुणः।', transliteration: 'Saḥ vedeṣu nipuṇaḥ.', translation: 'Он сведущ в Ведах.' },
      { text: 'सत्ये जगत् स्थितम्।', transliteration: 'Satye jagat sthitam.', translation: 'Мир стоит на истине.' },
    ],
  },
  {
    id: 'vocative',
    title: 'Звательный падеж (sambodhana)',
    mainFunction: 'прямое обращение (апеллятив). Не участвует в синтаксической структуре предложения, всегда обособлен.',
    secondaryFunctions: [],
    examples: [
      { text: 'हे राम! इह आगच्छ।', transliteration: 'He Rāma! iha āgaccha.', translation: 'О, Рама! Иди сюда.' },
    ],
  },
];

const CaseMeaningsLessonContent = () => {
  const { i18n } = useTranslation();
  const isRu = i18n.language === 'ru';

  return (
    <div className="case-meanings-content">
      {/* Table of contents */}
      <div className="mb-3 flex flex-wrap gap-2">
        {sections.map((s) => (
          <a
            key={s.id}
            href={`#${s.id}`}
            className="text-sm text-primary no-underline hover:underline"
            onClick={(e) => {
              e.preventDefault();
              document.getElementById(s.id)?.scrollIntoView({ behavior: 'smooth' });
            }}
          >
{s.title.replace(/\(.*\)$/, '').trim()}
          </a>
        ))}
      </div>

      {/* Sections */}
      {sections.map((s) => (
        <div key={s.id} id={s.id} className="mb-3">
          <h3 className="text-base font-medium mb-3">{s.title.replace(/\(.*\)$/, '').trim()}</h3>

          <p className="text-sm mb-2">{s.mainFunction}</p>

          {s.secondaryFunctions.length > 0 && (
            <div className="mb-2">
              <ul className="text-sm m-0 pl-3" style={{ listStyle: 'disc' }}>
                {s.secondaryFunctions.map((fn, i) => (
                  <li key={i}>{fn}</li>
                ))}
              </ul>
            </div>
          )}

          {s.examples.map((ex, i) => (
            <div key={i} className="flex flex-column mb-2">
              <div className="text-base" style={{ fontFamily: 'serif' }}>{ex.text}</div>
              <div className="text-sm text-color-secondary">{ex.transliteration}</div>
              <div className="text-sm">{ex.translation}</div>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
};

export default CaseMeaningsLessonContent;
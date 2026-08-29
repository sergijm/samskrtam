import { useTranslation } from 'react-i18next';
import { MiniProgressBar } from '../../components/common/MiniProgressBar';
import type { CaseAggregation } from '../../types/lesson';

interface Example {
  text: string;
  transliteration: string;
  translation: string;
  question: string;
}

interface CaseSection {
  id: string;
  title: string;
  questions: string;
  mainFunction: string;
  secondaryFunctions: string[];
  examples: Example[];
}

interface Differentiation {
  pair: string;
  questions: string;
  criterion: string;
  rule: string;
}

interface CaseMeaningsLessonContentProps {
  caseAggregations?: CaseAggregation[];
  onStartQuiz: (caseType: string) => void;
}

const SECTION_TO_CASE: Record<string, string> = {
  nominative: 'NOMINATIVE',
  accusative: 'ACCUSATIVE',
  instrumental: 'INSTRUMENTAL',
  dative: 'DATIVE',
  ablative: 'ABLATIVE',
  genitive: 'GENITIVE',
  locative: 'LOCATIVE',
  vocative: 'VOCATIVE',
};

const sections: CaseSection[] = [
  {
    id: 'nominative',
    title: 'Именительный падеж (prathamā vibhakti / kartā)',
    questions: 'кто? что?',
    mainFunction: 'маркировка грамматического субъекта (подлежащего) при глаголе в активном залоге (кто? что делает?).',
    secondaryFunctions: [
      'Именное сказуемое — идентификация или классификация субъекта при глаголах-связках (кто/что есть субъект?).',
      'Сравнительное уподобление — в конструкциях с частицей iva (кто/что подобно?).',
    ],
    examples: [
      { text: 'रामः फलं खादति।', transliteration: 'Rāmaḥ phalaṃ khādati.', translation: 'Рама ест плод.', question: 'Кто ест?' },
      { text: 'अयं राजा।', transliteration: 'Ayaṃ rājā.', translation: 'Это царь.', question: 'Кто это?' },
      { text: 'राजा इव भाति।', transliteration: 'Rājā iva bhāti.', translation: 'Сияет, как царь.', question: 'Кто подобно?' },
    ],
  },
  {
    id: 'accusative',
    title: 'Винительный падеж (dvitīyā vibhakti / karman)',
    questions: 'кого? что? куда? как долго?',
    mainFunction: 'маркировка прямого объекта действия / пациенса (кого? что?).',
    secondaryFunctions: [
      'Цель движения — обозначение конечной точки перемещения (куда?).',
      'Временной отрезок — указание на длительность действия (как долго? сколько времени?).',
      'Преодолеваемое пространство — мера расстояния (какое расстояние?).',
    ],
    examples: [
      { text: 'रामः फलम् खादति।', transliteration: 'Rāmaḥ phalaṃ khādati.', translation: 'Рама ест плод.', question: 'Ест что?' },
      { text: 'रामः वनं गच्छति।', transliteration: 'Rāmaḥ vanaṃ gacchati.', translation: 'Рама идёт в лес.', question: 'Идёт куда?' },
      { text: 'सः वर्षम् अवसत्।', transliteration: 'Saḥ varṣam avasat.', translation: 'Он жил год.', question: 'Жил как долго?' },
      { text: 'सः मार्गम् अगच्छत्।', transliteration: 'Saḥ mārgam agacchat.', translation: 'Он прошёл путь.', question: 'Прошёл какое расстояние?' },
    ],
  },
  {
    id: 'instrumental',
    title: 'Творительный падеж (tṛtīyā vibhakti / karaṇa)',
    questions: 'кем? чем? с кем? как? по какой причине? за сколько?',
    mainFunction: 'маркировка инструмента или средства совершения действия (чем? с помощью чего?).',
    secondaryFunctions: [
      'Совместность (соучастник) — указание на лицо, сопровождающее субъект (с кем? вместе с кем?).',
      'Образ действия — характеристика способа протекания действия (как? каким образом?).',
      'Внешняя причина — указание на фактор-посредник (от чего? из-за чего?).',
      'Сравнение — уподобление без сравнительной частицы (подобно кому/чему?).',
      'Цена / мера обмена — указание на эквивалент стоимости (за сколько? какой ценой?).',
      'Агент в пассивной конструкции — субъект действия при страдательном залоге (кем? чем?).',
    ],
    examples: [
      { text: 'रामः लेखन्या लिखति।', transliteration: "Rāmaḥ lekhan'yā likhati.", translation: 'Рама пишет пером.', question: 'Пишет чем?' },
      { text: 'रामः सीतया गच्छति।', transliteration: 'Rāmaḥ Sītayā gacchati.', translation: 'Рама идёт с Ситой.', question: 'Идёт с кем?' },
      { text: 'सः शनैः गच्छति।', transliteration: 'Saḥ śanaiḥ gacchati.', translation: 'Он идёт медленно.', question: 'Идёт как?' },
      { text: 'सः भूखेन मृतः।', transliteration: 'Saḥ bhukhena mṛtaḥ.', translation: 'Он умер от голода.', question: 'Умер из-за чего?' },
      { text: 'मुखं चन्द्रेण भाति।', transliteration: 'Mukhaṃ candreṇa bhāti.', translation: 'Лицо сияет подобно луне.', question: 'Сияет чем / подобно чему?' },
      { text: 'शतेन क्रीतम्।', transliteration: 'Śatena krītam.', translation: 'Куплено за сотню.', question: 'Куплено за сколько?' },
      { text: 'रामेण कृतम्।', transliteration: 'Rāmeṇa kṛtam.', translation: 'Сделано Рамой.', question: 'Сделано кем?' },
    ],
  },
  {
    id: 'dative',
    title: 'Дательный падеж (caturthī vibhakti / sampradāna)',
    questions: 'кому? чему? для чего? ради чего?',
    mainFunction: 'маркировка адресата (получателя) при глаголах передачи (кому? чему?).',
    secondaryFunctions: [
      'Цель / предназначение — указание на финальную причину или назначение (зачем? ради чего? для чего?).',
      'Объект эмоционального отношения — выражение почтения, гнева, радости (кому/чему выражается отношение?).',
      'Направление (архаичное употребление в ведийском санскрите) — цель движения (к чему? куда?).',
    ],
    examples: [
      { text: 'गुरुः शिष्याय पुस्तकं यच्छति।', transliteration: 'Guruḥ śiṣyāya pustakaṃ yacchati.', translation: 'Учитель даёт книгу ученику.', question: 'Даёт кому?' },
      { text: 'सः विजयाय यजति।', transliteration: 'Saḥ vijayāya yajati.', translation: 'Он жертвует ради победы.', question: 'Жертвует ради чего?' },
      { text: 'सः देवाय नमति।', transliteration: 'Saḥ devāya namati.', translation: 'Он кланяется Богу.', question: 'Кланяется кому?' },
      { text: 'सः गृहाय गच्छति। (архаика)', transliteration: 'Saḥ gṛhāya gacchati.', translation: 'Он идёт к дому.', question: 'Идёт к чему / куда?' },
    ],
  },
  {
    id: 'ablative',
    title: 'Аблатив / Отложительный падеж (pañcamī vibhakti / apadāna)',
    questions: 'откуда? от кого? из-за чего? чем кто/что (при сравнении)?',
    mainFunction: 'маркировка исходной точки движения или отделения (откуда? от кого? из чего?).',
    secondaryFunctions: [
      'Лишение, удаление — отделение от обладания или состояния (чего лишился? от чего удалился?).',
      'Внутренняя причина — причина как первоисточник, внутренний импульс (почему? от чего?).',
      'Объект сравнения — при сравнительной степени (чем кто/что? относительно кого?).',
      'Указание на расстояние — отсчёт от ориентира (от чего далеко/близко?).',
      'Выделение из множества — при превосходной степени (из кого? из чего?).',
    ],
    examples: [
      { text: 'पक्षी वृक्षात् पतति।', transliteration: 'Pakṣī vṛkṣāt patati.', translation: 'Птица падает с дерева.', question: 'Падает откуда?' },
      { text: 'शत्रुः राज्यात् हीनः।', transliteration: 'Śatruḥ rājyāt hīnaḥ.', translation: 'Враг лишён царства.', question: 'Лишён чего?' },
      { text: 'वृक्षः गृहात् दूरे।', transliteration: 'Vṛkṣaḥ gṛhāt dūre.', translation: 'Дерево далеко от дома.', question: 'Далеко от чего?' },
      { text: 'सः सर्वेभ्यः श्रेष्ठः।', transliteration: 'Saḥ sarvebhyaḥ śreṣṭhaḥ.', translation: 'Он лучший из всех.', question: 'Лучший из кого?' },
    ],
  },
  {
    id: 'genitive',
    title: 'Родительный падеж (ṣaṣṭhī vibhakti / sambandha)',
    questions: 'кого? чего? чей? чья? чьё? из кого/чего (часть)?',
    mainFunction: 'маркировка принадлежности / посессивность (чей? кого? чего?).',
    secondaryFunctions: [
      'Партитив — указание на часть от целого (из кого? из чего?).',
      'Объект при глаголах желания, памяти, страха — маркирует частичный или абстрактный объект (чего именно желает/боится?).',
      'Агент в пассивных конструкциях — в позднейшей грамматической традиции (кем именно сказано/сделано?).',
      'Объект при отрицании — генитив отрицания (кого/чего нет?).',
      'Содержимое / материал — в конструкциях наполнения (чем/чего полн?).',
    ],
    examples: [
      { text: 'इदं रथस्य चक्रम्।', transliteration: 'Idaṃ rathasya cakram.', translation: 'Это колесо колесницы.', question: 'Колесо чьё / чего?' },
      { text: 'एकः मनुष्याणाम्।', transliteration: 'Ekaḥ manuṣyāṇām.', translation: 'Один из людей.', question: 'Один из кого?' },
      { text: 'सः यशसः काङ्क्षति।', transliteration: 'Saḥ yaśasaḥ kāṅkṣati.', translation: 'Он желает славы.', question: 'Желает чего?' },
      { text: 'गुरोः उक्तम्।', transliteration: 'Guroḥ uktam.', translation: 'Сказано учителем.', question: 'Сказано кем? (чьё слово)' },
      { text: 'जलस्य न अस्ति।', transliteration: 'Jalasya na asti.', translation: 'Нет воды.', question: 'Нет чего?' },
    ],
  },
  {
    id: 'locative',
    title: 'Местный падеж / Локатив (saptamī vibhakti / adhikaraṇa)',
    questions: 'где? в ком? в чём? когда? о ком? о чём?',
    mainFunction: 'маркировка места нахождения (где? в чём? на чём?).',
    secondaryFunctions: [
      'Время действия (темпоральный локатив) — указание на временной контекст (когда? во сколько?).',
      'Тема речи или мысли — объект обсуждения (о ком? о чём?).',
      'Сфера знаний / область компетенции — в чем проявляется мастерство (в чём? в какой области?).',
      'Причина или повод (каузальный локатив) — жизненная ситуация или условие (при каких обстоятельствах?).',
      'Опора, основание (метафорический локатив) — то, на чём держится ситуация (на чём?).',
    ],
    examples: [
      { text: 'चक्रं रथे अस्ति।', transliteration: 'Cakraṃ rathe asti.', translation: 'Колесо находится в колеснице.', question: 'Находится где?' },
      { text: 'शनिवासरे बभूव।', transliteration: 'Śanivāsare babhūva.', translation: 'Случилось в субботу.', question: 'Случилось когда?' },
      { text: 'वयं धर्मे वदामः।', transliteration: 'Vayaṃ dharme vadāmaḥ.', translation: 'Мы говорим о дхарме.', question: 'Говорим о чём?' },
      { text: 'सः वेदेषु निपुणः।', transliteration: 'Saḥ vedeṣu nipuṇaḥ.', translation: 'Он сведущ в Ведах.', question: 'Сведущ в чём?' },
      { text: 'सत्ये जगत् स्थितम्।', transliteration: 'Satye jagat sthitam.', translation: 'Мир стоит на истине.', question: 'Стоит на чём?' },
    ],
  },
  {
    id: 'vocative',
    title: 'Звательный падеж (sambodhana)',
    questions: 'обращение (о кто! о что!)',
    mainFunction: 'прямое обращение (апеллятив). Не участвует в синтаксической структуре предложения как член предложения, всегда обособлен.',
    secondaryFunctions: [],
    examples: [
      { text: 'हे राम! इह आगच्छ।', transliteration: 'He Rāma! iha āgaccha.', translation: 'О, Рама! Иди сюда.', question: 'К кому обращаются?' },
    ],
  },
];

const differentiations: Differentiation[] = [
  {
    pair: 'Аблатив vs. Творительный (причина)',
    questions: 'От чего? (источник) vs. Чем / из-за чего? (посредник)',
    criterion: 'Характер причины',
    rule: 'Аблатив: внутренняя / естественная причина (родился от матери). Творительный: внешний фактор-посредник (умер от голода).',
  },
  {
    pair: 'Винительный vs. Местный (пространство)',
    questions: 'Куда? (цель) vs. Где? (локация)',
    criterion: 'Наличие динамики',
    rule: 'Винительный: направление движения (куда?). Местный: статичное положение (где?).',
  },
  {
    pair: 'Родительный vs. Дательный (объект)',
    questions: 'Чей / чего? vs. Кому / для чего?',
    criterion: 'Характер связи',
    rule: 'Родительный: принадлежность или частичный охват. Дательный: направленность, адресат или финальная цель.',
  },
  {
    pair: 'Именительный vs. Звательный',
    questions: 'Кто? (субъект) vs. О кто! (обращение)',
    criterion: 'Роль в предложении',
    rule: 'Именительный: член предложения (подлежащее/субъект). Звательный: обособленное обращение, не являющееся членом предложения.',
  },
];

const aggByCaseType = (list: CaseAggregation[] | undefined) => {
  const map = new Map<string, CaseAggregation>();
  if (list) list.forEach((a) => map.set(a.caseType, a));
  return map;
};

const CaseMeaningsLessonContent = ({ caseAggregations, onStartQuiz }: CaseMeaningsLessonContentProps) => {
  const { i18n } = useTranslation();
  const byCase = aggByCaseType(caseAggregations);

  return (
    <div className="case-meanings-content">
      <div className="flex flex-wrap gap-2 mb-3">
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
            {i18n.language === 'ru'
              ? s.title.replace(/\(.*\)$/, '').trim()
              : s.title.replace(/^[^(]+/, '').replace(/[()]/g, '').trim()}
          </a>
        ))}
        <a
          href="#differentiation"
          className="text-sm text-primary no-underline hover:underline"
          onClick={(e) => {
            e.preventDefault();
            document.getElementById('differentiation')?.scrollIntoView({ behavior: 'smooth' });
          }}
        >
          {i18n.language === 'ru' ? 'Различия' : 'Differences'}
        </a>
      </div>

      {sections.map((s) => {
        const agg = byCase.get(SECTION_TO_CASE[s.id]);

        return (
          <div key={s.id} id={s.id} className="mb-3">
            <div className="flex align-items-center justify-content-between mb-1">
              <div>
                <h3 className="text-base font-medium mb-1">{s.title}</h3>
                <div className="text-sm text-color-secondary">
                  <em>Вопросы: {s.questions}</em>
                </div>
              </div>
              <div className="flex align-items-center gap-2" style={{ flexShrink: 0 }}>
                {agg && (
                  <MiniProgressBar
                    value={agg.aggregatedProgress}
                    status={agg.status}
                    width="80px"
                    height="6px"
                  />
                )}
                <i
                  className="pi pi-angle-double-right text-xl cursor-pointer"
                  style={{ color: '#f97316' }}
                  onClick={() => onStartQuiz(SECTION_TO_CASE[s.id])}
                />
              </div>
            </div>

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

            <div className="overflow-x-auto">
              <table className="text-sm w-full" style={{ borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #ddd' }}>
                    <th style={{ padding: '4px 8px', textAlign: 'left' }}>Пример</th>
                    <th style={{ padding: '4px 8px', textAlign: 'left' }}>Транслитерация</th>
                    <th style={{ padding: '4px 8px', textAlign: 'left' }}>Перевод</th>
                    <th style={{ padding: '4px 8px', textAlign: 'left' }}>Вопрос</th>
                  </tr>
                </thead>
                <tbody>
                  {s.examples.map((ex, i) => (
                    <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                      <td style={{ padding: '4px 8px' }}>
                        <span className="text-base" style={{ fontFamily: 'serif' }}>{ex.text}</span>
                      </td>
                      <td style={{ padding: '4px 8px' }}>
                        <span className="text-color-secondary" style={{ fontStyle: 'italic' }}>{ex.transliteration}</span>
                      </td>
                      <td style={{ padding: '4px 8px' }}>{ex.translation}</td>
                      <td style={{ padding: '4px 8px' }}>
                        <span className="text-color-secondary">{ex.question}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        );
      })}

      <div id="differentiation" className="mb-3">
        <h3 className="text-base font-medium mb-2">
          {i18n.language === 'ru' ? 'Ключевые дифференциации' : 'Key Differentiations'}
        </h3>
        <div className="overflow-x-auto">
          <table className="text-sm w-full" style={{ borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #ddd' }}>
                <th style={{ padding: '4px 8px', textAlign: 'left' }}>{i18n.language === 'ru' ? 'Сравниваемая пара' : 'Pair'}</th>
                <th style={{ padding: '4px 8px', textAlign: 'left' }}>{i18n.language === 'ru' ? 'Вопросы для проверки' : 'Check questions'}</th>
                <th style={{ padding: '4px 8px', textAlign: 'left' }}>{i18n.language === 'ru' ? 'Критерий' : 'Criterion'}</th>
                <th style={{ padding: '4px 8px', textAlign: 'left' }}>{i18n.language === 'ru' ? 'Правило' : 'Rule'}</th>
              </tr>
            </thead>
            <tbody>
              {differentiations.map((d, i) => (
                <tr key={i} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: '4px 8px', fontWeight: 500 }}>{d.pair}</td>
                  <td style={{ padding: '4px 8px' }}>{d.questions}</td>
                  <td style={{ padding: '4px 8px' }}>{d.criterion}</td>
                  <td style={{ padding: '4px 8px' }}>{d.rule}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default CaseMeaningsLessonContent;
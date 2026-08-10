import { useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useSandhiRules } from '../../hooks/useLessons';
import { Skeleton } from 'primereact/skeleton';

const SANDHI_TITLES: Record<string, { ru: string; en: string }> = {
  'sandhi-vowels-external': {
    ru: 'Внешние сандхи: гласные',
    en: 'External sandhi: vowels',
  },
  'sandhi-consonants': {
    ru: 'Внешние сандхи: согласные',
    en: 'External sandhi: consonants',
  },
  'sandhi-visarga': {
    ru: 'Внешние сандхи: висарга',
    en: 'External sandhi: visarga',
  },
  'sandhi-vowels-internal': {
    ru: 'Внутренние сандхи: гласные',
    en: 'Internal sandhi: vowels',
  },
  'sandhi-consonants-internal': {
    ru: 'Внутренние сандхи: согласные',
    en: 'Internal sandhi: consonants',
  },
};

const SandhiLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const { t, i18n } = useTranslation();
  const { data: response, isLoading, isError } = useSandhiRules(slug || '');

  const isRu = i18n.language === 'ru';
  const topicTitle = SANDHI_TITLES[slug || ''];

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t('lesson.loadError')}</div>
      </div>
    );
  }

  return (
    <div className="p-4">
      {isLoading || !response ? (
        <div className="p-4">
          <Skeleton width="100%" height="40px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-4" />
          <Skeleton width="100%" height="200px" />
        </div>
      ) : (
        <>
          <div className="card mb-3">
            <h2 className="m-0">
              {topicTitle ? (isRu ? topicTitle.ru : topicTitle.en) : response.title}
            </h2>
          </div>

          {response.rules.length === 0 ? (
            <div className="card">
              <p className="text-color-secondary m-0">
                {isRu ? 'Нет правил для этой темы.' : 'No rules for this topic.'}
              </p>
            </div>
          ) : (
            <div className="flex flex-column gap-3">
              {response.rules.map((rule) => (
                <div key={rule.number} className="card p-0">
                  <div className="surface-ground p-3 border-bottom-1 border-200">
                    <div className="flex align-items-center gap-2">
                      <span className="bg-primary text-white border-circle w-2rem h-2rem flex align-items-center justify-content-center font-bold">
                        {rule.number}
                      </span>
                      <span className="text-sm text-color-secondary">
                        {rule.reference}
                      </span>
                    </div>
                  </div>
                  <div className="p-3">
                    <p className="m-0 mb-2 text-lg">{rule.text}</p>
                    {rule.example && (
                      <div className="surface-50 p-3 border-round text-base">
                        <span className="font-mono">{rule.example}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default SandhiLessonPage;
import { useTranslation } from 'react-i18next';
import { DataView } from 'primereact/dataview';
import { Button } from 'primereact/button';
import { useNavigate } from 'react-router-dom';

interface VocabularySection {
  title: string;
  description: string;
  link: string;
  icon: string;
}

const VocabularyPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const vocabularySections: VocabularySection[] = [
    {
      title: t('vocabulary.basicVocabularyTitle'),
      description: t('vocabulary.basicVocabularyDescription'),
      link: '/quizzes/vocabulary/basic',
      icon: 'pi pi-book',
    },
    {
      title: t('vocabulary.emeneauVocabularyTitle'),
      description: t('vocabulary.emeneauVocabularyDescription'),
      link: '/quizzes/vocabulary/emeneau',
      icon: 'pi pi-book-open',
    },
    {
      title: t('vocabulary.panchatatraTitle'),
      description: t('vocabulary.panchatatraDescription'),
      link: '/quizzes/vocabulary/texts',
      icon: 'pi pi-bookmark',
    },
  ];

  return (
    <div className="flex flex-column p-4">
      <h1 className="text-center mb-4">{t('nav.vocabulary')}</h1>
      <div className="mx-auto w-full" style={{ maxWidth: '900px' }}>
        <DataView
          value={vocabularySections}
          layout="list"
          listTemplate={(items) => (
            <div className="flex flex-column gap-2">
              {items.map((section, index) => (
                <div
                  key={index}
                  className="border-round-lg border-1 surface-border surface-card cursor-pointer hover:surface-hover transition-all transition-duration-200"
                  onClick={() => navigate(section.link)}
                >
                  <div
                    className="flex align-items-center justify-content-center border-circle flex-shrink-0"
                    style={{ width: '3rem', height: '3rem', backgroundColor: 'var(--surface-ground)' }}
                  >
                    <i className={`${section.icon} text-xl text-primary`} />
                  </div>
                  <div className="flex flex-column flex-1 gap-1">
                    <div className="font-bold text-lg">{section.title}</div>
                    <div className="text-color-secondary text-sm">{section.description}</div>
                  </div>
                </div>
              ))}
            </div>
          )}
          paginator={false}
        />
      </div>
    </div>
  );
};

export default VocabularyPage;
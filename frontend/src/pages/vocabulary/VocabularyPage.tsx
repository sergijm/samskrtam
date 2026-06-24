import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { useNavigate } from 'react-router-dom';

const VocabularyPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const vocabularySections = [
    {
      title: t('vocabulary.basicVocabularyTitle'),
      description: t('vocabulary.basicVocabularyDescription'),
      link: '/quizzes/vocabulary/basic',
      icon: 'pi pi-book'
    },
    {
      title: t('vocabulary.emeneauVocabularyTitle'),
      description: t('vocabulary.emeneauVocabularyDescription'),
      link: '/quizzes/vocabulary/emeneau',
      icon: 'pi pi-book-open'
    },
    {
      title: t('vocabulary.panchatatraTitle'),
      description: t('vocabulary.panchatatraDescription'),
      link: '/quizzes/vocabulary/texts',
      icon: 'pi pi-bookmark'
    },
  ];

  const handleSectionClick = (link: string) => {
    navigate(link);
  };

  return (
      <div className="flex flex-column align-items-center justify-content-center p-4">
        <h1 className="text-center mb-5">{t('nav.vocabulary')}</h1>
        <div className="grid justify-content-center w-full" style={{ maxWidth: '1600px' }}>
          {vocabularySections.map((section, index) => (
              <div key={index} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
                <div
                    onClick={() => handleSectionClick(section.link)}
                    className="p-card p-component lesson-card flex flex-column text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
                >
                  <div className="p-card-body flex flex-column flex-grow-1">
                    <div className="p-card-title">{section.title}</div>
                    <div className="p-card-subtitle">{section.description}</div>
                    <div className="flex-grow-1 flex align-items-center justify-content-center">
                      <i className={`${section.icon} text-5xl text-primary`} />
                    </div>
                  </div>
                </div>
              </div>
          ))}
        </div>
      </div>
  );
};

export default VocabularyPage;
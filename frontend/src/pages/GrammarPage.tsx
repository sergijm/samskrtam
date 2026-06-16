import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { useNavigate } from 'react-router-dom';

const GrammarPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const grammarSections = [
    {
      title: t('grammar.sandhiExercisesTitle'),
      description: t('grammar.sandhiExercisesDescription'),
      link: '/grammar/emeneau-exercises',
      icon: 'pi pi-pencil'
    },
    {
      title: t('grammar.sandhiQuizzesTitle'),
      description: t('grammar.sandhiQuizzesDescription'),
      link: '/grammar/emeneau-quizzes',
      icon: 'pi pi-question-circle'
    },
    {
      title: t('grammar.sandhiRulesTitle'),
      description: t('grammar.sandhiRulesDescription'),
      link: '/grammar/emeneau-rules',
      icon: 'pi pi-book'
    },
    {
      title: t('grammar.declensionsTitle'),
      description: t('grammar.declensionsDescription'),
      link: '/grammar/declensions',
      icon: 'pi pi-table'
    },
    {
      title: t('grammar.conjugationsTitle'),
      description: t('grammar.conjugationsDescription'),
      link: '/grammar/conjugations',
      icon: 'pi pi-share-alt'
    },
  ];

  const handleSectionClick = (link: string) => {
    navigate(link);
  };

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <h1 className="text-center mb-5">{t('nav.grammar')}</h1>
      <div className="grid justify-content-center w-full" style={{ maxWidth: '1200px' }}>
        {grammarSections.map((section, index) => (
          <div key={index} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
            <div
              onClick={() => handleSectionClick(section.link)}
              className="p-card p-component quiz-card flex flex-column text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
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

export default GrammarPage;

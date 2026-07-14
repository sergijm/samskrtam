import React from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';

interface DashboardTile {
  titleKey: string;
  descriptionKey: string;
  icon: string;
  route: string;
}

/**
 * Второстепенные плитки категорий внизу дашборда (§5.6 IA).
 *
 * Наименьший приоритет по вёрстке — навигационные ссылки
 * к основным разделам платформы.
 */
const CategoryTiles: React.FC = () => {
  const { t } = useTranslation();

  const tiles: DashboardTile[] = [
    {
      titleKey: 'nav.grammar',
      descriptionKey: 'dashboard.grammarDescription',
      icon: 'pi pi-pencil',
      route: '/grammar',
    },
    {
      titleKey: 'nav.vocabulary',
      descriptionKey: 'dashboard.vocabularyDescription',
      icon: 'pi pi-book',
      route: '/quizzes/vocabulary',
    },
    {
      titleKey: 'nav.dictionary',
      descriptionKey: 'dashboard.dictionaryDescription',
      icon: 'pi pi-search',
      route: '/dictionary',
    },
    {
      titleKey: 'nav.sangraha',
      descriptionKey: 'dashboard.sangrahaDescription',
      icon: 'pi pi-bookmark',
      route: '/sangraha',
    },
    {
      titleKey: 'nav.statistics',
      descriptionKey: 'dashboard.statisticsDescription',
      icon: 'pi pi-chart-line',
      route: '/statistics',
    },
    {
      titleKey: 'nav.leaderboard',
      descriptionKey: 'dashboard.leaderboardDescription',
      icon: 'pi pi-trophy',
      route: '/leaderboard',
    },
  ];

  return (
    <div className="dashboard-category-tiles fade-in">
      <h3 className="text-lg font-medium mb-3">{t('dashboard.categoryTilesTitle')}</h3>
      <div className="grid">
        {tiles.map((tile) => (
          <div key={tile.route} className="col-12 sm:col-6 lg:col-4 p-2">
            <Link to={tile.route} className="no-underline">
              <Card
                className="dashboard-category-card cursor-pointer hover:shadow-4 transition-all transition-duration-200 border-round-xl"
                pt={{
                  body: { className: 'p-3' },
                  content: { className: 'p-0' },
                }}
              >
                <div className="flex align-items-center gap-3">
                  <div className="flex align-items-center justify-content-center w-3rem h-3rem bg-primary-50 border-round-lg">
                    <i className={`${tile.icon} text-xl text-primary`} />
                  </div>
                  <div className="flex flex-column gap-1">
                    <span className="font-medium">{t(tile.titleKey)}</span>
                    <span className="text-sm text-500">{t(tile.descriptionKey)}</span>
                  </div>
                </div>
              </Card>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
};

export default React.memo(CategoryTiles);

import React, { useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Menu } from 'primereact/menu';
import type { MenuItem } from 'primereact/menuitem';

interface HeaderNavItem {
  label: string;
  route: string;
}

const HeaderNav: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const lessonsMenuRef = useRef<Menu>(null);
  const analysisMenuRef = useRef<Menu>(null);

  const isActive = (route: string) => location.pathname === route;

  const mainItems: HeaderNavItem[] = [
    { label: t('nav.dictionary'), route: '/dictionary' },
    { label: t('nav.sangraha'), route: '/sangraha' },
  ];

  const lessonsItems: MenuItem[] = [
    { label: t('nav.phonetics'), command: () => navigate('/alphabet') },
    { label: t('nav.grammar'), command: () => navigate('/grammar') },
    { label: t('nav.vocabulary'), command: () => navigate('/lexicon') },
  ];

  const analysisItems: MenuItem[] = [
    { label: t('writing.transliteration'), command: () => navigate('/writing/transliteration') },
    { label: `${t('nav.grammarAnalysis')} (${t('nav.underDevelopment')})`, command: () => navigate('/analysis') },
  ];

  return (
    <nav className="layout-topbar-nav flex align-items-center gap-2">
      <button
        type="button"
        className={`header-nav-btn${isActive('/dashboard') ? ' active' : ''}`}
        onClick={() => navigate('/dashboard')}
      >
        {t('nav.dashboard')}
      </button>
      <button
        type="button"
        className="header-nav-btn"
        aria-haspopup="menu"
        aria-label={t('nav.lessons')}
        onClick={(e) => lessonsMenuRef.current?.toggle(e)}
      >
        <span>{t('nav.lessons')}</span>
        <i className="pi pi-chevron-down" style={{ fontSize: '0.8rem' }} />
      </button>
      <Menu model={lessonsItems} popup popupAlignment="right" ref={lessonsMenuRef} />
      {mainItems.map((item) => (
        <button
          key={item.route}
          type="button"
          className={`header-nav-btn${isActive(item.route) ? ' active' : ''}`}
          onClick={() => navigate(item.route)}
        >
          {item.label}
        </button>
      ))}
      <button
        type="button"
        className="header-nav-btn"
        aria-haspopup="menu"
        aria-label={t('nav.analysis')}
        onClick={(e) => analysisMenuRef.current?.toggle(e)}
      >
        <span>{t('nav.analysis')}</span>
        <i className="pi pi-chevron-down" style={{ fontSize: '0.8rem' }} />
      </button>
      <Menu model={analysisItems} popup popupAlignment="right" ref={analysisMenuRef} />
    </nav>
  );
};

export default HeaderNav;
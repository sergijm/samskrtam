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
  const writingMenuRef = useRef<Menu>(null);

  const isActive = (route: string) => location.pathname === route;

  const mainItems: HeaderNavItem[] = [
    { label: t('nav.dashboard'), route: '/dashboard' },
    { label: t('nav.grammar'), route: '/grammar' },
    { label: t('nav.vocabulary'), route: '/lexicon' },
    { label: t('nav.dictionary'), route: '/dictionary' },
    { label: t('nav.sangraha'), route: '/sangraha' },
  ];

  const writingItems: MenuItem[] = [
    { label: t('writing.alphabet'), command: () => navigate('/writing/alphabet') },
    { label: `${t('writing.ligatures')} [${t('status.planned')}]`, disabled: true },
    { label: t('writing.transliteration'), command: () => navigate('/writing/transliteration') },
  ];

  return (
    <nav className="layout-topbar-nav flex align-items-center gap-2">
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
        aria-label={t('nav.writing')}
        onClick={(e) => writingMenuRef.current?.toggle(e)}
      >
        <span>{t('nav.writing')}</span>
        <i className="pi pi-chevron-down" style={{ fontSize: '0.8rem' }} />
      </button>
      <Menu model={writingItems} popup popupAlignment="right" ref={writingMenuRef} />
    </nav>
  );
};

export default HeaderNav;

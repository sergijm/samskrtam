import React from 'react';
import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMe } from '../../hooks/useUser'; // Import useMe

const Sidebar = () => {
  const { t } = useTranslation();
  const { data: user } = useMe(); // Get user data from react-query

  const navItems = [
    { to: '/dashboard', label: t('nav.dashboard'), icon: 'pi-home' },
    { to: '/quizzes', label: t('nav.quizzes'), icon: 'pi-question-circle' },
    { to: '/dictionary', label: t('nav.dictionary'), icon: 'pi-book' },
    { to: '/statistics', label: t('nav.statistics'), icon: 'pi-chart-bar' },
    { to: '/leaderboard', label: t('nav.leaderboard'), icon: 'pi-sitemap' },
  ];

  if (user?.roles.includes('ADMIN')) { // Check for roles array
    navItems.push({ to: '/admin/users', label: t('nav.admin'), icon: 'pi-shield' });
  }

  return (
    <div className="layout-sidebar">
      <ul className="layout-menu">
        {navItems.map(item => (
          <li key={item.to}>
            <NavLink to={item.to} className={({ isActive }) => `p-ripple ${isActive ? 'router-link-active' : ''}`}>
              <i className={`pi ${item.icon} layout-menuitem-icon`}></i>
              <span className="layout-menuitem-text">{item.label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default Sidebar;

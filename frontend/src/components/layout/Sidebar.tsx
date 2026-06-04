import React from 'react';
import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../../store/authStore';

const Sidebar = () => {
  const { t } = useTranslation();
  const user = useAuthStore((state) => state.user);

  const navItems = [
    { to: '/dashboard', label: t('nav.dashboard'), icon: 'pi-home' }, // Changed '/' to '/dashboard'
    { to: '/quizzes', label: t('nav.quizzes'), icon: 'pi-question-circle' },
    { to: '/dictionary', label: t('nav.dictionary'), icon: 'pi-book' },
    { to: '/statistics', label: t('nav.statistics'), icon: 'pi-chart-bar' },
    { to: '/leaderboard', label: t('nav.leaderboard'), icon: 'pi-sitemap' },
    // Removed the settings item from here
    // { to: '/settings', label: t('nav.settings'), icon: 'pi-cog' },
  ];

  if (user?.role === 'ADMIN') {
    navItems.push({ to: '/admin/users', label: t('nav.admin'), icon: 'pi-shield' }); // Updated link to /admin/users
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

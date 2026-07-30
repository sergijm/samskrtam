import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useMe } from '../../hooks/useUser';
import { useSidebarStore } from '../../store/sidebarStore';
import { LocaleSwitcher } from '../common/LocaleSwitcher';
import { ThemeSwitcher } from '../common/ThemeSwitcher';
import UserAvatar from '../user/UserAvatar';
import { IconButton } from '../common/buttons';

const Header = () => {
  const { isAuthenticated, logout } = useAuthStore();
  const { data: user } = useMe();
  const { collapsed, toggle } = useSidebarStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="layout-topbar flex justify-content-between align-items-center">
      <div className="flex align-items-center gap-2">
        <Link to="/" className="layout-topbar-logo no-underline text-xl font-bold flex align-items-center" style={{ padding: 0, margin: 0 }}>
          <img src="/logo.png" alt="Aksharamārga Logo"
               style={{ height: '70px', width: '70px', marginRight: '0' }} />
          <span className="text-3xl font-bold " >Akshara Mārga</span>
        </Link>
        {isAuthenticated && (
          <IconButton
            iconName={`pi-chevron-${collapsed ? 'right' : 'left'}`}
            className="sidebar-toggle-btn ml-2"
            onClick={toggle}
            tooltip={collapsed ? 'Show sidebar' : 'Hide sidebar'}
            tooltipOptions={{ position: 'bottom' }}
            aria-label="Toggle sidebar"
          />
        )}
      </div>
      <div className="layout-topbar-menu flex align-items-center gap-3">
        <ThemeSwitcher />
        <LocaleSwitcher />
        {isAuthenticated && user && (
          <Link to="/settings" className="no-underline text-color mr-2">
            <UserAvatar
              username={user.username}
              firstName={user.firstName}
              lastName={user.lastName}
              email={user.email}
              avatarUrl={user.avatarUrl}
            />
          </Link>
        )}
        {isAuthenticated && (
          <IconButton iconName="pi-sign-out" onClick={handleLogout} />
        )}
      </div>
    </div>
  );
};

export default Header;


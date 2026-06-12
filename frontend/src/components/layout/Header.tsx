import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { Button } from 'primereact/button';
import { LocaleSwitcher } from '../common/LocaleSwitcher';
import { ThemeSwitcher } from '../common/ThemeSwitcher';
import UserAvatar from '../user/UserAvatar';

const Header = () => {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="layout-topbar flex justify-content-between align-items-center"> {/* Removed px-4 py-2 */}
      <Link to="/" className="layout-topbar-logo no-underline text-xl font-bold flex align-items-center" style={{ padding: 0, margin: 0 }}> {/* Ensure no padding/margin */}
        <img src="/logo.png" alt="Aksharamārga Logo" style={{ height: '70px', width: '70px', marginRight: '0' }} /> {/* Set size and remove margin */}
        <span className="text-3xl font-bold">Akshara Mārga</span>
      </Link>
      <div className="layout-topbar-menu flex align-items-center gap-3">
        <ThemeSwitcher />
        <LocaleSwitcher />
        {user && (
          <Link to="/settings" className="no-underline text-color">
            <UserAvatar
              username={user.username}
              firstName={user.firstName}
              lastName={user.lastName}
              email={user.email}
              avatarUrl={user.avatarUrl}
            />
          </Link>
        )}
        {user && (
          <Button icon="pi pi-sign-out" className="p-button-text" onClick={handleLogout} />
        )}
      </div>
    </div>
  );
};

export default Header;

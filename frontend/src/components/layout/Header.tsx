import React from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { Button } from 'primereact/button';
import { LocaleSwitcher } from '../common/LocaleSwitcher';
import { ThemeSwitcher } from '../common/ThemeSwitcher';
import UserAvatar from '../user/UserAvatar';

const Header = () => {
  const { user, logout } = useAuthStore();

  return (
    <div className="layout-topbar flex justify-content-between align-items-center px-4 py-2">
      <Link to="/" className="layout-topbar-logo no-underline text-xl font-bold">
        <span>SamskrtamApp</span>
      </Link>
      <div className="layout-topbar-menu flex align-items-center gap-3"> {/* Adjusted gap */}
        {user && (
          <> {/* Use fragment to group without adding extra div */}
            <UserAvatar username={user.username} />
            <Button icon="pi pi-sign-out" className="p-button-text" onClick={logout} />
          </>
        )}
        <ThemeSwitcher />
        <LocaleSwitcher />
      </div>
    </div>
  );
};

export default Header;

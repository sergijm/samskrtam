import React from 'react';
import { Outlet, useLocation } from 'react-router-dom'; // Added useLocation
import Header from './Header';
import Sidebar from './Sidebar';

const AppLayout = () => {
  const location = useLocation();
  const showSidebar = location.pathname !== '/'; // Hide sidebar on dashboard page

  return (
    <div className="layout-wrapper">
      <Header />
      <div className="layout-main-container">
        {showSidebar && <Sidebar />} {/* Conditionally render Sidebar */}
        <div className="layout-main">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default AppLayout;

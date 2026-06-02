import React from 'react';
import { Outlet } from 'react-router-dom'; // Removed useLocation as it's no longer needed for conditional rendering
import Header from './Header';
// import Sidebar from './Sidebar'; // Removed Sidebar import

const AppLayout = () => {
  // const location = useLocation(); // No longer needed
  // const showSidebar = location.pathname !== '/'; // No longer needed

  return (
    <div className="layout-wrapper">
      <Header />
      <div className="layout-main-container">
        {/* Removed Sidebar completely */}
        {/* {showSidebar && <Sidebar />} */}
        <div className="layout-main">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default AppLayout;

import React from 'react';
import Header from './Header';

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  return (
    <div className="layout-wrapper">
      <Header />
      <div className="layout-main-container">
        <div className="layout-main">
          {children}
        </div>
      </div>
    </div>
  );
};

export default AppLayout;

import React, { useCallback, useEffect, useRef } from 'react';
import Header from './Header';
import Sidebar from './Sidebar';
import { useSidebarStore } from '../../store/sidebarStore';

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const { collapsed, width, setWidth } = useSidebarStore();
  const resizeRef = useRef<HTMLDivElement>(null);
  const dragging = useRef(false);

  const onMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    dragging.current = true;
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  }, []);

  useEffect(() => {
    const onMouseMove = (e: MouseEvent) => {
      if (!dragging.current) return;
      setWidth(e.clientX);
    };
    const onMouseUp = () => {
      if (!dragging.current) return;
      dragging.current = false;
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
    return () => {
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);
    };
  }, [setWidth]);

  return (
    <div className="layout-wrapper">
      <Header />
      <div className="layout-main-container">
        <div
          className={`layout-sidebar-wrapper${collapsed ? ' collapsed' : ''}`}
          style={{ width: collapsed ? 0 : width }}
        >
          <div className="layout-sidebar-inner" style={{ width }}>
            <Sidebar />
          </div>
          {!collapsed && (
            <div
              ref={resizeRef}
              className="layout-sidebar-resize-handle"
              onMouseDown={onMouseDown}
            />
          )}
        </div>
        <div className="layout-main">
          {children}
        </div>
      </div>
    </div>
  );
};

export default AppLayout;

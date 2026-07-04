import React, { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PrimeReactProvider } from 'primereact/api';

// Components
import ErrorBoundary from './components/common/ErrorBoundary';

// Routes
import AppRoutes from './routes/AppRoutes';
// Stores
import { useThemeStore } from './store/themeStore';
import { useAuthStore } from './store/authStore';

// Hooks
import { useMe } from './hooks/useUser';

// i18n
import './i18n';

const queryClient = new QueryClient();

export default function App() {
    const { theme, setTheme } = useThemeStore();
    const { isAuthenticated } = useAuthStore();
    const { data: userData } = useMe();
    const setUser = useAuthStore((s) => s.setUser);

    useEffect(() => {
        setTheme(theme);
    }, [theme, setTheme]);

    useEffect(() => {
        if (isAuthenticated && userData) {
            setUser(userData);
        }
    }, [isAuthenticated, userData, setUser]);

    return (
        <ErrorBoundary>
            <PrimeReactProvider>
                <QueryClientProvider client={queryClient}>
                    <BrowserRouter>
                        <AppRoutes />
                    </BrowserRouter>
                </QueryClientProvider>
            </PrimeReactProvider>
        </ErrorBoundary>
    );
}
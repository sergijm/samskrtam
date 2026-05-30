import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { PrimeReactProvider } from 'primereact/api';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import 'primereact/resources/themes/lara-light-blue/theme.css';
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';

import App from './App';

// ===================================================================
// ОТЛАДОЧНЫЙ ВЫВОД: Проверяем, что Vite загрузил переменные
console.log("VITE_API_URL from main.tsx:", import.meta.env.VITE_API_URL);
// ===================================================================

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error("Root element not found");

const queryClient = new QueryClient();

createRoot(rootElement).render(
  <StrictMode>
    <PrimeReactProvider>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </PrimeReactProvider>
  </StrictMode>
);

import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type Theme = 'light' | 'dark';

const THEME_HREFS: Record<Theme, string> = {
  light: '/themes/lara-light-blue/theme.css',
  dark: '/themes/lara-dark-blue/theme.css',
};

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: (localStorage.getItem('theme') as Theme) ?? 'light',
      setTheme: (theme) => {
        const link = document.getElementById('theme-link') as HTMLLinkElement;
        if (link) {
          link.href = THEME_HREFS[theme];
        }
        document.documentElement.setAttribute('data-theme', theme);
        set({ theme });
      },
    }),
    {
      name: 'theme-storage',
    }
  )
);

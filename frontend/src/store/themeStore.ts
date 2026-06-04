import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type Theme = 'light' | 'dark';

const THEME_HREFS: Record<Theme, string> = {
  light: '/themes/lara-light-amber/theme.css', // Changed to orange theme
  dark: '/themes/lara-dark-amber/theme.css',   // Changed to orange theme
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
        // Ensure the theme is valid before trying to set the href
        if (theme && THEME_HREFS[theme]) {
          const link = document.getElementById('theme-link') as HTMLLinkElement;
          if (link) {
            link.href = THEME_HREFS[theme];
          }
        } else {
          console.warn(`Attempted to set an invalid theme: ${theme}. Defaulting to 'light'.`);
          theme = 'light'; // Fallback to a default valid theme
          const link = document.getElementById('theme-link') as HTMLLinkElement;
          if (link) {
            link.href = THEME_HREFS[theme];
          }
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

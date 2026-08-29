import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    allowedHosts: ['samskrtam.local'],
    host: '0.0.0.0', // Для доступа из Docker и по кастомным хостам
    hmr: {
      clientPort: 3000,
    },
    watch: {
      usePolling: true,
    },

  },
  build: {
    // Страницы теперь грузятся через React.lazy() (см. src/routes/AppRoutes.tsx),
    // поэтому каждый роут уже станет отдельным чанком автоматически.
    // Здесь дополнительно выносим крупные вендорские библиотеки в свои чанки,
    // чтобы браузер кэшировал их отдельно от кода приложения.
    rollupOptions: {
      output: {
        // Функциональная форма (вместо списков имён пакетов) — не форсирует
        // резолюцию "главной" точки входа пакета. Это важно для primereact:
        // приложение использует только deep-imports (primereact/button и т.п.),
        // а его package.json "main" указывает на кухонный комбайн-бандл
        // primereact.all.esm.min.js, который тянет необязательные peer-зависимости
        // (chart.js, quill, fullcalendar...) — при указании 'primereact' строкой
        // Rollup пытается резолвить именно этот файл целиком и падает на них.
        manualChunks(id) {
          if (!id.includes('node_modules')) return;
          if (/node_modules\/(react|react-dom|react-router-dom)\//.test(id)) {
            return 'vendor-react';
          }
          if (id.includes('node_modules/primereact/')) {
            return 'vendor-primereact';
          }
          if (/node_modules\/(@tanstack\/react-query|axios)\//.test(id)) {
            return 'vendor-query';
          }
          if (/node_modules\/(i18next|react-i18next|i18next-browser-languagedetector)\//.test(id)) {
            return 'vendor-i18n';
          }
          if (/node_modules\/(zustand|react-hook-form|@indic-transliteration)\//.test(id)) {
            return 'vendor-misc';
          }
        },
      },
    },
    // Понижаем порог предупреждения, чтобы Vite подсказывал,
    // если какой-то чанк снова разрастётся.
    chunkSizeWarningLimit: 600,
  },
});

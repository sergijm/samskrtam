import { ProgressSpinner } from 'primereact/progressspinner';

/**
 * Fallback UI, показываемый пока React.lazy() подгружает чанк страницы.
 * Используется в <Suspense fallback={<PageLoader />}> в AppRoutes.
 */
export default function PageLoader() {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: '100%',
        height: '100%',
        minHeight: '60vh',
      }}
    >
      <ProgressSpinner strokeWidth="4" />
    </div>
  );
}

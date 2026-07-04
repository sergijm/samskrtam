import { useTranslation } from 'react-i18next';
import { Controller, Control } from 'react-hook-form';
import { RadioButton } from 'primereact/radiobutton';

interface FormValues {
  username: string;
  firstName: string;
  lastName: string;
  quizSize: number;
  theme: string;
  locale: string;
}

interface PreferencesSectionProps {
  control: Control<FormValues>;
  onThemeChange: (theme: string) => void;
  onLocaleChange: (locale: string) => void;
}

export default function PreferencesSection({
  control,
  onThemeChange,
  onLocaleChange,
}: PreferencesSectionProps) {
  const { t } = useTranslation();

  return (
    <>
      <div className="flex align-items-center mb-4">
        <span className="font-bold w-10rem mr-3">{t('settings.theme')}</span>
        <div className="flex flex-wrap gap-3">
          <div className="field-radiobutton">
            <Controller
              name="theme"
              control={control}
              render={({ field }) => (
                <RadioButton
                  inputId="theme-light"
                  {...field}
                  value="light"
                  checked={field.value === 'light'}
                  onChange={(e) => {
                    field.onChange(e);
                    onThemeChange('light');
                  }}
                />
              )}
            />
            <label htmlFor="theme-light" className="ml-2">
              {t('settings.themeLight')}
            </label>
          </div>
          <div className="field-radiobutton">
            <Controller
              name="theme"
              control={control}
              render={({ field }) => (
                <RadioButton
                  inputId="theme-dark"
                  {...field}
                  value="dark"
                  checked={field.value === 'dark'}
                  onChange={(e) => {
                    field.onChange(e);
                    onThemeChange('dark');
                  }}
                />
              )}
            />
            <label htmlFor="theme-dark" className="ml-2">
              {t('settings.themeDark')}
            </label>
          </div>
        </div>
      </div>

      <div className="flex align-items-center mb-4">
        <span className="font-bold w-10rem mr-3">{t('settings.language')}</span>
        <div className="flex flex-wrap gap-3">
          <div className="field-radiobutton">
            <Controller
              name="locale"
              control={control}
              render={({ field }) => (
                <RadioButton
                  inputId="locale-ru"
                  {...field}
                  value="ru"
                  checked={field.value === 'ru'}
                  onChange={(e) => {
                    field.onChange(e);
                    onLocaleChange('ru');
                  }}
                />
              )}
            />
            <label htmlFor="locale-ru" className="ml-2">
              {t('settings.languageRu')}
            </label>
          </div>
          <div className="field-radiobutton">
            <Controller
              name="locale"
              control={control}
              render={({ field }) => (
                <RadioButton
                  inputId="locale-en"
                  {...field}
                  value="en"
                  checked={field.value === 'en'}
                  onChange={(e) => {
                    field.onChange(e);
                    onLocaleChange('en');
                  }}
                />
              )}
            />
            <label htmlFor="locale-en" className="ml-2">
              {t('settings.languageEn')}
            </label>
          </div>
        </div>
      </div>
    </>
  );
}
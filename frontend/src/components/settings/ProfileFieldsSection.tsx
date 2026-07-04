import { useTranslation } from 'react-i18next';
import { Controller, Control } from 'react-hook-form';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';

interface FormValues {
  username: string;
  firstName: string;
  lastName: string;
  quizSize: number;
  theme: string;
  locale: string;
}

interface ProfileFieldsSectionProps {
  control: Control<FormValues>;
  errors: Record<string, unknown>;
}

const quizSizeOptions = [
  { label: '5', value: 5 }, { label: '10', value: 10 }, { label: '15', value: 15 },
  { label: '20', value: 20 }, { label: '30', value: 30 }, { label: '50', value: 50 },
];

export default function ProfileFieldsSection({ control }: ProfileFieldsSectionProps) {
  const { t } = useTranslation();

  return (
    <>
      <div className="field mb-4 flex align-items-center">
        <label htmlFor="username" className="font-bold w-10rem mr-3">
          {t('settings.username')}
        </label>
        <div className="flex-grow-1">
          <Controller
            name="username"
            control={control}
            rules={{ required: t('validation.usernameRequired') }}
            render={({ field, fieldState }) => (
              <>
                <InputText
                  id={field.name}
                  {...field}
                  className={fieldState.invalid ? 'p-invalid' : ''}
                />
                {fieldState.error && (
                  <small className="p-error">{fieldState.error.message}</small>
                )}
              </>
            )}
          />
        </div>
      </div>

      <div className="field mb-4 flex align-items-center">
        <label htmlFor="firstName" className="font-bold w-10rem mr-3">
          {t('settings.firstName')}
        </label>
        <div className="flex-grow-1">
          <Controller
            name="firstName"
            control={control}
            render={({ field }) => <InputText id={field.name} {...field} />}
          />
        </div>
      </div>

      <div className="field mb-4 flex align-items-center">
        <label htmlFor="lastName" className="font-bold w-10rem mr-3">
          {t('settings.lastName')}
        </label>
        <div className="flex-grow-1">
          <Controller
            name="lastName"
            control={control}
            render={({ field }) => <InputText id={field.name} {...field} />}
          />
        </div>
      </div>

      <div className="field mb-4 flex align-items-center">
        <label htmlFor="quizSize" className="font-bold w-10rem mr-3">
          {t('settings.quizSize')}
        </label>
        <div className="flex-grow-1">
          <Controller
            name="quizSize"
            control={control}
            render={({ field }) => (
              <Dropdown id={field.name} {...field} options={quizSizeOptions} />
            )}
          />
        </div>
      </div>
    </>
  );
}
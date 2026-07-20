import React from 'react';
import { Button, ButtonProps } from 'primereact/button';
import { useTranslation } from 'react-i18next';

/**
 * Варианты кнопок, встречающиеся вне таблиц по всему интерфейсу.
 *
 * Паттерны (аудит 2025):
 *   page-action   — главное действие страницы: «Создать», «Добавить», «Редактировать»
 *   form-submit   — отправка формы: type="submit" + loading
 *   form-cancel   — отмена в форме: p-button-text + onClick-навигация
 *   icon-only     — только иконка: toggle sidebar, logout, delete в карточках
 *   filter-reset  — сброс фильтров: pi-filter-slash + outlined
 *   navigation    — переход на другой маршрут: outlined/text + chevron
 *   cta-primary   — большой CTA: «Продолжить», «Изучить»
 *   danger        — деструктивное действие: удаление, блокировка
 *   dialog-action — кнопка внутри Dialog.footer
 */

export type PageButtonVariant =
  | 'page-action'
  | 'form-submit'
  | 'form-cancel'
  | 'icon-only'
  | 'filter-reset'
  | 'navigation'
  | 'cta-primary'
  | 'danger'
  | 'dialog-action';

export interface PageButtonProps extends Omit<ButtonProps, 'icon' | 'label'> {
  variant: PageButtonVariant;
  /** Ключ i18n или явная строка */
  labelKey?: string;
  /** Иконка (класс PrimeIcons без префикса `pi-` или полный `pi pi-...`) */
  iconName?: string;
  /** Позиция иконки */
  iconPosition?: 'left' | 'right';
  /** Показать спиннер загрузки */
  loading?: boolean;
  /** Для variant="navigation" — target route (передаётся onClick вместо href) */
  onClick?: (e: React.MouseEvent<HTMLButtonElement>) => void;
}

const VARIANT_CLASSES: Record<PageButtonVariant, string> = {
  'page-action': 'p-button-sm',
  'form-submit': 'p-button-sm',
  'form-cancel': 'p-button-text  p-button-outlined  p-button-sm',
  'icon-only': 'p-button-text p-button-rounded',
  'filter-reset': 'p-button-outlined  p-button-sm',
  'navigation': 'p-button-outlined p-button-sm',
  'cta-primary': 'p-button-outlined  p-button-sm',
  'danger': 'p-button-danger p-button-outlined  p-button-sm',
  'dialog-action': 'p-button-sm'
};

const VARIANT_DEFAULTS: Record<
  PageButtonVariant,
  { icon?: string; iconPos?: 'left' | 'right'; severity?: ButtonProps['severity'] }
> = {
  'page-action': { icon: 'pi pi-plus' },
  'form-submit': { icon: 'pi pi-check' },
  'form-cancel': { icon: 'pi pi-times', severity: 'secondary' },
  'icon-only': {},
  'filter-reset': { icon: 'pi pi-filter-slash', severity: 'secondary' },
  'navigation': { icon: 'pi pi-arrow-right', iconPos: 'right' },
  'cta-primary': { icon: 'pi pi-play', iconPos: 'right' },
  'danger': { icon: 'pi pi-trash' },
  'dialog-action': {},
};

/**
 * Единый компонент для всех кнопок вне таблиц.
 *
 * Примеры:
 *   <PageButton variant="page-action" labelKey="groups.createGroup" onClick={...} />
 *   <PageButton variant="form-submit" labelKey="common.save" loading={isPending} />
 *   <PageButton variant="form-cancel" labelKey="common.cancel" onClick={goBack} />
 *   <PageButton variant="icon-only" iconName="pi-sign-out" onClick={logout} />
 *   <PageButton variant="filter-reset" onClick={resetFilters} />
 *   <PageButton variant="danger" labelKey="common.delete" onClick={onDelete} loading={isDeleting} />
 */
export const PageButton: React.FC<PageButtonProps> = ({
  variant,
  labelKey,
  iconName,
  iconPosition,
  loading,
  className,
  onClick,
  type,
  ...rest
}) => {
  const { t } = useTranslation();

  const defaultConfig = VARIANT_DEFAULTS[variant];
  const baseClass = VARIANT_CLASSES[variant];

  // Определяем иконку: явная > дефолтная по варианту
  const resolvedIcon = iconName
    ? iconName.startsWith('pi ')
      ? iconName
      : `pi ${iconName}`
    : defaultConfig.icon;

  // Определяем позицию иконки
  const resolvedIconPos = iconPosition ?? defaultConfig.iconPos ?? 'left';

  // Определяем label
  const resolvedLabel = labelKey ? t(labelKey) : rest.label;

  // Определяем type
  const resolvedType = type ?? (variant === 'form-submit' ? 'submit' : 'button');

  // Определяем severity
  const resolvedSeverity = (rest as any).severity ?? defaultConfig.severity;

  // Собираем className
  const resolvedClassName = [baseClass, className].filter(Boolean).join(' ');

  return (
    <Button
      {...rest}
      type={resolvedType}
      icon={resolvedIcon}
      iconPos={resolvedIconPos}
      label={resolvedLabel}
      loading={loading}
      className={resolvedClassName}
      onClick={onClick}
      severity={resolvedSeverity}
    />
  );
};

/**
 * Специализированные компоненты для наиболее частых паттернов.
 * Позволяют сократить шаблонный код.
 */

/** Главное действие страницы: «Создать», «Добавить» */
export const CreateButton: React.FC<Omit<PageButtonProps, 'variant'>> = (props) => (
  <PageButton variant="page-action" {...props} />
);

/** Отправка формы */
export const SubmitButton: React.FC<Omit<PageButtonProps, 'variant' | 'type'>> = (props) => (
  <PageButton variant="form-submit" type="submit" {...props} />
);

/** Отмена в форме */
export const CancelButton: React.FC<Omit<PageButtonProps, 'variant'>> = (props) => (
  <PageButton variant="form-cancel" {...props} />
);

/** Только иконка: toggle, logout, delete */
export const IconButton: React.FC<Omit<PageButtonProps, 'variant'>> = (props) => (
  <PageButton variant="icon-only" {...props} />
);

/** Сброс фильтров */
export const ResetFiltersButton: React.FC<Omit<PageButtonProps, 'variant'>> = (props) => (
  <PageButton variant="filter-reset" {...props} />
);

/** Удаление / деструктивное действие */
export const DangerButton: React.FC<Omit<PageButtonProps, 'variant'>> = (props) => (
  <PageButton variant="danger" {...props} />
);

/** Большой CTA */
export const CtaButton: React.FC<Omit<PageButtonProps, 'variant'>> = (props) => (
  <PageButton variant="cta-primary" {...props} />
);

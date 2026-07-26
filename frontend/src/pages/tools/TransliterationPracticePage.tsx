import React, { useState, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { InputTextarea } from 'primereact/inputtextarea';
import { SelectButton, type SelectButtonChangeEvent } from 'primereact/selectbutton';
import {
  asciiToDevanagari,
  asciiToIast,
  devanagariToIast,
  iastToDevanagari,
  type InputScheme,
} from '../../utils/transliteration';
import DevanagariKeyboard from '../../components/transliteration/DevanagariKeyboard';

type Direction = 'ascii-to-devanagari' | 'iast-to-devanagari' | 'devanagari-to-iast';

interface SelectOption<T extends string> {
  label: string;
  value: T;
}

const TransliterationPracticePage: React.FC = () => {
  const { t } = useTranslation();

  const [input, setInput] = useState('');
  const [direction, setDirection] = useState<Direction>('ascii-to-devanagari');
  const [scheme, setScheme] = useState<InputScheme>('hk');
  const inputRef = useRef<HTMLTextAreaElement | null>(null);

  // Clear input when direction changes
  const handleDirectionChange = useCallback((e: SelectButtonChangeEvent) => {
    setDirection(e.value as Direction);
    setInput('');
  }, []);

  const handleSchemeChange = useCallback((e: SelectButtonChangeEvent) => {
    setScheme(e.value as InputScheme);
    setInput('');
  }, []);

  const handleInputChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setInput(e.target.value);
    },
    [],
  );

  // Called from virtual keyboard on key press
  const handleKeyboardInput = useCallback((text: string) => {
    setInput(text);
  }, []);

  // Full re-conversion on every render — no incremental logic
  const devanagariOutput =
    direction === 'ascii-to-devanagari'
      ? asciiToDevanagari(input, scheme)
      : direction === 'iast-to-devanagari'
        ? iastToDevanagari(input)
        : null;

  const iastOutput =
    direction === 'ascii-to-devanagari'
      ? asciiToIast(input, scheme)
      : direction === 'iast-to-devanagari'
        ? input // IAST input → show unchanged as IAST output
        : devanagariToIast(input);

  const directionOptions: SelectOption<Direction>[] = [
    { label: t('transliteration.directionAscii'), value: 'ascii-to-devanagari' },
    { label: t('transliteration.directionIast'), value: 'iast-to-devanagari' },
    { label: t('transliteration.directionDevanagari'), value: 'devanagari-to-iast' },
  ];

  const schemeOptions: SelectOption<InputScheme>[] = [
    { label: 'HK', value: 'hk' },
    { label: 'ITRANS', value: 'itrans' },
    { label: 'SLP1', value: 'slp1' },
  ];

  const isAsciiMode = direction === 'ascii-to-devanagari';
  const isIastMode = direction === 'iast-to-devanagari';
  const isDevanagariInput = direction === 'devanagari-to-iast';

  return (
    <div className="flex flex-column gap-3 p-3">
      <h2 className="m-0">{t('transliteration.title')}</h2>
      <p className="m-0 text-color-secondary">{t('transliteration.description')}</p>

      {/* Direction toggle */}
      <div className="flex justify-content-center">
        <SelectButton
          value={direction}
          options={directionOptions}
          onChange={handleDirectionChange}
          pt={{ button: { className: 'p-button-sm' } }}
        />
      </div>

      {/* Scheme selector (ASCII mode only) */}
      {isAsciiMode && (
        <div className="flex align-items-center gap-2">
          <span className="text-sm font-medium">{t('transliteration.schemeLabel')}:</span>
          <SelectButton
            value={scheme}
            options={schemeOptions}
            onChange={handleSchemeChange}
            pt={{ button: { className: 'p-button-sm' } }}

          />
        </div>
      )}

      {/* Input field */}
      <div>
        <label className="block text-sm font-medium mb-1">
          {isAsciiMode
            ? t('transliteration.inputAscii', { scheme: scheme.toUpperCase() })
            : isIastMode
              ? t('transliteration.inputIast')
              : t('transliteration.inputDevanagari')}
        </label>
        <InputTextarea
          ref={(el) => {
            inputRef.current = el;
          }}
          value={input}
          onChange={handleInputChange}
          rows={6}
          autoResize
          className="w-full"
          placeholder={t('transliteration.inputPlaceholder')}
          style={{ fontFamily: isAsciiMode ? 'inherit' : "'Noto Sans Devanagari', 'Siddhanta', serif" }}
        />
      </div>

      {/* Virtual keyboard for Devanagari mode */}
      {isDevanagariInput && (
        <div>
          <p className="text-sm text-color-secondary m-0 mb-2">
            {t('transliteration.keyboardHint')}
          </p>
          <DevanagariKeyboard
            inputRef={inputRef}
            onTextChange={handleKeyboardInput}
          />
        </div>
      )}

      {/* Output */}
      <div className="grid">
        {devanagariOutput !== null && (
          <div className={isAsciiMode ? 'col-12 md:col-6' : 'col-12'}>
            <label className="block text-sm font-medium mb-1">{t('transliteration.outputDevanagari')}</label>
            <div
              className="p-3 border-round surface-100 text-lg"
              style={{
                minHeight: '6rem',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                lineHeight: 2,
                fontFamily:
                  "'Noto Sans Devanagari', 'Siddhanta', 'Lohit Devanagari', 'Sanskrit Text', serif",
              }}
            >
              {devanagariOutput || (
                <span className="text-color-secondary">
                  {t('transliteration.outputPlaceholder')}
                </span>
              )}
            </div>
          </div>
        )}
        {!isIastMode && (
          <div className={isAsciiMode ? 'col-12 md:col-6' : 'col-12'}>
            <label className="block text-sm font-medium mb-1">{t('transliteration.outputIast')}</label>
            <div
              className="p-3 border-round surface-100 text-lg"
              style={{
                minHeight: '6rem',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                lineHeight: 2,
              }}
            >
              {iastOutput || (
                <span className="text-color-secondary">
                  {t('transliteration.outputPlaceholder')}
                </span>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default TransliterationPracticePage;

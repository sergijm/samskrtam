import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { TabView, TabPanel } from 'primereact/tabview';
import { useDictionarySearch, useFrischLemma, useApteLemma } from '../../hooks/useDictionary';
import { MwDictionaryEntryDto, FrischEntryDto, ApteEntryDto } from '../../types';

const DictionaryPage = () => {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const query = searchParams.get('q');

  const [searchTerm, setSearchTerm] = useState(query || '');

  const {
    data: mwEntry,
    isPending: isMwSearching,
    error: mwError,
  } = useDictionarySearch(query);

  const {
    data: frischEntries,
    isPending: isFrischSearching,
    error: frischError,
  } = useFrischLemma(query);

  const {
    data: apteEntries,
    isPending: isApteSearching,
    error: apteError,
  } = useApteLemma(query);

  const handleSearch = () => {
    if (searchTerm.trim()) {
      navigate(`/dictionary?q=${encodeURIComponent(searchTerm.trim())}`);
    }
  };

  const stripHtmlTags = (html: string) => {
    if (!html) return '';
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const text = doc.body.textContent || '';
    return text.replace(/\s+/g, ' ').trim();
  };

  const renderMwTable = (entries: MwDictionaryEntryDto[]) => (
    <table className="w-full">
      <tbody>
        {entries.map((entry, index) => (
          <tr
            key={entry.recordId}
            style={{
              borderBottom:
                index < entries.length - 1
                  ? '1px solid var(--surface-d)'
                  : 'none',
            }}
          >
            <td
              style={{
                width: '300px',
                padding: '0.75rem',
                verticalAlign: 'top',
                fontWeight: 'bold',
              }}
            >
              {entry.key1Display}
            </td>
            <td
              style={{
                padding: '0.75rem',
                whiteSpace: 'pre-wrap',
                wordWrap: 'break-word',
              }}
            >
              {stripHtmlTags(entry.rawBody)}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );

  const renderFrisch = (entries: FrischEntryDto[]) => (
    <div className="flex flex-column gap-3">
      {entries.map((entry) => (
        <div key={entry.entry_id} className="card">
          <h3 className="mb-1">
            {entry.lemma_iast}
            {entry.homonym_index != null ? ` (${entry.homonym_index})` : ''}
          </h3>
          {entry.grammar_note && (
            <p className="text-sm text-color-secondary mb-2">{entry.grammar_note}</p>
          )}
          {entry.gloss_ru && <p className="mb-1"><strong>{t('dictionary.gloss')} (ru):</strong> {entry.gloss_ru}</p>}
          {entry.gloss_en && <p className="mb-1"><strong>{t('dictionary.gloss')} (en):</strong> {entry.gloss_en}</p>}
          {entry.gloss_cs && <p className="mb-1"><strong>{t('dictionary.gloss')} (cs):</strong> {entry.gloss_cs}</p>}
          {entry.parent_lemma && (
            <p className="mb-1 text-sm">{t('dictionary.parentLemma')}: {entry.parent_lemma}</p>
          )}
          {entry.senses && entry.senses.length > 0 && (
            <div className="mt-2">
              <strong>{t('dictionary.senses')}:</strong>
              <ul className="ml-3">
                {entry.senses.map((sense, i) => (
                  <li key={i}>
                    {[sense.ru, sense.en, sense.cs].filter(Boolean).join(' / ')}
                    {sense.number_note ? ` (${sense.number_note})` : ''}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      ))}
    </div>
  );

  const renderApte = (entries: ApteEntryDto[]) => (
    <div className="flex flex-column gap-3">
      {entries.map((entry) => (
        <div key={entry.id} className="card">
          <h3 className="mb-2">
            {entry.headwordDevanagari}
            {entry.homonymNum != null ? ` (${entry.homonymNum})` : ''}
          </h3>
          <p className="white-space-pre-wrap" style={{ wordWrap: 'break-word' }}>
            {entry.bodyText || entry.rawMarkup}
          </p>
        </div>
      ))}
    </div>
  );

  return (
    <div className="p-4">
      <h1 className="text-center mb-5">{t('nav.dictionary')}</h1>

      <div className="flex justify-content-center mb-4">
        <div className="p-inputgroup" style={{ maxWidth: '500px' }}>
          <InputText
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder={t('common.search')}
            onKeyPress={(e) => {
              if (e.key === 'Enter') handleSearch();
            }}
          />
          <Button icon="pi pi-search" onClick={handleSearch} />
        </div>
      </div>

      {mwError && <Message severity="error" text={mwError.message} />}
      {frischError && <Message severity="error" text={frischError.message} />}
      {apteError && <Message severity="error" text={apteError.message} />}

      <TabView>
        <TabPanel header="Monier-Williams">
          {isMwSearching && !!query && (
            <div className="flex justify-content-center my-4">
              <ProgressSpinner />
            </div>
          )}
          {mwEntry && mwEntry.entries.length > 0 && (
            <div className="card mt-3">{renderMwTable(mwEntry.entries)}</div>
          )}
          {mwEntry && mwEntry.entries.length === 0 && !isMwSearching && query && (
            <p className="text-center mt-3">{t('dictionary.noWordsFound')}</p>
          )}
          {!query && <p className="text-center mt-3 text-color-secondary">{t('dictionary.enterLemma')}</p>}
        </TabPanel>

        <TabPanel header="Frisch (IAST)">
          {isFrischSearching && !!query && (
            <div className="flex justify-content-center my-4">
              <ProgressSpinner />
            </div>
          )}
          {frischEntries && frischEntries.length > 0 && (
            <div className="mt-3">{renderFrisch(frischEntries)}</div>
          )}
          {frischEntries && frischEntries.length === 0 && !isFrischSearching && query && (
            <p className="text-center mt-3">{t('dictionary.noWordsFound')}</p>
          )}
          {!query && <p className="text-center mt-3 text-color-secondary">{t('dictionary.enterLemma')}</p>}
        </TabPanel>

        <TabPanel header="Apte">
          {isApteSearching && !!query && (
            <div className="flex justify-content-center my-4">
              <ProgressSpinner />
            </div>
          )}
          {apteEntries && apteEntries.length > 0 && (
            <div className="mt-3">{renderApte(apteEntries)}</div>
          )}
          {apteEntries && apteEntries.length === 0 && !isApteSearching && query && (
            <p className="text-center mt-3">{t('dictionary.noWordsFound')}</p>
          )}
          {!query && <p className="text-center mt-3 text-color-secondary">{t('dictionary.enterLemma')}</p>}
        </TabPanel>
      </TabView>
    </div>
  );
};

export default DictionaryPage;

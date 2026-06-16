import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useMwWordSearch, useMwEntry } from '../hooks/useDictionary';

const DictionaryPage = () => {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const query = searchParams.get('q');
  const selectedSlp1 = searchParams.get('slp');

  const [searchTerm, setSearchTerm] = useState(query || '');

  const {
    data: searchResults,
    isPending: isSearching,
    error: searchError,
  } = useMwWordSearch(query);

  const {
    data: entryDetails,
    isFetching: isFetchingEntry,
    error: entryError,
  } = useMwEntry(selectedSlp1);

  const handleSearch = () => {
    if (searchTerm.trim()) {
      navigate(`/dictionary?q=${encodeURIComponent(searchTerm.trim())}`);
    }
  };

  const handleSelectWord = (slp1Normalized: string) => {
    const currentQuery = query || searchTerm;
    navigate(
      `/dictionary?q=${encodeURIComponent(
        currentQuery
      )}&slp=${encodeURIComponent(slp1Normalized)}`
    );
  };

  const stripHtmlTags = (html: string) => {
    if (!html) return '';

    // Создаём временный DOM-элемент
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const text = doc.body.textContent || '';

    // Схлопываем пробелы
    return text.replace(/\s+/g, ' ').trim();
  };

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

      {((isSearching && query) || (isFetchingEntry && selectedSlp1)) && (
        <div className="flex justify-content-center">
          <ProgressSpinner />
        </div>
      )}
      {searchError && <Message severity="error" text={searchError.message} />}
      {entryError && <Message severity="error" text={entryError.message} />}

      {searchResults && searchResults.length > 0 && (
        <div className="flex flex-wrap justify-content-center gap-2 mb-4">
          {searchResults.map((word) => (
            <Button
              key={word.id}
              label={word.slp1Normalized || word.slp1Spelling}
              className={
                word.slp1Normalized === selectedSlp1
                  ? 'p-button-sm'
                  : 'p-button-outlined p-button-sm'
              }
              onClick={() => handleSelectWord(word.slp1Normalized)}
            />
          ))}
        </div>
      )}
      {searchResults && searchResults.length === 0 && !isSearching && query && (
        <p className="text-center">{t('dictionary.noWordsFound')}</p>
      )}

      {entryDetails && (
        <div className="card mt-4">
          <table className="w-full">
            <tbody>
              {entryDetails.entries.map((entry, index) => (
                <tr
                  key={entry.recordId}
                  style={{
                    borderBottom:
                      index < entryDetails.entries.length - 1
                        ? '1px solid var(--surface-d)'
                        : 'none',
                  }}
                >
                  <td
                    style={{
                      width: '150px',
                      padding: '0.75rem',
                      verticalAlign: 'top',
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
        </div>
      )}
    </div>
  );
};

export default DictionaryPage;

import React, { useState, useMemo, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { TabView, TabPanel } from 'primereact/tabview';
import { useDictionaryEntries, useLemmaSearch } from '../../hooks/useDictionary';
import { MwDictionaryEntryDto, FrischEntryDto, ApteEntryDto, CaeEntryDto, LemmaSearchResultDto } from '../../types';

const DictionaryPage = () => {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const query = searchParams.get('q');
  const selected = searchParams.get('s');

  const [searchTerm, setSearchTerm] = useState(query || '');
  const [activeIndex, setActiveIndex] = useState(0);

  const {
    data: lemmaSearchResults,
    isPending: isLemmaSearching,
    error: lemmaSearchError,
  } = useLemmaSearch(query);

  // Deduplicate chips by k1Iast, keep best score
  const chips = useMemo(() => {
    if (!lemmaSearchResults) return [];
    const map = new Map<string, LemmaSearchResultDto>();
    for (const r of lemmaSearchResults) {
      const key = r.k1Iast || r.k1Slp1 || r.k2Original || '';
      if (!key) continue;
      const existing = map.get(key);
      if (!existing || (r.score ?? 0) > (existing.score ?? 0)) {
        map.set(key, r);
      }
    }
    return Array.from(map.values());
  }, [lemmaSearchResults]);

  // Selected lemma IAST value
  const [selectedLemma, setSelectedLemma] = useState<string | null>(selected || null);

  // The LemmaSearchResultDto for the currently selected lemma (matched by key)
  const selectedResult = useMemo(() => {
    if (!selectedLemma || !chips) return null;
    return chips.find((r) => (r.k1Slp1 || r.k2Original) === selectedLemma) || null;
  }, [chips, selectedLemma]);

  // Dictionary codes for which entryIds were actually found for this lemma
  const foundDictionaries = useMemo<string[]>(() => {
    const entries = selectedResult?.entries as Record<string, number[]> | undefined;
    return entries ? Object.keys(entries) : [];
  }, [selectedResult]);

  const allTabs = [
    { code: 'frisch', header: 'Frisch (IAST)' },
    { code: 'cae', header: 'Cappeller' },
    { code: 'apte', header: 'Apte' },
    { code: 'mw', header: 'Monier-Williams' },
  ];
  const availableTabs = allTabs.filter((t) => foundDictionaries.includes(t.code));

  useEffect(() => {
    if (activeIndex >= availableTabs.length) {
      setActiveIndex(0);
    }
  }, [availableTabs.length, activeIndex]);

// Auto-select if only one chip
  useEffect(() => {
    if (chips.length === 1 && !selectedLemma) {
      const auto = chips[0].k1Slp1 || chips[0].k2Original || '';
      if (auto) {
        setSelectedLemma(auto);
        navigate(`/dictionary?q=${encodeURIComponent(query || searchTerm)}&s=${encodeURIComponent(auto)}`, { replace: true });
      }
    }
  }, [chips, selectedLemma]);

  const handleSearch = () => {
    if (searchTerm.trim()) {
      setSelectedLemma(null);
      setActiveIndex(0);
      navigate(`/dictionary?q=${encodeURIComponent(searchTerm.trim())}`);
    }
  };

  const handleChipClick = (r: LemmaSearchResultDto) => {
    const val = r.k1Slp1 || r.k2Original || '';
    if (!val) return;
    setSelectedLemma(val);
    navigate(`/dictionary?q=${encodeURIComponent(query || searchTerm)}&s=${encodeURIComponent(val)}`);
  };

  // Unified dictionary-entries endpoint: load by dictionary code + entry ids.
  // Only the active tab's articles are fetched.
  const activeCode = availableTabs[activeIndex]?.code;
  const activeEntryIds =
    (selectedResult?.entries?.[activeCode ?? ''] as number[] | undefined) ?? [];
  const {
    data: activeEntriesData,
    isPending: isActiveSearching,
    error: activeError,
  } = useDictionaryEntries(
    activeCode && foundDictionaries.includes(activeCode) ? activeCode : null,
    activeEntryIds.length ? activeEntryIds : null,
  );

  const mwEntries =
    (activeCode === 'mw'
      ? (activeEntriesData?.entries as MwDictionaryEntryDto[] | undefined)
      : undefined) ?? [];
  const frischEntries =
    (activeCode === 'frisch'
      ? (activeEntriesData?.entries as FrischEntryDto[] | undefined)
      : undefined) ?? [];
  const apteEntries =
    (activeCode === 'apte'
      ? (activeEntriesData?.entries as ApteEntryDto[] | undefined)
      : undefined) ?? [];

  const caeEntries =
    (activeCode === 'cae'
      ? (activeEntriesData?.entries as CaeEntryDto[] | undefined)
      : undefined) ?? [];

  const stripHtmlTags = (html: string) => {
    if (!html) return '';
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const text = doc.body.textContent || '';
    return text.replace(/\s+/g, ' ').trim();
  };

  const renderMw = (entries: MwDictionaryEntryDto[]) => {
    // One ready HTML article per headword group (first entry of each group
    // carries headword + page references + body html; the rest have null).
    const articles = entries.filter((e) => e.html);
    if (articles.length === 0) {
      return (
        <div className="flex flex-column gap-3">
          {entries.map((entry) => (
            <div key={entry.entryId ?? entry.id ?? entry.recordId} className="card">
              <div style={{ whiteSpace: 'pre-wrap', wordWrap: 'break-word' }}>
                {entry.displayTitle || entry.key2 || entry.key1Display}
                <div className="mt-2">{entry.cleanText || stripHtmlTags(entry.rawBody)}</div>
              </div>
            </div>
          ))}
        </div>
      );
    }
    return (
      <div className="flex flex-column gap-3">
        {articles.map((entry) => (
          <div key={entry.entryId ?? entry.id ?? entry.recordId} className="card">
            <div className="flex justify-content-between align-items-start mb-2">
              <span className="mw-headword sdata_siddhanta">
                {entry.headwordDevanagari}
              </span>
              {entry.pageRefsHtml && (
                <span
                  className="mw-page-refs"
                  style={{ textAlign: 'right', fontSize: '0.85rem', color: 'var(--text-color-secondary)' }}
                  dangerouslySetInnerHTML={{ __html: entry.pageRefsHtml }}
                />
              )}
            </div>
            <div
              className="mw-entry-html"
              style={{ wordWrap: 'break-word' }}
              dangerouslySetInnerHTML={{ __html: entry.html! }}
            />
          </div>
        ))}
      </div>
    );
  };

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
          {entry.html ? (
            <div
              className="apte-entry-html"
              style={{ wordWrap: 'break-word' }}
              dangerouslySetInnerHTML={{ __html: entry.html }}
            />
          ) : (
            <>
              <h3 className="mb-2">
                {entry.headwordDevanagari}
                {entry.homonymNum != null ? ` (${entry.homonymNum})` : ''}
              </h3>
              <p className="white-space-pre-wrap" style={{ wordWrap: 'break-word' }}>
                {entry.bodyText || entry.rawMarkup}
              </p>
            </>
          )}
        </div>
      ))}
    </div>
  );

  const renderCae = (entries: CaeEntryDto[]) => (
    <div className="flex flex-column gap-3">
      {entries.map((entry) => (
        <div key={entry.id} className="card">
          <h3 className="mb-1">
            {entry.headwordAccented}
            {entry.headwordPlain && entry.headwordPlain !== entry.headwordAccented ? (
              <span className="text-sm text-color-secondary"> ({entry.headwordPlain})</span>
            ) : null}
            {entry.homonymNum != null ? ` (${entry.homonymNum})` : ''}
          </h3>
          <div className="flex gap-3 text-sm text-color-secondary mb-2">
            {entry.grammarPos && <span>{entry.grammarPos}</span>}
            {entry.page != null && <span>p. {entry.page}</span>}
          </div>
          {entry.gloss && (
            <p className="mb-1"><strong>{t('dictionary.gloss')}:</strong> {entry.gloss}</p>
          )}
          <p className="white-space-pre-wrap" style={{ wordWrap: 'break-word' }}>
            {entry.cleanText || entry.rawText}
          </p>
        </div>
      ))}
    </div>
  );

  return (
    <div className="p-4">
      <h1 className="text-center mb-5">{t('nav.dictionary')}</h1>

      <div className="flex justify-content-center mb-3">
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

      {/* Chips row */}
      {isLemmaSearching && !!query && (
        <div className="flex justify-content-center mb-3">
          <ProgressSpinner style={{ width: '24px', height: '24px' }} />
        </div>
      )}
      {!isLemmaSearching && chips.length > 0 && (
        <div className="flex flex-wrap justify-content-center gap-2 mb-3">
{chips.map((r) => {
            const label = r.k1Iast || r.k1Slp1 || r.k2Original || '';
            const searchVal = r.k1Slp1 || r.k2Original || '';
            const isActive = selectedLemma === searchVal;
            const deva = r.lemmaDevanagari || '';
            return (
              <div
                key={label}
                onClick={() => handleChipClick(r)}
                style={{
                  cursor: 'pointer',
                  background: isActive ? 'var(--primary-color)' : 'var(--surface-card)',
                  color: isActive ? 'var(--primary-color-text)' : 'var(--text-color)',
                  border: '1px solid var(--surface-border)',
                  borderRadius: '20px',
                  padding: '0',
                }}
              >
                <div style={{ padding: '0.25rem 0.5rem', textAlign: 'center', lineHeight: '1.3' }}>
                  <div style={{ fontWeight: 700 }}>{label}</div>
                  {deva && <div>{deva}</div>}
                </div>
              </div>
            );
          })}
        </div>
      )}
      {!isLemmaSearching && chips.length === 0 && query && (
        <p className="text-center mb-3 text-color-secondary">{t('dictionary.noWordsFound')}</p>
      )}

      {activeError && <Message severity="error" text={activeError.message} />}
      {lemmaSearchError && <Message severity="error" text={lemmaSearchError.message} />}

      <TabView activeIndex={activeIndex} onTabChange={(e) => setActiveIndex(e.index)}>
        {availableTabs.map((tab) => {
          if (tab.code === 'mw') {
            return (
              <TabPanel key="mw" header={t('dictionary.mw')}>
                {activeCode === 'mw' && isActiveSearching && !!selectedLemma && (
                  <div className="flex justify-content-center my-4">
                    <ProgressSpinner />
                  </div>
                )}
                {activeCode === 'mw' && mwEntries.length > 0 && (
                  <div className="mt-3">{renderMw(mwEntries)}</div>
                )}
                {activeCode === 'mw' && mwEntries.length === 0 && !isActiveSearching && selectedLemma && (
                  <p className="text-center mt-3">{t('dictionary.noWordsFound')}</p>
                )}
                {!selectedLemma && <p className="text-center mt-3 text-color-secondary">{t('dictionary.enterLemma')}</p>}
              </TabPanel>
            );
          }
          if (tab.code === 'frisch') {
            return (
              <TabPanel key="frisch" header={t('dictionary.frisch')}>
                {activeCode === 'frisch' && isActiveSearching && !!selectedLemma && (
                  <div className="flex justify-content-center my-4">
                    <ProgressSpinner />
                  </div>
                )}
                {activeCode === 'frisch' && frischEntries.length > 0 && (
                  <div className="mt-3">{renderFrisch(frischEntries)}</div>
                )}
                {activeCode === 'frisch' && frischEntries.length === 0 && !isActiveSearching && selectedLemma && (
                  <p className="text-center mt-3">{t('dictionary.noWordsFound')}</p>
                )}
                {!selectedLemma && <p className="text-center mt-3 text-color-secondary">{t('dictionary.enterLemma')}</p>}
              </TabPanel>
            );
          }
          if (tab.code === 'apte') {
            return (
              <TabPanel key="apte" header={t('dictionary.apte')}>
                {activeCode === 'apte' && isActiveSearching && !!selectedLemma && (
                  <div className="flex justify-content-center my-4">
                    <ProgressSpinner />
                  </div>
                )}
                {activeCode === 'apte' && apteEntries.length > 0 && (
                  <div className="mt-3">{renderApte(apteEntries)}</div>
                )}
                {activeCode === 'apte' && apteEntries.length === 0 && !isActiveSearching && selectedLemma && (
                  <p className="text-center mt-3">{t('dictionary.noWordsFound')}</p>
                )}
                {!selectedLemma && <p className="text-center mt-3 text-color-secondary">{t('dictionary.enterLemma')}</p>}
              </TabPanel>
            );
          }
          if (tab.code === 'cae') {
            return (
              <TabPanel key="cae" header={t('dictionary.cae')}>
                {activeCode === 'cae' && isActiveSearching && !!selectedLemma && (
                  <div className="flex justify-content-center my-4">
                    <ProgressSpinner />
                  </div>
                )}
                {activeCode === 'cae' && caeEntries.length > 0 && (
                  <div className="mt-3">{renderCae(caeEntries)}</div>
                )}
                {activeCode === 'cae' && caeEntries.length === 0 && !isActiveSearching && selectedLemma && (
                  <p className="text-center mt-3">{t('dictionary.noWordsFound')}</p>
                )}
                {!selectedLemma && <p className="text-center mt-3 text-color-secondary">{t('dictionary.enterLemma')}</p>}
              </TabPanel>
            );
          }
          return null;
        })}
      </TabView>
    </div>
  );
};

export default DictionaryPage;
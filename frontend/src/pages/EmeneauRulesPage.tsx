import React, { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import { useSandhiRules } from '../hooks/useContent';
import { useLocation } from 'react-router-dom';
import { OverlayPanel } from 'primereact/overlaypanel';
import { Tooltip } from 'primereact/tooltip';

const EmeneauRulesPage = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const { data: allSandhiRules, isLoading, isError, error } = useSandhiRules();

  const [currentPage, setCurrentPage] = useState(() => {
    const storedPage = localStorage.getItem('emeneauRulesPage');
    return storedPage ? parseInt(storedPage, 10) : 0;
  });
  const rulesPerPage = 10;

  // Filter rules based on URL parameters
  const getFilteredRules = () => {
    const params = new URLSearchParams(location.search);
    const ruleParams = params.getAll('rule'); // Changed to 'rule'

    if (ruleParams.length > 0 && allSandhiRules) {
      const ruleNumbersToFilter = ruleParams.map(Number);
      return allSandhiRules.filter(rule => ruleNumbersToFilter.includes(rule.ruleNumber));
    }
    return allSandhiRules;
  };

  const sandhiRules = getFilteredRules();

  useEffect(() => {
    localStorage.setItem('emeneauRulesPage', currentPage.toString());
  }, [currentPage]);

  const totalRules = sandhiRules?.length || 0;
  const totalPages = Math.ceil(totalRules / rulesPerPage);

  useEffect(() => {
    if (currentPage >= totalPages && totalPages > 0) {
      setCurrentPage(totalPages - 1);
    } else if (currentPage < 0 && totalPages > 0) {
      setCurrentPage(0);
    }
  }, [currentPage, totalPages]);

  if (isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('emeneau.fetchRulesError', { message: error?.message })} />
      </div>
    );
  }

  const startIndex = currentPage * rulesPerPage;
  const endIndex = Math.min(startIndex + rulesPerPage, totalRules);
  const currentRules = sandhiRules?.slice(startIndex, endIndex) || [];

  const pageRange = Array.from({ length: totalPages }, (_, i) => i);

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <h1 className="text-center mb-5">{t('grammar.sandhiRulesTitle')}</h1>
      <div className="w-full" style={{ maxWidth: '800px' }}>
        {totalRules > rulesPerPage && (
          <div className="flex justify-content-center gap-2 mb-4 flex-wrap">
            {pageRange.map((pageIdx) => (
              <Button
                key={pageIdx}
                label={`${pageIdx * rulesPerPage + 1}-${Math.min((pageIdx + 1) * rulesPerPage, totalRules)}`}
                className={`p-button-sm ${currentPage === pageIdx ? 'p-button-primary' : 'p-button-outlined'}`}
                onClick={() => setCurrentPage(pageIdx)}
              />
            ))}
          </div>
        )}

        {currentRules.map((rule) => (
          <Card
            key={rule.id}
            className="mb-4 relative" // Added relative positioning for absolute child
          >
            <div className="p-card-title flex justify-content-between align-items-start">
              <div>
                <span className="text-lg font-bold">{rule.ruleNumber}</span>
                {rule.whitneyNumber && <span className="text-lg font-bold ml-2">({rule.whitneyNumber})</span>}
                {rule.shortDescription && <span className="text-lg font-bold ml-2">{rule.shortDescription}</span>}
              </div>
              {rule.sandhiRuleGroups && rule.sandhiRuleGroups.length > 0 && (
                <div className="flex gap-2 absolute top-0 right-0 p-3"> {/* Positioned top-right */}
                  {rule.sandhiRuleGroups.map((group) => (
                    <React.Fragment key={group.id}>
                      <i
                        className="pi pi-tag text-xl cursor-pointer" // Example icon, adjust as needed
                        data-pr-tooltip={group.description}
                        data-pr-position="top"
                      ></i>
                      <Tooltip target=".pi-tag" />
                    </React.Fragment>
                  ))}
                </div>
              )}
            </div>
            <div className="p-card-content">
              <p className="m-0">{rule.fullText}</p>
              {rule.iastExample && (
                <p className="m-0 pl-4 text-color-secondary font-italic"><br/>{rule.iastExample}</p>
              )}
            </div>
          </Card>
        ))}
        {currentRules.length === 0 && totalRules > 0 && (
          <Message severity="info" text={t('emeneau.noRulesFoundForPage')} />
        )}
        {totalRules === 0 && (
          <Message severity="info" text={t('emeneau.noRulesFound')} />
        )}
      </div>
    </div>
  );
};

export default EmeneauRulesPage;

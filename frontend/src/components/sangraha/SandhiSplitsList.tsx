import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

interface SandhiSplit {
  surface: string;
  components: string[];
  ruleNumbers?: number[];
}

interface SandhiSplitsListProps {
  sandhiSplits: SandhiSplit[];
}

const SandhiSplitsList = ({ sandhiSplits }: SandhiSplitsListProps) => {
  const { t } = useTranslation();

  if (!sandhiSplits || sandhiSplits.length === 0) return null;

  const allRuleNumbers = sandhiSplits
    .flatMap(s => s.ruleNumbers ?? [])
    .filter((v, i, a) => a.indexOf(v) === i);

  return (
    <div className="mb-3">
      <label className="flex justify-content-between align-items-center mb-1 font-semibold">
        <span>{t('sangraha.fields.sandhiSplits')}</span>
        {allRuleNumbers.length > 0 && (
          <Link
            to={`/lessons/grammar/sandhi?${allRuleNumbers.map(r => `rule=${r}`).join('&')}`}
            className="text-sm text-primary"
            target="_blank"
            rel="noopener noreferrer"
          >
            {allRuleNumbers.join(', ')}
          </Link>
        )}
      </label>
      <div className="p-3 border-1 border-round surface-border surface-ground">
        {sandhiSplits.map((s, i) => (
          <div key={i} className="mb-2">
            <span className="font-medium">{s.surface}</span>
            <span className="mx-2">→</span>
            <span>{s.components.join(' + ')}</span>
            {s.ruleNumbers && s.ruleNumbers.length > 0 && (
              <span className="text-sm text-color-secondary ml-2">
                [{s.ruleNumbers.join(', ')}]
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default SandhiSplitsList;


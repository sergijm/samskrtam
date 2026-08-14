import React from "react";
import { useTranslation } from "react-i18next";
import type { EndingsTableData } from "../../data/aStemEndingsTable";

interface DeclensionEndingsReferenceTableProps {
  data: EndingsTableData;
  onCellClick?: (caseKey: string, columnKey: string) => void;
  selectedCell?: { caseKey: string; columnKey: string } | null;
}

const DeclensionEndingsReferenceTable: React.FC<DeclensionEndingsReferenceTableProps> = ({ data, onCellClick, selectedCell }) => {
  const { i18n } = useTranslation();

  const caseLabel = (caseKey: string): string => {
    const mapRu: Record<string, string> = {
      nominative: "Именительный", accusative: "Винительный", instrumental: "Творительный",
      dative: "Дательный", ablative: "Отложительный", genitive: "Родительный",
      locative: "Местный", vocative: "Звательный",
    };
    const mapEn: Record<string, string> = {
      nominative: "Nominative", accusative: "Accusative", instrumental: "Instrumental",
      dative: "Dative", ablative: "Ablative", genitive: "Genitive",
      locative: "Locative", vocative: "Vocative",
    };
    return i18n.language === "ru" ? (mapRu[caseKey] ?? caseKey) : (mapEn[caseKey] ?? caseKey);
  };

  const identityVocativeSg = i18n.language === "ru" ? "= \u043e\u0441\u043d\u043e\u0432\u0435" : "= stem";
  const identityVocativeDuPl = i18n.language === "ru" ? "= Именительный" : "= Nominative";

  return (
    <div className="overflow-x-auto mb-3">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr>
            <th className="text-left p-2 border-1 border-200 font-semibold" style={{ width: "16%" }}>
              {i18n.language === "ru" ? "\u041f\u0430\u0434\u0435\u0436" : "Case"}
            </th>
            {data.columns.map((col) => (
              <th key={col.key} className="text-center p-2 border-1 border-200 font-semibold">
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.rows.map((row) => {
            const isVocative = row.caseKey === "vocative";
            return (
              <tr key={row.caseKey}>
                <td className="p-2 border-1 border-200 text-color-secondary font-medium">
                  {caseLabel(row.caseKey)}
                </td>
                {data.columns.map((col) => {
                  const cell = row.cells[col.key];
                  const cellClick = cell && !cell.isIdentity
                    ? () => onCellClick?.(row.caseKey, col.key)
                    : undefined;
                  const isSelected = !!selectedCell
                    && selectedCell.caseKey === row.caseKey
                    && selectedCell.columnKey === col.key;
                  return renderCell(cell, isVocative, identityVocativeSg, identityVocativeDuPl, col.key, cellClick, isSelected);
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

function renderCell(
  cell: { text: string; rowSpan?: number; isIdentity?: boolean; identityDuPl?: boolean } | undefined,
  isVocative: boolean,
  identitySgLabel: string,
  identityDuPlLabel: string,
  key: string,
  onClick?: () => void,
  isSelected?: boolean,
): React.ReactNode {
  if (!cell) {
    return <td key={key} className="text-center p-2 border-1 border-200 text-color-secondary">{'\u2014'}</td>;
  }
  if (cell.rowSpan === 0) {
    return null;
  }
  const rowSpan = cell.rowSpan && cell.rowSpan > 1 ? cell.rowSpan : undefined;
  if (isVocative && cell.isIdentity) {
    const label = cell.identityDuPl ? identityDuPlLabel : identitySgLabel;
    return (
      <td key={key} className="text-center p-2 border-1 border-200 text-color-secondary italic" rowSpan={rowSpan} style={{ fontSize: "0.85rem" }}>
        {label}
      </td>
    );
  }
  const clickable = Boolean(onClick && cell.text && cell.text !== '\u2014');
  const selectedClass = clickable && isSelected ? " text-primary" : "";
  return (
    <td
      key={key}
      className={`text-center p-2 border-1 border-200 font-bold${clickable ? " cursor-pointer hover:surface-100 transition-colors" : ""}${selectedClass}`}
      rowSpan={rowSpan}
      style={{ fontFamily: "inherit" }}
      onClick={clickable ? onClick : undefined}
    >
      {cell.text}
    </td>
  );
}

export default DeclensionEndingsReferenceTable;
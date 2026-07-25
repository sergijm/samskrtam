import React from "react";
import { useTranslation } from "react-i18next";
import type { EndingsTableData } from "../../data/aStemEndingsTable";

interface DeclensionEndingsReferenceTableProps {
  data: EndingsTableData;
}

const DeclensionEndingsReferenceTable: React.FC<DeclensionEndingsReferenceTableProps> = ({ data }) => {
  const { i18n } = useTranslation();

  const caseLabel = (caseKey: string): string => {
    const mapRu: Record<string, string> = {
      nominative: "\u0418\u043c.", accusative: "\u0412\u0438\u043d.", instrumental: "\u0422\u0432.",
      dative: "\u0414\u0430\u0442.", ablative: "\u041e\u0442\u043b.", genitive: "\u0420\u043e\u0434.",
      locative: "\u041c\u0435\u0441\u0442.", vocative: "\u0417\u0432.",
    };
    const mapEn: Record<string, string> = {
      nominative: "N.", accusative: "A.", instrumental: "I.",
      dative: "D.", ablative: "Abl.", genitive: "G.",
      locative: "L.", vocative: "V.",
    };
    return i18n.language === "ru" ? (mapRu[caseKey] ?? caseKey) : (mapEn[caseKey] ?? caseKey);
  };

  const identityVocativeSg = i18n.language === "ru" ? "= \u043e\u0441\u043d\u043e\u0432\u0435" : "= stem";
  const identityVocativeDuPl = i18n.language === "ru" ? "= N." : "= N.";

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
                  return renderCell(cell, isVocative, identityVocativeSg, identityVocativeDuPl, col.key);
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
  return (
    <td key={key} className="text-center p-2 border-1 border-200 font-bold" rowSpan={rowSpan} style={{ fontFamily: "inherit" }}>
      {cell.text}
    </td>
  );
}

export default DeclensionEndingsReferenceTable;
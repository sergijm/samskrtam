import React from 'react';
import { GRAY, NUMERALS, sectionTitleStyle, cellBase } from './constants';

interface Props {
  isRu: boolean;
}

const NumeralsTable: React.FC<Props> = ({ isRu }) => (
  <table style={{ borderCollapse: 'collapse', width: '100%', marginTop: '1.5rem' }}>
    <tbody>
      <tr><td colSpan={NUMERALS.length} style={sectionTitleStyle}>{isRu ? 'Цифры' : 'Numerals'}</td></tr>
      <tr>
        {NUMERALS.map((n) => (
          <td key={n.word} style={cellBase}>
            <div style={{ fontSize: '1.3rem', color: n.faint ? GRAY : undefined }}>{n.dev}</div>
            <div style={{ fontSize: '0.72rem', color: n.faint ? GRAY : '#444' }}>{n.word}</div>
            <div style={{ fontSize: '0.8rem', fontWeight: 700 }}>{n.value}</div>
          </td>
        ))}
      </tr>
    </tbody>
  </table>
);

export default NumeralsTable;

import React from 'react';
import Glyph from './Glyph';
import { RED, VARGAS, sectionTitleStyle, subHeaderStyle, cellBase, devStyle, iastStyle } from './constants';
import type { Varga } from './constants';

interface Props {
  isRu: boolean;
}

const ConsonantsTable: React.FC<Props> = ({ isRu }) => (
  <table style={{ borderCollapse: 'collapse' }}>
    <tbody>
      <tr><td colSpan={9} style={sectionTitleStyle}>{isRu ? 'Согласные' : 'Consonants'}</td></tr>
      <tr>
        <td style={subHeaderStyle}>{isRu ? 'непридых.' : 'unasp.'}</td>
        <td style={subHeaderStyle}>{isRu ? 'придых.' : 'asp.'}</td>
        <td style={subHeaderStyle}>{isRu ? 'непридых.' : 'unasp.'}</td>
        <td style={subHeaderStyle}>{isRu ? 'придых.' : 'asp.'}</td>
        <td style={subHeaderStyle}>{isRu ? 'носовые' : 'nasal'}</td>
        <td style={subHeaderStyle}></td>
        <td style={subHeaderStyle}>{isRu ? 'полугласн.' : 'semivowels'}</td>
        <td style={subHeaderStyle}>{isRu ? 'шипящие' : 'sibilants'}</td>
        <td style={subHeaderStyle}>{isRu ? 'придых.' : 'aspirate'}</td>
      </tr>
      {VARGAS.map((v: Varga, i: number) => (
        <tr key={v.titleEn}>
          <Glyph dev={v.letters[0]} iast={v.iast[0]} />
          <Glyph dev={v.letters[1]} iast={v.iast[1]} />
          <Glyph dev={v.letters[2]} iast={v.iast[2]} color={RED} />
          <Glyph dev={v.letters[3]} iast={v.iast[3]} color={RED} />
          <Glyph dev={v.letters[4]} iast={v.iast[4]} color={RED} />
          <td style={{ ...cellBase, textAlign: 'left', fontSize: '0.7rem', maxWidth: '100px' }}
              title={isRu ? v.descRu : v.descEn}>
            <b style={{  wordBreak: 'break-word' }}>{isRu ? v.titleRu : v.titleEn}</b><br/>
            <span style={{ color: RED, fontWeight: 700 }}>{v.iastLabel}</span>
          </td>
          {v.semivowel
            ? <Glyph dev={v.semivowel.dev} iast={v.semivowel.iast} color={RED} />
            : <td style={cellBase} />}
          {v.sibilant
            ? <Glyph dev={v.sibilant.dev} iast={v.sibilant.iast} />
            : <td style={cellBase} />}
          {i === 0 && (
            <td style={cellBase} rowSpan={VARGAS.length}>
              <div style={devStyle}>ह</div>
              <div style={iastStyle}>ha</div>
            </td>
          )}
        </tr>
      ))}
    </tbody>
  </table>
);

export default ConsonantsTable;


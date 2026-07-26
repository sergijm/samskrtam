import React from 'react';
import Glyph from './Glyph';
import { RED, GRAY, CONJUNCT_EXAMPLES, sectionTitleStyle, subHeaderStyle, cellBase } from './constants';

interface Props {
  isRu: boolean;
}

const WeakenedConsonantsTable: React.FC<Props> = ({ isRu }) => (
  <table style={{ borderCollapse: 'collapse' }}>
    <tbody>
      <tr><td colSpan={2} style={sectionTitleStyle}>{isRu ? 'Ослабленные согл.' : 'Weakened consonants'}</td></tr>
      <tr>
        <td style={subHeaderStyle}>{isRu ? 'анусвара' : 'anusvāra'}</td>
        <td style={subHeaderStyle}>{isRu ? 'висарга' : 'visarga'}</td>
      </tr>
      <tr>
        <Glyph dev="अं" iast="aṃ" color={RED} />
        <Glyph dev="अः" iast="aḥ" color={RED} />
      </tr>
      {Array.from({ length: 3 }).map((_, row) => (
        <tr key={`conj-${row}`}>
          <Glyph dev={CONJUNCT_EXAMPLES[row * 3].dev} iast={CONJUNCT_EXAMPLES[row * 3].iast} color={GRAY} />
          <Glyph dev={CONJUNCT_EXAMPLES[row * 3 + 1].dev} iast={CONJUNCT_EXAMPLES[row * 3 + 1].iast} color={GRAY} />
        </tr>
      ))}
      <tr>
        <td colSpan={2} style={{ padding: '10px 6px 0' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              <tr><td colSpan={1} style={sectionTitleStyle}>{isRu ? 'Звуки по цветам' : 'Sounds by colour'}</td></tr>
              <tr><td style={{ ...cellBase, textAlign: 'left', fontWeight: 700, color: RED }}>
                {isRu ? 'Звонкие' : 'Voiced'}
              </td></tr>
              <tr><td style={{ ...cellBase, textAlign: 'left', fontWeight: 700 }}>
                {isRu ? 'Глухие' : 'Voiceless'}
              </td></tr>
            </tbody>
          </table>
        </td>
      </tr>
    </tbody>
  </table>
);

export default WeakenedConsonantsTable;

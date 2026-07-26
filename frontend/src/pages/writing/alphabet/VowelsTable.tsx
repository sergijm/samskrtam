import React from 'react';
import Glyph from './Glyph';
import { RED, GRAY, sectionTitleStyle, subHeaderStyle, cellBase } from './constants';

interface Props {
  isRu: boolean;
}

const VowelsTable: React.FC<Props> = ({ isRu }) => (
  <table style={{ borderCollapse: 'collapse' }}>
    <tbody>
      <tr><td colSpan={4} style={sectionTitleStyle}>{isRu ? 'Гласные' : 'Vowels'}</td></tr>
      <tr>
        <td colSpan={2} style={subHeaderStyle}>{isRu ? 'простые' : 'simple'}</td>
        <td colSpan={2} style={subHeaderStyle}>{isRu ? 'дифтонги' : 'diphthongs'}</td>
      </tr>
      <tr>
        <Glyph dev="अ" iast="a" color={RED} />
        <Glyph dev="आ" iast="ā" color={RED} />
        <td style={cellBase} /><td style={cellBase} />
      </tr>
      <tr>
        <Glyph dev="इ" iast="i" color={RED} />
        <Glyph dev="ई" iast="ī" color={RED} />
        <Glyph dev="ए" iast="e" color={RED} />
        <Glyph dev="ऐ" iast="ai" color={RED} />
      </tr>
      <tr>
        <Glyph dev="ऋ" iast="r̥" color={RED} />
        <Glyph dev="ॠ" iast="r̥̄" color={RED} />
        <td style={cellBase} /><td style={cellBase} />
      </tr>
      <tr>
        <Glyph dev="ऌ" iast="l̥" color={RED} />
        <Glyph dev="ॡ" iast="l̥̄" color={GRAY} />
        <td style={cellBase} /><td style={cellBase} />
      </tr>
      <tr>
        <Glyph dev="उ" iast="u" color={RED} />
        <Glyph dev="ऊ" iast="ū" color={RED} />
        <Glyph dev="ओ" iast="o" color={RED} />
        <Glyph dev="औ" iast="au" color={RED} />
      </tr>
    </tbody>
  </table>
);

export default VowelsTable;

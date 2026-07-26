import React from 'react';
import { cellBase, devStyle, iastStyle } from './constants';

export interface GlyphProps {
  dev: string;
  iast: string;
  color?: string;
}

const Glyph: React.FC<GlyphProps> = ({ dev, iast, color }) => (
  <td style={cellBase}>
    <div style={{ ...devStyle, color }}>{dev}</div>
    <div style={iastStyle}>{iast}</div>
  </td>
);

export default Glyph;

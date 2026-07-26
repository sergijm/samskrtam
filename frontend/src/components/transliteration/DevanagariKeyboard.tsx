import React, { useRef, useCallback } from 'react';
import Keyboard from 'react-simple-keyboard';
import 'react-simple-keyboard/build/css/index.css';
import './DevanagariKeyboard.css';
import { devanagariLayout } from '../../data/devanagariKeyboardLayout';

interface DevanagariKeyboardProps {
  /** The target textarea to insert characters into */
  inputRef: React.RefObject<HTMLTextAreaElement | null>;
  /** Called with updated text after each key press */
  onTextChange: (text: string) => void;
}

/**
 * Virtual on-screen Devanagari keyboard.
 *
 * Wraps react-simple-keyboard (MIT) with a custom Devanagari layout.
 * On button press, inserts the Unicode glyph at the cursor position
 * of the target textarea.
 *
 * Styling: overrides react-simple-keyboard default CSS via
 * a project-scoped class (.devanagari-kbd) to fit PrimeReact theme.
 */
const DevanagariKeyboard: React.FC<DevanagariKeyboardProps> = ({
  inputRef,
  onTextChange,
}) => {
  const keyboardRef = useRef<any>(null);

  const handleKeyPress = useCallback(
    (button: string) => {
      const textarea = inputRef.current;
      if (!textarea) return;

      const start = textarea.selectionStart ?? 0;
      const end = textarea.selectionEnd ?? 0;
      const current = textarea.value;

      const newText =
        current.slice(0, start) + button + current.slice(end);

      onTextChange(newText);

      // Restore cursor position after React re-render via setTimeout
      setTimeout(() => {
        textarea.focus();
        const newPos = start + button.length;
        textarea.setSelectionRange(newPos, newPos);
      }, 0);
    },
    [inputRef, onTextChange],
  );

  return (
    <div className="devanagari-kbd mt-3">
      <Keyboard
        keyboardRef={(r: any) => (keyboardRef.current = r)}
        layout={devanagariLayout}
        onKeyPress={handleKeyPress}
        display={{
          // The keys ARE the display labels already — no mapping needed
        }}
      />
    </div>
  );
};

export default DevanagariKeyboard;


import { useEffect, type ReactNode } from 'react';
import { Icon } from './Icon';

/**
 * title = Überschrift, onClose = Schließen-Funktion der Seite, children = Inhalt (meist ein Formular).
 */
interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/** Einfaches Dialogfenster. Schließt mit Escape, Klick auf den Hintergrund oder das X. */
export function Modal({ title, onClose, children }: ModalProps) {
  // Escape-Taste schließt den Dialog; der Listener wird beim Schließen wieder entfernt
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-header">
          <h2>{title}</h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Schließen">
            <Icon name="close" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

import { useCallback, useMemo, useState, type ReactNode } from 'react';
import { Modal } from '../components/Modal';
import { DialogContext, type ConfirmOptions, type DialogContextValue } from './dialog-context';

/** Der gerade angezeigte Dialog (höchstens einer gleichzeitig). */
type OpenDialog =
  | { kind: 'confirm'; options: ConfirmOptions; resolve: (confirmed: boolean) => void }
  | { kind: 'error'; message: string };

/**
 * Stellt eigene Dialoge (Sicherheitsabfrage, Fehlermeldung) für die ganze App bereit.
 * confirm() gibt ein Promise zurück, das erst beim Klick auf einen Button erfüllt wird – so lässt sich
 * im Code wie bei window.confirm() schreiben: if (!(await dialogs.confirm(...))) return;
 * Die Dialoge nutzen das gleiche Modal wie die Formulare (Escape, Klick daneben und X schließen).
 */
export function DialogProvider({ children }: { children: ReactNode }) {
  const [dialog, setDialog] = useState<OpenDialog | null>(null);

  const confirm = useCallback(
    (options: ConfirmOptions) => new Promise<boolean>((resolve) => setDialog({ kind: 'confirm', options, resolve })),
    [],
  );

  const showError = useCallback((message: string) => setDialog({ kind: 'error', message }), []);

  // Dialog schließen; eine offene Sicherheitsabfrage gilt dabei als abgebrochen bzw. bestätigt
  const close = useCallback(
    (confirmed: boolean) => {
      if (dialog?.kind === 'confirm') dialog.resolve(confirmed);
      setDialog(null);
    },
    [dialog],
  );

  const cancel = useCallback(() => close(false), [close]);

  // Wert des Contexts – bleibt gleich, damit die Seiten nicht unnötig neu rendern
  const value = useMemo<DialogContextValue>(() => ({ confirm, showError }), [confirm, showError]);

  return (
    <DialogContext.Provider value={value}>
      {children}
      {dialog?.kind === 'confirm' && (
        <Modal title={dialog.options.title} onClose={cancel}>
          <p className="dialog-message">{dialog.options.message}</p>
          <div className="form-actions">
            {/* Fokus auf "Abbrechen": Enter löscht also nicht versehentlich */}
            <button type="button" className="button secondary" onClick={cancel} autoFocus>
              Abbrechen
            </button>
            <button
              type="button"
              className={dialog.options.danger ? 'button danger' : 'button'}
              onClick={() => close(true)}
            >
              {dialog.options.confirmLabel ?? 'OK'}
            </button>
          </div>
        </Modal>
      )}
      {dialog?.kind === 'error' && (
        <Modal title="Das hat nicht geklappt" onClose={cancel}>
          <div className="alert" role="alert">
            {dialog.message}
          </div>
          <div className="form-actions">
            <button type="button" className="button" onClick={cancel} autoFocus>
              OK
            </button>
          </div>
        </Modal>
      )}
    </DialogContext.Provider>
  );
}

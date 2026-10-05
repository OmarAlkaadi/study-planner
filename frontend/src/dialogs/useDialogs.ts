import { useContext } from 'react';
import { DialogContext, type DialogContextValue } from './dialog-context';

/**
 * Hook für eigene Dialoge, z. B.:
 *   const dialogs = useDialogs();
 *   if (!(await dialogs.confirm({ title: 'Löschen?', message: '…', danger: true }))) return;
 * Wirft einen Fehler, wenn er außerhalb von <DialogProvider> benutzt wird.
 */
export function useDialogs(): DialogContextValue {
  const context = useContext(DialogContext);
  if (!context) {
    throw new Error('useDialogs muss innerhalb von <DialogProvider> verwendet werden');
  }
  return context;
}

import { useContext } from 'react';
import { AuthContext, type AuthContextValue } from './auth-context';

/**
 * Hook, mit dem jede Komponente an den Login-Zustand kommt, z. B. const { user, logout } = useAuth();
 * Wirft einen Fehler, wenn er außerhalb von <AuthProvider> benutzt wird.
 */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth muss innerhalb von <AuthProvider> verwendet werden');
  }
  return context;
}

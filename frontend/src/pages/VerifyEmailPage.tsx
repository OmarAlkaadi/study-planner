import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { authApi } from '../api/api';
import { errorMessage } from '../api/client';
import { AuthCard } from '../components/AuthCard';
import { ErrorMessage, Loading } from '../components/ui';

/**
 * Seite "E-Mail bestätigen" (/verify-email?token=..., öffentlich).
 * Wird über den Link in der Bestätigungsmail geöffnet und löst das Token sofort ein
 * (POST /api/auth/verify-email). Angemeldet wird dabei NICHT: Nach Erfolg geht es zur Login-Seite,
 * die den Hinweis "E-Mail bestätigt" anzeigt (Meldung über den Router-State).
 */
export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(token ? null : 'In diesem Link fehlt das Token.');
  // Ein Token funktioniert nur einmal. React ruft Effekte im Entwicklungsmodus (StrictMode) doppelt auf –
  // die Ref verhindert, dass das Token dabei zweimal eingelöst wird.
  const started = useRef(false);

  useEffect(() => {
    if (!token || started.current) return;
    started.current = true;
    authApi
      .verifyEmail(token)
      .then(({ message }) => navigate('/login', { replace: true, state: { message } }))
      .catch((err) => setError(errorMessage(err)));
  }, [token, navigate]);

  return (
    <AuthCard title="E-Mail bestätigen">
      {error ? (
        <>
          <ErrorMessage message={error} />
          <p className="hint">
            Melde dich an – ist deine E-Mail noch nicht bestätigt, kannst du dort einen neuen Link anfordern.
          </p>
          <p className="auth-switch">
            <Link to="/login">Zum Login</Link>
          </p>
        </>
      ) : (
        <Loading />
      )}
    </AuthCard>
  );
}

import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation } from 'react-router';
import { authApi } from '../api/api';
import { ApiError, errorMessage } from '../api/client';
import { useAuth } from '../auth/useAuth';
import { AuthCard } from '../components/AuthCard';
import { ErrorMessage, Field } from '../components/ui';

/** Fehlercode des Backends, wenn das Passwort stimmt, die E-Mail aber noch nicht bestätigt ist. */
const EMAIL_NOT_VERIFIED = 'EMAIL_NOT_VERIFIED';

/**
 * Login- und Registrierungsseite (gleiches Layout, unterschiedliche Felder).
 *
 * mode kommt aus der Route in App.tsx.
 * - Registrieren: legt das Konto an und zeigt danach den Hinweis "Bestätige deine E-Mail" – angemeldet wird
 *   man erst über den Link in der Mail (Seite VerifyEmailPage).
 * - Login: ist die E-Mail noch nicht bestätigt, erscheint ein Button "Bestätigungsmail erneut senden".
 *   Nach erfolgreichem Login leitet <Navigate> zum Dashboard weiter.
 * Eine Erfolgsmeldung von einer anderen Seite (z. B. "Passwort geändert") kommt über den Router-State.
 */
export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { user, login, register } = useAuth();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);
  // Meldung nach erfolgreicher Registrierung ("Wir haben dir eine E-Mail geschickt …")
  const [registeredMessage, setRegisteredMessage] = useState<string | null>(null);
  // true, wenn der Login wegen fehlender E-Mail-Bestätigung abgelehnt wurde
  const [needsVerification, setNeedsVerification] = useState(false);
  const [resendMessage, setResendMessage] = useState<string | null>(null);

  if (user) {
    return <Navigate to="/" replace />;
  }

  const isRegister = mode === 'register';
  const notice = (location.state as { message?: string } | null)?.message ?? null;

  // Formular absenden; Fehler vom Backend erscheinen oben bzw. unter dem jeweiligen Feld
  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    setNeedsVerification(false);
    setResendMessage(null);
    try {
      if (isRegister) {
        setRegisteredMessage(await register(email.trim(), password, displayName.trim()));
        setSubmitting(false);
      } else {
        await login(email.trim(), password);
        // Nach erfolgreichem Login leitet das <Navigate> oben automatisch weiter
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldErrors);
        setNeedsVerification(err.code === EMAIL_NOT_VERIFIED);
      }
      setError(errorMessage(err));
      setSubmitting(false);
    }
  };

  // Bestätigungsmail erneut anfordern (nach der Registrierung oder beim Login ohne Bestätigung)
  const resendVerification = async () => {
    setResendMessage(null);
    try {
      setResendMessage((await authApi.resendVerification(email.trim())).message);
    } catch (err) {
      setResendMessage(errorMessage(err));
    }
  };

  // Nach der Registrierung: nur noch der Hinweis auf die Bestätigungsmail
  if (registeredMessage) {
    return (
      <AuthCard title="Bestätige deine E-Mail" subtitle="Nur noch ein Schritt bis zu deinem Konto.">
        <div className="alert success" role="status">
          {registeredMessage}
        </div>
        <p className="hint">Keine Mail bekommen? Schau im Spam-Ordner nach oder fordere sie erneut an.</p>
        <button type="button" className="button secondary full" onClick={resendVerification}>
          Bestätigungsmail erneut senden
        </button>
        {resendMessage && <p className="hint">{resendMessage}</p>}
        <p className="auth-switch">
          Schon bestätigt? <Link to="/login">Anmelden</Link>
        </p>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title={isRegister ? 'Konto erstellen' : 'Willkommen zurück'}
      subtitle={
        isRegister
          ? 'Plane Vorlesungen, Klausuren und Aufgaben an einem Ort.'
          : 'Melde dich an, um dein Semester im Blick zu behalten.'
      }
    >
      <form className="form" onSubmit={onSubmit}>
        {notice && !error && (
          <div className="alert success" role="status">
            {notice}
          </div>
        )}
        <ErrorMessage message={error} />
        {needsVerification && (
          <>
            <button type="button" className="button secondary full" onClick={resendVerification}>
              Bestätigungsmail erneut senden
            </button>
            {resendMessage && <p className="hint">{resendMessage}</p>}
          </>
        )}
        {isRegister && (
          <Field label="Name" error={fieldErrors.displayName}>
            <input
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              required
              maxLength={100}
              autoComplete="name"
            />
          </Field>
        )}
        <Field label="E-Mail" error={fieldErrors.email}>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
          />
        </Field>
        <Field label="Passwort" error={fieldErrors.password}>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={isRegister ? 8 : undefined}
            maxLength={72}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
          />
        </Field>
        {isRegister ? (
          <p className="hint">Mindestens 8 Zeichen. Du bekommst danach eine E-Mail zur Bestätigung.</p>
        ) : (
          <p className="auth-forgot">
            <Link to="/forgot-password">Passwort vergessen?</Link>
          </p>
        )}
        <button type="submit" className="button full" disabled={submitting}>
          {submitting ? 'Bitte warten …' : isRegister ? 'Registrieren' : 'Anmelden'}
        </button>
      </form>

      <p className="auth-switch">
        {isRegister ? (
          <>
            Schon ein Konto? <Link to="/login">Anmelden</Link>
          </>
        ) : (
          <>
            Noch kein Konto? <Link to="/register">Registrieren</Link>
          </>
        )}
      </p>
    </AuthCard>
  );
}

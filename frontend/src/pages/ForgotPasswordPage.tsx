import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { authApi } from '../api/api';
import { ApiError, errorMessage } from '../api/client';
import { AuthCard } from '../components/AuthCard';
import { ErrorMessage, Field } from '../components/ui';

/**
 * Seite "Passwort vergessen" (/forgot-password, öffentlich).
 * Schickt POST /api/auth/forgot-password. Das Backend antwortet immer gleich – egal ob es die Adresse gibt –
 * damit niemand so herausfinden kann, welche E-Mail-Adressen registriert sind.
 */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // Absenden: Meldung vom Backend anzeigen, Formular ausblenden
  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      setMessage((await authApi.forgotPassword(email.trim())).message);
    } catch (err) {
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard
      title="Passwort vergessen?"
      subtitle="Gib deine E-Mail-Adresse ein. Wir schicken dir einen Link, mit dem du ein neues Passwort festlegen kannst."
    >
      {message ? (
        <div className="alert success" role="status">
          {message}
        </div>
      ) : (
        <form className="form" onSubmit={onSubmit}>
          <ErrorMessage message={error} />
          <Field label="E-Mail" error={fieldErrors.email}>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              autoFocus
            />
          </Field>
          <button type="submit" className="button full" disabled={submitting}>
            {submitting ? 'Bitte warten …' : 'Link senden'}
          </button>
        </form>
      )}
      <p className="auth-switch">
        <Link to="/login">Zurück zum Login</Link>
      </p>
    </AuthCard>
  );
}

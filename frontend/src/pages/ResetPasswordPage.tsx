import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { authApi } from '../api/api';
import { ApiError, errorMessage } from '../api/client';
import { AuthCard } from '../components/AuthCard';
import { ErrorMessage, Field } from '../components/ui';

/**
 * Seite "Neues Passwort festlegen" (/reset-password?token=..., öffentlich).
 * Wird über den Link in der Mail "Passwort zurücksetzen" geöffnet. Nach Erfolg geht es zum Login,
 * die Erfolgsmeldung wird über den Router-State mitgegeben und dort angezeigt.
 */
export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  // Absenden: beide Eingaben müssen gleich sein, dann neues Passwort setzen und zum Login wechseln
  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (password !== confirm) {
      setFieldErrors({ confirm: 'Die Passwörter stimmen nicht überein.' });
      return;
    }
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    try {
      const { message } = await authApi.resetPassword(token, password);
      navigate('/login', { replace: true, state: { message } });
    } catch (err) {
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
      setError(errorMessage(err));
      setSubmitting(false);
    }
  };

  if (!token) {
    return (
      <AuthCard title="Link unvollständig">
        <ErrorMessage message="In diesem Link fehlt das Token. Öffne den Link bitte direkt aus der E-Mail." />
        <p className="auth-switch">
          <Link to="/forgot-password">Neuen Link anfordern</Link>
        </p>
      </AuthCard>
    );
  }

  return (
    <AuthCard title="Neues Passwort festlegen" subtitle="Der Link ist 60 Minuten gültig und funktioniert nur einmal.">
      <form className="form" onSubmit={onSubmit}>
        <ErrorMessage message={error} />
        <Field label="Neues Passwort" error={fieldErrors.password}>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={72}
            autoComplete="new-password"
            autoFocus
          />
        </Field>
        <Field label="Passwort wiederholen" error={fieldErrors.confirm}>
          <input
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
            autoComplete="new-password"
          />
        </Field>
        <p className="hint">Mindestens 8 Zeichen.</p>
        <button type="submit" className="button full" disabled={submitting}>
          {submitting ? 'Bitte warten …' : 'Passwort speichern'}
        </button>
      </form>
      <p className="auth-switch">
        <Link to="/forgot-password">Neuen Link anfordern</Link>
      </p>
    </AuthCard>
  );
}

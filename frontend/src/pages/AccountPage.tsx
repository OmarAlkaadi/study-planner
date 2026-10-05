import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { accountApi } from '../api/api';
import { ApiError, errorMessage } from '../api/client';
import { useAuth } from '../auth/useAuth';
import { ErrorMessage, Field, PageHeader } from '../components/ui';
import { useDialogs } from '../dialogs/useDialogs';

/**
 * Seite "Konto" (/account): Anzeigenamen ändern, Passwort ändern und Konto löschen.
 * Jeder Bereich ist ein eigenes kleines Formular mit eigener Erfolgs-/Fehlermeldung.
 * Beim öffentlichen Demo-Account sind alle Formulare gesperrt (das Backend lehnt Änderungen ohnehin ab).
 */
export function AccountPage() {
  const { user } = useAuth();
  return (
    <>
      <PageHeader title="Konto" subtitle={user?.email} />
      {user?.demo && (
        <div className="alert info">
          Das ist der öffentliche Demo-Account – Name, Passwort und Konto sind gesperrt. Registriere dich für ein
          eigenes Konto, um alles auszuprobieren. Kurse, Klausuren und Aufgaben der Demo werden jede Nacht zurückgesetzt.
        </div>
      )}
      {/* disabled sperrt alle Eingabefelder und Buttons darin auf einmal */}
      <fieldset className="account-grid" disabled={user?.demo}>
        <ProfileSection />
        <PasswordSection />
        <DeleteSection />
      </fieldset>
    </>
  );
}

/** Anzeigenamen ändern (PUT /api/account/profile); der neue Name erscheint sofort oben rechts. */
function ProfileSection() {
  const { user, updateUser } = useAuth();
  const [displayName, setDisplayName] = useState(user?.displayName ?? '');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setMessage(null);
    setError(null);
    setFieldErrors({});
    try {
      updateUser(await accountApi.updateProfile(displayName.trim()));
      setMessage('Name gespeichert.');
    } catch (err) {
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card">
      <h2>Profil</h2>
      <form className="form" onSubmit={onSubmit}>
        {message && <div className="alert success">{message}</div>}
        <ErrorMessage message={error} />
        <Field label="E-Mail">
          <input value={user?.email ?? ''} disabled />
        </Field>
        <Field label="Name" error={fieldErrors.displayName}>
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required maxLength={100} />
        </Field>
        <div className="form-actions">
          <button type="submit" className="button" disabled={saving}>
            {saving ? 'Speichert …' : 'Name speichern'}
          </button>
        </div>
      </form>
    </section>
  );
}

/** Passwort ändern (PUT /api/account/password) – das aktuelle Passwort muss zur Sicherheit angegeben werden. */
function PasswordSection() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setMessage(null);
    setError(null);
    if (newPassword !== confirm) {
      setFieldErrors({ confirm: 'Die Passwörter stimmen nicht überein.' });
      return;
    }
    setSaving(true);
    setFieldErrors({});
    try {
      await accountApi.changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirm('');
      // Das Backend beendet alle anderen Sitzungen und gibt diesem Gerät ein neues Refresh-Cookie
      setMessage('Passwort geändert. Auf allen anderen Geräten wurdest du abgemeldet.');
    } catch (err) {
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="card">
      <h2>Passwort ändern</h2>
      <form className="form" onSubmit={onSubmit}>
        {message && <div className="alert success">{message}</div>}
        <ErrorMessage message={error} />
        <Field label="Aktuelles Passwort" error={fieldErrors.currentPassword}>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </Field>
        <div className="form-row">
          <Field label="Neues Passwort" error={fieldErrors.newPassword}>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
            />
          </Field>
          <Field label="Wiederholen" error={fieldErrors.confirm}>
            <input
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
              autoComplete="new-password"
            />
          </Field>
        </div>
        <div className="form-actions">
          <button type="submit" className="button" disabled={saving}>
            {saving ? 'Speichert …' : 'Passwort ändern'}
          </button>
        </div>
      </form>
    </section>
  );
}

/**
 * Konto löschen (DELETE /api/account) – mit Passwort und zusätzlicher Sicherheitsabfrage.
 * Danach wird abgemeldet und der Login mit einer Bestätigung angezeigt.
 */
function DeleteSection() {
  // Eigene Dialoge statt der Browser-Fenster (siehe DialogProvider)
  const dialogs = useDialogs();
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const confirmed = await dialogs.confirm({
      title: 'Konto endgültig löschen?',
      message: 'Alle Kurse, Klausuren, Aufgaben und Lerneinheiten gehen verloren. Das lässt sich nicht rückgängig machen.',
      confirmLabel: 'Konto löschen',
      danger: true,
    });
    if (!confirmed) return;
    setDeleting(true);
    setError(null);
    try {
      await accountApi.deleteAccount(password);
      // Erst zur Login-Seite wechseln, dann abmelden – so kommt die Meldung dort an
      navigate('/login', { replace: true, state: { message: 'Dein Konto wurde gelöscht.' } });
      logout();
    } catch (err) {
      setError(errorMessage(err));
      setDeleting(false);
    }
  };

  return (
    <section className="card danger-zone">
      <h2>Konto löschen</h2>
      <p className="muted small">
        Löscht dein Konto und alle deine Daten endgültig. Das lässt sich nicht rückgängig machen.
      </p>
      <form className="form" onSubmit={onSubmit}>
        <ErrorMessage message={error} />
        <Field label="Passwort zur Bestätigung">
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </Field>
        <div className="form-actions">
          <button type="submit" className="button danger" disabled={deleting}>
            {deleting ? 'Löscht …' : 'Konto endgültig löschen'}
          </button>
        </div>
      </form>
    </section>
  );
}

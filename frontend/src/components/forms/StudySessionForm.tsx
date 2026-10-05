import { useState, type FormEvent } from 'react';
import { studySessionApi } from '../../api/api';
import { ApiError, errorMessage } from '../../api/client';
import type { Course, StudySession, StudySessionInput } from '../../api/types';
import { todayIso } from '../../utils/format';
import { ErrorMessage, Field } from '../ui';

/** Vorschläge für die Dauer (Minuten) als Schnellauswahl-Buttons. */
const QUICK_DURATIONS = [25, 45, 60, 90];

/**
 * Zustand des Formulars. Alle Eingaben sind Text (so liefern sie die Eingabefelder);
 * in Zahlen/null umgewandelt wird erst beim Absenden.
 */
interface FormState {
  courseId: string;
  sessionDate: string;
  durationMinutes: string;
  topic: string;
}

/** Startwerte: beim Bearbeiten die Werte der Lerneinheit, beim Anlegen sinnvolle Vorgaben. */
function initialState(session: StudySession | null, courses: Course[]): FormState {
  return {
    courseId: session ? String(session.courseId) : courses.length > 0 ? String(courses[0].id) : '',
    sessionDate: session?.sessionDate ?? todayIso(),
    durationMinutes: session ? String(session.durationMinutes) : '45',
    topic: session?.topic ?? '',
  };
}

/**
 * session = null → neu anlegen, sonst bearbeiten.
 * onSaved/onCancel kommen von der Seite (Dialog schließen, Liste neu laden).
 */
interface Props {
  session: StudySession | null;
  courses: Course[];
  onSaved: () => void;
  onCancel: () => void;
}

/**
 * Formular zum Anlegen und Bearbeiten einer Lerneinheit (wird in einem Modal auf der Seite angezeigt).
 */
export function StudySessionForm({ session, courses, onSaved, onCancel }: Props) {
  const [form, setForm] = useState<FormState>(() => initialState(session, courses));
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Ändert ein einzelnes Feld im Formularzustand (typsicher über den Feldnamen)
  const update = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  // Absenden: Eingaben ins API-Format umwandeln und je nach Fall create oder update aufrufen.
  // Fehler vom Backend: fieldErrors erscheinen unter den Feldern, die Meldung oben im Formular.
  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const input: StudySessionInput = {
      courseId: Number(form.courseId),
      sessionDate: form.sessionDate,
      durationMinutes: Number(form.durationMinutes),
      topic: form.topic.trim() || null,
    };

    setSaving(true);
    setError(null);
    setFieldErrors({});
    try {
      if (session) {
        await studySessionApi.update(session.id, input);
      } else {
        await studySessionApi.create(input);
      }
      onSaved();
    } catch (err) {
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors);
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="form" onSubmit={onSubmit}>
      <ErrorMessage message={error} />
      <div className="form-row">
        <Field label="Kurs *" error={fieldErrors.courseId}>
          <select value={form.courseId} onChange={(e) => update('courseId', e.target.value)} required>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Datum *" error={fieldErrors.sessionDate}>
          <input
            type="date"
            value={form.sessionDate}
            onChange={(e) => update('sessionDate', e.target.value)}
            required
          />
        </Field>
      </div>
      <Field label="Dauer in Minuten *" error={fieldErrors.durationMinutes}>
        <input
          type="number"
          min={1}
          max={1440}
          step={1}
          value={form.durationMinutes}
          onChange={(e) => update('durationMinutes', e.target.value)}
          required
        />
      </Field>
      <div className="chips">
        {QUICK_DURATIONS.map((minutes) => (
          <button
            key={minutes}
            type="button"
            className={String(minutes) === form.durationMinutes ? 'chip active' : 'chip'}
            onClick={() => update('durationMinutes', String(minutes))}
          >
            {minutes} min
          </button>
        ))}
      </div>
      <Field label="Thema" error={fieldErrors.topic}>
        <input
          value={form.topic}
          onChange={(e) => update('topic', e.target.value)}
          maxLength={200}
          placeholder="z. B. Integralrechnung"
        />
      </Field>
      <div className="form-actions">
        <button type="button" className="button secondary" onClick={onCancel}>
          Abbrechen
        </button>
        <button type="submit" className="button" disabled={saving}>
          {saving ? 'Speichert …' : 'Speichern'}
        </button>
      </div>
    </form>
  );
}

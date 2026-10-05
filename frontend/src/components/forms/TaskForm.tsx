import { useState, type FormEvent } from 'react';
import { taskApi } from '../../api/api';
import { ApiError, errorMessage } from '../../api/client';
import type { Course, Task, TaskInput, TaskPriority, TaskStatus } from '../../api/types';
import { PRIORITY_LABELS, STATUS_LABELS, todayIso } from '../../utils/format';
import { ErrorMessage, Field } from '../ui';

/**
 * Zustand des Formulars. Alle Eingaben sind Text (so liefern sie die Eingabefelder);
 * in Zahlen/null umgewandelt wird erst beim Absenden.
 */
interface FormState {
  title: string;
  description: string;
  courseId: string;
  dueDate: string;
  priority: TaskPriority;
  status: TaskStatus;
}

/** Startwerte: beim Bearbeiten die Werte der Aufgabe, beim Anlegen sinnvolle Vorgaben. */
function initialState(task: Task | null): FormState {
  return {
    title: task?.title ?? '',
    description: task?.description ?? '',
    courseId: task?.courseId != null ? String(task.courseId) : '',
    dueDate: task?.dueDate ?? todayIso(),
    priority: task?.priority ?? 'MEDIUM',
    status: task?.status ?? 'OPEN',
  };
}

/**
 * task = null → neu anlegen, sonst bearbeiten.
 * onSaved/onCancel kommen von der Seite (Dialog schließen, Liste neu laden).
 */
interface Props {
  task: Task | null;
  courses: Course[];
  onSaved: () => void;
  onCancel: () => void;
}

/** Formular zum Anlegen und Bearbeiten einer Aufgabe (wird in einem Modal auf der Seite angezeigt). */
export function TaskForm({ task, courses, onSaved, onCancel }: Props) {
  const [form, setForm] = useState<FormState>(() => initialState(task));
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
    const input: TaskInput = {
      title: form.title.trim(),
      description: form.description.trim() || null,
      courseId: form.courseId === '' ? null : Number(form.courseId),
      dueDate: form.dueDate,
      priority: form.priority,
      status: form.status,
    };

    setSaving(true);
    setError(null);
    setFieldErrors({});
    try {
      if (task) {
        await taskApi.update(task.id, input);
      } else {
        await taskApi.create(input);
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
      <Field label="Titel *" error={fieldErrors.title}>
        <input
          value={form.title}
          onChange={(e) => update('title', e.target.value)}
          required
          maxLength={150}
          autoFocus
          placeholder="z. B. Übungsblatt 5 abgeben"
        />
      </Field>
      <Field label="Beschreibung" error={fieldErrors.description}>
        <textarea
          value={form.description}
          onChange={(e) => update('description', e.target.value)}
          rows={3}
          maxLength={2000}
        />
      </Field>
      <div className="form-row">
        <Field label="Kurs" error={fieldErrors.courseId}>
          <select value={form.courseId} onChange={(e) => update('courseId', e.target.value)}>
            <option value="">– kein Kurs –</option>
            {courses.map((course) => (
              <option key={course.id} value={course.id}>
                {course.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Deadline *" error={fieldErrors.dueDate}>
          <input type="date" value={form.dueDate} onChange={(e) => update('dueDate', e.target.value)} required />
        </Field>
      </div>
      <div className="form-row">
        <Field label="Priorität" error={fieldErrors.priority}>
          <select value={form.priority} onChange={(e) => update('priority', e.target.value as TaskPriority)}>
            {Object.entries(PRIORITY_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Status" error={fieldErrors.status}>
          <select value={form.status} onChange={(e) => update('status', e.target.value as TaskStatus)}>
            {Object.entries(STATUS_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </Field>
      </div>
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

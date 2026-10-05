import { useState, type FormEvent } from 'react';
import { examApi } from '../../api/api';
import { ApiError, errorMessage } from '../../api/client';
import type { Course, Exam, ExamInput } from '../../api/types';
import { ErrorMessage, Field } from '../ui';

/**
 * Zustand des Formulars. Alle Eingaben sind Text (so liefern sie die Eingabefelder);
 * in Zahlen/null umgewandelt wird erst beim Absenden.
 */
interface FormState {
  courseId: string;
  title: string;
  examDate: string;
  room: string;
  notes: string;
  grade: string;
}

/** Startwerte: beim Bearbeiten die Werte der Klausur, beim Anlegen sinnvolle Vorgaben. */
function initialState(exam: Exam | null, courses: Course[]): FormState {
  return {
    courseId: exam ? String(exam.courseId) : courses.length > 0 ? String(courses[0].id) : '',
    title: exam?.title ?? '',
    // Backend: "2026-10-05T09:00:00" -> Eingabefeld: "2026-10-05T09:00"
    examDate: exam ? exam.examDate.slice(0, 16) : '',
    room: exam?.room ?? '',
    notes: exam?.notes ?? '',
    grade: exam?.grade != null ? String(exam.grade) : '',
  };
}

/**
 * exam = null → neu anlegen, sonst bearbeiten.
 * onSaved/onCancel kommen von der Seite (Dialog schließen, Liste neu laden).
 */
interface Props {
  exam: Exam | null;
  courses: Course[];
  onSaved: () => void;
  onCancel: () => void;
}

/** Formular zum Anlegen und Bearbeiten einer Klausur (wird in einem Modal auf der Seite angezeigt). */
export function ExamForm({ exam, courses, onSaved, onCancel }: Props) {
  const [form, setForm] = useState<FormState>(() => initialState(exam, courses));
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
    const input: ExamInput = {
      courseId: Number(form.courseId),
      title: form.title.trim(),
      examDate: form.examDate,
      room: form.room.trim() || null,
      notes: form.notes.trim() || null,
      grade: form.grade === '' ? null : Number(form.grade.replace(',', '.')),
    };

    setSaving(true);
    setError(null);
    setFieldErrors({});
    try {
      if (exam) {
        await examApi.update(exam.id, input);
      } else {
        await examApi.create(input);
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
      <Field label="Kurs *" error={fieldErrors.courseId}>
        <select value={form.courseId} onChange={(e) => update('courseId', e.target.value)} required>
          {courses.map((course) => (
            <option key={course.id} value={course.id}>
              {course.name}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Titel *" error={fieldErrors.title}>
        <input
          value={form.title}
          onChange={(e) => update('title', e.target.value)}
          required
          maxLength={150}
          autoFocus
          placeholder="z. B. Klausur Mathematik 1"
        />
      </Field>
      <div className="form-row">
        <Field label="Datum & Uhrzeit *" error={fieldErrors.examDate}>
          <input
            type="datetime-local"
            value={form.examDate}
            onChange={(e) => update('examDate', e.target.value)}
            required
          />
        </Field>
        <Field label="Raum" error={fieldErrors.room}>
          <input value={form.room} onChange={(e) => update('room', e.target.value)} maxLength={50} />
        </Field>
      </div>
      <Field label="Note (1,0 – 5,0)" error={fieldErrors.grade}>
        <input
          type="number"
          min={1}
          max={5}
          step={0.1}
          value={form.grade}
          onChange={(e) => update('grade', e.target.value)}
          placeholder="noch keine Note"
        />
      </Field>
      <Field label="Notizen" error={fieldErrors.notes}>
        <textarea value={form.notes} onChange={(e) => update('notes', e.target.value)} rows={3} maxLength={1000} />
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

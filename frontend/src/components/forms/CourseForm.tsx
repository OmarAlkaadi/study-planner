import { useState, type FormEvent } from 'react';
import { courseApi } from '../../api/api';
import { ApiError, errorMessage } from '../../api/client';
import type { Course, CourseInput, CourseType, DayOfWeek } from '../../api/types';
import {
  COURSE_COLORS,
  COURSE_TYPE_LABELS,
  DAYS,
  DAY_LABELS,
  DEFAULT_COURSE_COLOR,
  formatTime,
} from '../../utils/format';
import { ErrorMessage, Field } from '../ui';

/**
 * Zustand des Formulars. Alle Eingaben sind Text (so liefern sie die Eingabefelder);
 * in Zahlen/null umgewandelt wird erst beim Absenden.
 */
interface FormState {
  name: string;
  lecturer: string;
  room: string;
  courseType: CourseType;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  ects: string;
  color: string;
  semesterStart: string;
  semesterEnd: string;
}

/** Startwerte: beim Bearbeiten die Werte des Kurses, beim Anlegen sinnvolle Vorgaben. */
function initialState(course: Course | null): FormState {
  return {
    name: course?.name ?? '',
    lecturer: course?.lecturer ?? '',
    room: course?.room ?? '',
    courseType: course?.courseType ?? 'VORLESUNG',
    dayOfWeek: course?.dayOfWeek ?? 'MONDAY',
    startTime: course ? formatTime(course.startTime) : '08:15',
    endTime: course ? formatTime(course.endTime) : '09:45',
    ects: course?.ects != null ? String(course.ects) : '',
    color: course?.color ?? DEFAULT_COURSE_COLOR,
    semesterStart: course?.semesterStart ?? '',
    semesterEnd: course?.semesterEnd ?? '',
  };
}

/**
 * course = null → neu anlegen, sonst bearbeiten.
 * onSaved/onCancel kommen von der Seite (Dialog schließen, Liste neu laden).
 */
interface Props {
  course: Course | null;
  onSaved: () => void;
  onCancel: () => void;
}

/** Formular zum Anlegen und Bearbeiten eines Kurses (wird in einem Modal auf der Seite angezeigt). */
export function CourseForm({ course, onSaved, onCancel }: Props) {
  const [form, setForm] = useState<FormState>(() => initialState(course));
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
    // Gleiche Prüfung wie im Backend – zeigt den Fehler sofort am Feld, ohne Anfrage an den Server
    if (form.endTime <= form.startTime) {
      setFieldErrors({ endTime: 'Die Endzeit muss nach der Startzeit liegen.' });
      return;
    }
    // Datumswerte "2026-10-12" lassen sich als Text vergleichen
    if (form.semesterStart && form.semesterEnd && form.semesterEnd < form.semesterStart) {
      setFieldErrors({ semesterEnd: 'Das Semesterende darf nicht vor dem Semesterbeginn liegen.' });
      return;
    }
    const input: CourseInput = {
      name: form.name.trim(),
      lecturer: form.lecturer.trim() || null,
      room: form.room.trim() || null,
      courseType: form.courseType,
      dayOfWeek: form.dayOfWeek,
      startTime: form.startTime,
      endTime: form.endTime,
      ects: form.ects === '' ? null : Number(form.ects),
      color: form.color,
      semesterStart: form.semesterStart || null,
      semesterEnd: form.semesterEnd || null,
    };

    setSaving(true);
    setError(null);
    setFieldErrors({});
    try {
      if (course) {
        await courseApi.update(course.id, input);
      } else {
        await courseApi.create(input);
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
      <Field label="Name *" error={fieldErrors.name}>
        <input
          value={form.name}
          onChange={(e) => update('name', e.target.value)}
          required
          maxLength={150}
          autoFocus
          placeholder="z. B. Mathematik 1"
        />
      </Field>
      <div className="form-row">
        <Field label="Typ" error={fieldErrors.courseType}>
          <select value={form.courseType} onChange={(e) => update('courseType', e.target.value as CourseType)}>
            {Object.entries(COURSE_TYPE_LABELS).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Wochentag" error={fieldErrors.dayOfWeek}>
          <select value={form.dayOfWeek} onChange={(e) => update('dayOfWeek', e.target.value as DayOfWeek)}>
            {DAYS.map((day) => (
              <option key={day} value={day}>
                {DAY_LABELS[day]}
              </option>
            ))}
          </select>
        </Field>
      </div>
      <div className="form-row">
        <Field label="Beginn *" error={fieldErrors.startTime}>
          <input type="time" value={form.startTime} onChange={(e) => update('startTime', e.target.value)} required />
        </Field>
        <Field label="Ende *" error={fieldErrors.endTime}>
          <input type="time" value={form.endTime} onChange={(e) => update('endTime', e.target.value)} required />
        </Field>
      </div>
      {/* Optionaler Semesterzeitraum: außerhalb davon erscheint der Kurs nicht im Kalender */}
      <div className="form-row">
        <Field label="Semesterbeginn" error={fieldErrors.semesterStart}>
          <input type="date" value={form.semesterStart} onChange={(e) => update('semesterStart', e.target.value)} />
        </Field>
        <Field label="Semesterende" error={fieldErrors.semesterEnd}>
          <input type="date" value={form.semesterEnd} onChange={(e) => update('semesterEnd', e.target.value)} />
        </Field>
      </div>
      <div className="form-row">
        <Field label="Dozent/in" error={fieldErrors.lecturer}>
          <input value={form.lecturer} onChange={(e) => update('lecturer', e.target.value)} maxLength={150} />
        </Field>
        <Field label="Raum" error={fieldErrors.room}>
          <input value={form.room} onChange={(e) => update('room', e.target.value)} maxLength={50} />
        </Field>
      </div>
      <div className="form-row">
        <Field label="ECTS" error={fieldErrors.ects}>
          <input
            type="number"
            min={0}
            max={30}
            step={1}
            value={form.ects}
            onChange={(e) => update('ects', e.target.value)}
          />
        </Field>
        <div className="field">
          <span className="field-label">Farbe</span>
          <div className="color-picker">
            {COURSE_COLORS.map((color) => (
              <button
                key={color}
                type="button"
                className={color === form.color ? 'color-swatch selected' : 'color-swatch'}
                style={{ background: color }}
                onClick={() => update('color', color)}
                aria-label={`Farbe ${color}`}
                aria-pressed={color === form.color}
              />
            ))}
          </div>
        </div>
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

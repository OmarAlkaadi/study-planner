import { useCallback, useState } from 'react';
import { courseApi } from '../api/api';
import { errorMessage } from '../api/client';
import type { Course } from '../api/types';
import { CourseForm } from '../components/forms/CourseForm';
import { Icon } from '../components/Icon';
import { Modal } from '../components/Modal';
import { ColorDot, EmptyState, ErrorMessage, Loading, PageHeader } from '../components/ui';
import { COURSE_TYPE_LABELS, DAYS, DAY_LABELS, formatPeriod, formatTime } from '../utils/format';
import { useApiData, useDebouncedValue } from '../utils/hooks';
import { useDialogs } from '../dialogs/useDialogs';

/** null = kein Dialog offen; { course: null } = neuer Kurs; { course } = diesen Kurs bearbeiten. */
type Editing = { course: Course | null } | null;

/**
 * Seite "Vorlesungen & Veranstaltungen": Stundenplan als Wochenansicht, Tabelle aller Kurse,
 * Suche und Dialog zum Anlegen/Bearbeiten.
 * Die Suche ist entprellt (useDebouncedValue), damit nicht jeder Tastendruck eine Anfrage an
 * GET /api/courses?q= auslöst.
 */
export function CoursesPage() {
  // Eigene Dialoge statt der Browser-Fenster (siehe DialogProvider)
  const dialogs = useDialogs();
  const [search, setSearch] = useState('');
  const query = useDebouncedValue(search.trim());
  const loader = useCallback(() => courseApi.list(query), [query]);
  const { data: courses, error, loading, reload } = useApiData(loader);
  const [editing, setEditing] = useState<Editing>(null);

  // Löschen mit Sicherheitsabfrage, danach Liste neu laden
  const onDelete = async (course: Course) => {
    const confirmed = await dialogs.confirm({
      title: 'Kurs löschen?',
      message: `„${course.name}“ wirklich löschen? Zugehörige Klausuren und Lerneinheiten werden ebenfalls gelöscht.`,
      confirmLabel: 'Löschen',
      danger: true,
    });
    if (!confirmed) return;
    try {
      await courseApi.remove(course.id);
      await reload();
    } catch (err) {
      dialogs.showError(errorMessage(err));
    }
  };

  // Nach dem Speichern im Formular: Dialog schließen und Liste neu laden
  const onSaved = () => {
    setEditing(null);
    void reload();
  };

  // Mo–Fr immer anzeigen, Sa/So nur wenn dort Kurse liegen
  const visibleDays = DAYS.filter(
    (day, index) => index < 5 || (courses ?? []).some((course) => course.dayOfWeek === day),
  );

  return (
    <>
      <PageHeader
        title="Vorlesungen & Veranstaltungen"
        subtitle="Dein wöchentlicher Stundenplan"
        actions={
          <button type="button" className="button" onClick={() => setEditing({ course: null })}>
            <Icon name="plus" /> Neuer Kurs
          </button>
        }
      />

      <div className="toolbar">
        <input
          type="search"
          className="toolbar-search"
          placeholder="Nach Name oder Dozent suchen …"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Kurse durchsuchen"
        />
      </div>

      <ErrorMessage message={error} />
      {!courses && loading && <Loading />}

      {courses && courses.length === 0 && (
        <EmptyState
          title={query ? 'Keine Treffer' : 'Noch keine Kurse'}
          text={query ? `Kein Kurs passt zu „${query}“.` : 'Lege deine erste Vorlesung an, um loszulegen.'}
        />
      )}

      {courses && courses.length > 0 && (
        <>
          {/* Stundenplan: eine Spalte pro Wochentag, Klick auf einen Kurs öffnet ihn zum Bearbeiten */}
          <section className="timetable" style={{ gridTemplateColumns: `repeat(${visibleDays.length}, minmax(140px, 1fr))` }}>
            {visibleDays.map((day) => (
              <div key={day} className="timetable-day">
                <h3>{DAY_LABELS[day]}</h3>
                {courses
                  .filter((course) => course.dayOfWeek === day)
                  .map((course) => (
                    <button
                      type="button"
                      key={course.id}
                      className="timetable-entry"
                      style={{ borderLeftColor: course.color ?? 'var(--primary)' }}
                      onClick={() => setEditing({ course })}
                    >
                      <span className="small muted">
                        {formatTime(course.startTime)}–{formatTime(course.endTime)}
                      </span>
                      <strong>{course.name}</strong>
                      {course.room && <span className="small muted">{course.room}</span>}
                    </button>
                  ))}
              </div>
            ))}
          </section>

          {/* Tabelle aller Kurse mit Bearbeiten/Löschen */}
          <section className="card">
            <h2>Alle Kurse</h2>
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Typ</th>
                    <th>Termin</th>
                    <th>Dozent/in</th>
                    <th>Raum</th>
                    <th>ECTS</th>
                    <th>Semester</th>
                    <th aria-label="Aktionen" />
                  </tr>
                </thead>
                <tbody>
                  {courses.map((course) => (
                    <tr key={course.id}>
                      <td>
                        <span className="with-dot">
                          <ColorDot color={course.color} />
                          {course.name}
                        </span>
                      </td>
                      <td>{COURSE_TYPE_LABELS[course.courseType]}</td>
                      <td>
                        {DAY_LABELS[course.dayOfWeek]}, {formatTime(course.startTime)}–{formatTime(course.endTime)}
                      </td>
                      <td>{course.lecturer ?? '–'}</td>
                      <td>{course.room ?? '–'}</td>
                      <td>{course.ects ?? '–'}</td>
                      <td className="nowrap">{formatPeriod(course.semesterStart, course.semesterEnd)}</td>
                      <td className="row-actions">
                        <button
                          type="button"
                          className="icon-button"
                          onClick={() => setEditing({ course })}
                          aria-label={`${course.name} bearbeiten`}
                        >
                          <Icon name="edit" />
                        </button>
                        <button
                          type="button"
                          className="icon-button danger"
                          onClick={() => onDelete(course)}
                          aria-label={`${course.name} löschen`}
                        >
                          <Icon name="trash" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}

      {/* Dialog zum Anlegen/Bearbeiten – nur sichtbar, wenn editing gesetzt ist */}
      {editing && (
        <Modal title={editing.course ? 'Kurs bearbeiten' : 'Neuer Kurs'} onClose={() => setEditing(null)}>
          <CourseForm course={editing.course} onSaved={onSaved} onCancel={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}

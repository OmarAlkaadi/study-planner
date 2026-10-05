import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router';
import { courseApi, examApi } from '../api/api';
import { errorMessage } from '../api/client';
import type { Exam } from '../api/types';
import { ExamForm } from '../components/forms/ExamForm';
import { Icon } from '../components/Icon';
import { Modal } from '../components/Modal';
import { ColorDot, EmptyState, ErrorMessage, Loading, PageHeader } from '../components/ui';
import { daysUntil, formatDateTime, formatGrade, parseDateTime, relativeDays } from '../utils/format';
import { useApiData, useDebouncedValue } from '../utils/hooks';
import { useDialogs } from '../dialogs/useDialogs';

/**
 * Zeitfilter der Klausurliste; wird als upcoming=true/false bzw. ohne Parameter an das Backend geschickt.
 */
type TimeFilter = 'upcoming' | 'past' | 'all';

/** Buttons des Zeitfilters. */
const TIME_FILTERS: { value: TimeFilter; label: string }[] = [
  { value: 'upcoming', label: 'Anstehend' },
  { value: 'past', label: 'Vergangen' },
  { value: 'all', label: 'Alle' },
];

/**
 * Seite "Klausuren": Karten mit Countdown bzw. Note, Filter (Zeitraum, Kurs, Suche) und Dialog
 * zum Anlegen/Bearbeiten. Lädt zusätzlich die Kurse – für den Kursfilter und die Auswahl im Formular
 * (eine Klausur gehört immer zu einem Kurs).
 */
export function ExamsPage() {
  // Eigene Dialoge statt der Browser-Fenster (siehe DialogProvider)
  const dialogs = useDialogs();
  const [timeFilter, setTimeFilter] = useState<TimeFilter>('upcoming');
  const [courseId, setCourseId] = useState('');
  const [search, setSearch] = useState('');
  const query = useDebouncedValue(search.trim());
  const [editing, setEditing] = useState<{ exam: Exam | null } | null>(null);

  const coursesLoader = useCallback(() => courseApi.list(), []);
  const { data: courses } = useApiData(coursesLoader);

  // Ändert sich ein Filter, entsteht ein neuer loader und die Liste wird automatisch neu geladen
  const examsLoader = useCallback(
    () =>
      examApi.list({
        courseId: courseId === '' ? null : Number(courseId),
        upcoming: timeFilter === 'all' ? null : timeFilter === 'upcoming',
        q: query,
      }),
    [courseId, timeFilter, query],
  );
  const { data: exams, error, loading, reload } = useApiData(examsLoader);
  // Aktueller Zeitpunkt – entscheidet, ob eine Klausur als vergangen gilt. Wird jede Minute aktualisiert,
  // damit eine gerade begonnene Klausur auch ohne Neuladen der Seite ihre Note statt des Countdowns zeigt.
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 60_000);
    return () => clearInterval(timer);
  }, []);

  // Löschen mit Sicherheitsabfrage, danach Liste neu laden
  const onDelete = async (exam: Exam) => {
    const confirmed = await dialogs.confirm({
      title: 'Klausur löschen?',
      message: `„${exam.title}“ wirklich löschen?`,
      confirmLabel: 'Löschen',
      danger: true,
    });
    if (!confirmed) return;
    try {
      await examApi.remove(exam.id);
      await reload();
    } catch (err) {
      dialogs.showError(errorMessage(err));
    }
  };

  // Nach dem Speichern: Dialog schließen und Liste neu laden
  const onSaved = () => {
    setEditing(null);
    void reload();
  };

  // Ohne Kurs kann keine Klausur angelegt werden → Button deaktivieren und Hinweis anzeigen
  const hasCourses = (courses?.length ?? 0) > 0;

  return (
    <>
      <PageHeader
        title="Klausuren"
        subtitle="Prüfungstermine und Noten"
        actions={
          <button
            type="button"
            className="button"
            onClick={() => setEditing({ exam: null })}
            disabled={!hasCourses}
            title={hasCourses ? undefined : 'Lege zuerst einen Kurs an'}
          >
            <Icon name="plus" /> Neue Klausur
          </button>
        }
      />

      {courses && !hasCourses && (
        <div className="alert info">
          Klausuren gehören immer zu einem Kurs. <Link to="/courses">Lege zuerst einen Kurs an.</Link>
        </div>
      )}

      {/* Filterleiste: Zeitraum, Kurs, Suchtext */}
      <div className="toolbar">
        <div className="segmented" role="group" aria-label="Zeitraum">
          {TIME_FILTERS.map((filter) => (
            <button
              key={filter.value}
              type="button"
              className={timeFilter === filter.value ? 'active' : ''}
              onClick={() => setTimeFilter(filter.value)}
            >
              {filter.label}
            </button>
          ))}
        </div>
        <select value={courseId} onChange={(e) => setCourseId(e.target.value)} aria-label="Nach Kurs filtern">
          <option value="">Alle Kurse</option>
          {courses?.map((course) => (
            <option key={course.id} value={course.id}>
              {course.name}
            </option>
          ))}
        </select>
        <input
          type="search"
          className="toolbar-search"
          placeholder="Suchen …"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Klausuren durchsuchen"
        />
      </div>

      <ErrorMessage message={error} />
      {!exams && loading && <Loading />}

      {exams && exams.length === 0 && <EmptyState title="Keine Klausuren gefunden" />}

      {/* Eine Karte pro Klausur: vergangene zeigen die Note, anstehende den Countdown */}
      {exams && exams.length > 0 && (
        <div className="exam-grid">
          {exams.map((exam) => {
            const days = daysUntil(exam.examDate);
            const isPast = parseDateTime(exam.examDate) < now;
            return (
              <article key={exam.id} className="card exam-card" style={{ borderTopColor: exam.courseColor ?? 'var(--primary)' }}>
                <div className="exam-card-head">
                  <span className="with-dot small muted">
                    <ColorDot color={exam.courseColor} />
                    {exam.courseName}
                  </span>
                  <div className="row-actions">
                    <button
                      type="button"
                      className="icon-button"
                      onClick={() => setEditing({ exam })}
                      aria-label={`${exam.title} bearbeiten`}
                    >
                      <Icon name="edit" />
                    </button>
                    <button
                      type="button"
                      className="icon-button danger"
                      onClick={() => onDelete(exam)}
                      aria-label={`${exam.title} löschen`}
                    >
                      <Icon name="trash" />
                    </button>
                  </div>
                </div>
                <h3>{exam.title}</h3>
                <p className="with-icon small">
                  <Icon name="clock" size={15} /> {formatDateTime(exam.examDate)}
                </p>
                {exam.room && (
                  <p className="with-icon small">
                    <Icon name="pin" size={15} /> {exam.room}
                  </p>
                )}
                {exam.notes && <p className="small muted notes">{exam.notes}</p>}
                <div className="exam-card-foot">
                  {isPast ? (
                    <span className="badge">Note: {formatGrade(exam.grade)}</span>
                  ) : (
                    <span className={days <= 7 ? 'badge warning' : 'badge'}>{relativeDays(exam.examDate)}</span>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}

      {editing && courses && (
        <Modal title={editing.exam ? 'Klausur bearbeiten' : 'Neue Klausur'} onClose={() => setEditing(null)}>
          <ExamForm exam={editing.exam} courses={courses} onSaved={onSaved} onCancel={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}

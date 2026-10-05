import { useCallback, useState } from 'react';
import { Link } from 'react-router';
import { courseApi, dashboardApi, studySessionApi } from '../api/api';
import { errorMessage } from '../api/client';
import type { StudySession } from '../api/types';
import { CourseMinutesChart, DailyBarChart } from '../components/Charts';
import { StudySessionForm } from '../components/forms/StudySessionForm';
import { Icon } from '../components/Icon';
import { Modal } from '../components/Modal';
import { ColorDot, EmptyState, ErrorMessage, Loading, PageHeader, ProgressBar } from '../components/ui';
import { formatDate, formatMinutes } from '../utils/format';
import { useApiData } from '../utils/hooks';
import { useDialogs } from '../dialogs/useDialogs';

/**
 * Seite "Lernfortschritt": Diagramme zur Lernzeit, Aufgabenfortschritt pro Kurs und Tabelle aller
 * Lerneinheiten. Lädt drei Dinge: Kurse (Filter/Formular), Statistiken aus GET /api/dashboard/progress
 * (Diagramme) und die Lerneinheiten (Tabelle, optional nach Kurs gefiltert).
 */
export function ProgressPage() {
  // Eigene Dialoge statt der Browser-Fenster (siehe DialogProvider)
  const dialogs = useDialogs();
  const [courseId, setCourseId] = useState('');
  const [editing, setEditing] = useState<{ session: StudySession | null } | null>(null);

  const coursesLoader = useCallback(() => courseApi.list(), []);
  const { data: courses } = useApiData(coursesLoader);

  // Die Statistiken (Summen pro Kurs/Tag) berechnet das Backend – über einen eigenen, schlanken Endpunkt,
  // der nur die Lern-Statistiken liefert (nicht das komplette Dashboard)
  const statsLoader = useCallback(() => dashboardApi.progress(), []);
  const { data: stats, reload: reloadStats } = useApiData(statsLoader);

  const sessionsLoader = useCallback(
    () => studySessionApi.list(courseId === '' ? null : Number(courseId)),
    [courseId],
  );
  const { data: sessions, error, loading, reload: reloadSessions } = useApiData(sessionsLoader);

  // Nach Änderungen Tabelle und Statistiken neu laden
  const reloadAll = () => {
    void reloadSessions();
    void reloadStats();
  };

  // Löschen mit Sicherheitsabfrage
  const onDelete = async (session: StudySession) => {
    const confirmed = await dialogs.confirm({
      title: 'Lerneinheit löschen?',
      message: 'Diese Lerneinheit wirklich löschen?',
      confirmLabel: 'Löschen',
      danger: true,
    });
    if (!confirmed) return;
    try {
      await studySessionApi.remove(session.id);
      reloadAll();
    } catch (err) {
      dialogs.showError(errorMessage(err));
    }
  };

  // Nach dem Speichern: Dialog schließen und alles neu laden
  const onSaved = () => {
    setEditing(null);
    reloadAll();
  };

  // hasCourses: ohne Kurs keine Lerneinheit möglich; totalMinutes: gesamte Lernzeit für die Überschrift
  const hasCourses = (courses?.length ?? 0) > 0;
  const totalMinutes = stats?.minutesPerCourse.reduce((sum, entry) => sum + entry.minutes, 0) ?? 0;

  return (
    <>
      <PageHeader
        title="Lernfortschritt"
        subtitle="Trage ein, wann du wie lange für welches Fach gelernt hast"
        actions={
          <button
            type="button"
            className="button"
            onClick={() => setEditing({ session: null })}
            disabled={!hasCourses}
            title={hasCourses ? undefined : 'Lege zuerst einen Kurs an'}
          >
            <Icon name="plus" /> Lerneinheit eintragen
          </button>
        }
      />

      {courses && !hasCourses && (
        <div className="alert info">
          Lerneinheiten gehören zu einem Kurs. <Link to="/courses">Lege zuerst einen Kurs an.</Link>
        </div>
      )}

      {/* Statistiken und Diagramme (aus GET /api/dashboard/progress) */}
      {stats && (
        <div className="dashboard-grid two">
          <section className="card">
            <h2>Letzte 7 Tage · {formatMinutes(stats.studyMinutesLast7Days)}</h2>
            <DailyBarChart data={stats.dailyMinutes} />
          </section>
          <section className="card">
            <h2>Gesamt pro Kurs · {formatMinutes(totalMinutes)}</h2>
            {stats.minutesPerCourse.length === 0 ? (
              <EmptyState title="Noch keine Lerneinheiten" />
            ) : (
              <CourseMinutesChart data={stats.minutesPerCourse} />
            )}
          </section>
          <section className="card">
            <h2>Aufgaben erledigt pro Kurs</h2>
            {stats.taskProgress.length === 0 ? (
              <EmptyState title="Noch keine Aufgaben mit Kurs" />
            ) : (
              <ul className="hbar-list">
                {stats.taskProgress.map((entry) => (
                  <li key={entry.courseId}>
                    <div className="hbar-head">
                      <span className="with-dot">
                        <ColorDot color={entry.color} />
                        {entry.courseName}
                      </span>
                      <span className="muted">
                        {entry.done}/{entry.total} ({Math.round((entry.done / entry.total) * 100)} %)
                      </span>
                    </div>
                    <ProgressBar value={entry.done} max={entry.total} color={entry.color} />
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}

      {/* Tabelle der Lerneinheiten mit Kursfilter */}
      <section className="card">
        <div className="card-head">
          <h2>Lerneinheiten</h2>
          <select value={courseId} onChange={(e) => setCourseId(e.target.value)} aria-label="Nach Kurs filtern">
            <option value="">Alle Kurse</option>
            {courses?.map((course) => (
              <option key={course.id} value={course.id}>
                {course.name}
              </option>
            ))}
          </select>
        </div>
        <ErrorMessage message={error} />
        {!sessions && loading && <Loading />}
        {sessions && sessions.length === 0 && <EmptyState title="Keine Lerneinheiten gefunden" />}
        {sessions && sessions.length > 0 && (
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr>
                  <th>Datum</th>
                  <th>Kurs</th>
                  <th>Thema</th>
                  <th>Dauer</th>
                  <th aria-label="Aktionen" />
                </tr>
              </thead>
              <tbody>
                {sessions.map((session) => (
                  <tr key={session.id}>
                    <td>{formatDate(session.sessionDate)}</td>
                    <td>
                      <span className="with-dot">
                        <ColorDot color={session.courseColor} />
                        {session.courseName}
                      </span>
                    </td>
                    <td>{session.topic ?? '–'}</td>
                    <td>{formatMinutes(session.durationMinutes)}</td>
                    <td className="row-actions">
                      <button
                        type="button"
                        className="icon-button"
                        onClick={() => setEditing({ session })}
                        aria-label="Lerneinheit bearbeiten"
                      >
                        <Icon name="edit" />
                      </button>
                      <button
                        type="button"
                        className="icon-button danger"
                        onClick={() => onDelete(session)}
                        aria-label="Lerneinheit löschen"
                      >
                        <Icon name="trash" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {editing && courses && (
        <Modal
          title={editing.session ? 'Lerneinheit bearbeiten' : 'Lerneinheit eintragen'}
          onClose={() => setEditing(null)}
        >
          <StudySessionForm
            session={editing.session}
            courses={courses}
            onSaved={onSaved}
            onCancel={() => setEditing(null)}
          />
        </Modal>
      )}
    </>
  );
}

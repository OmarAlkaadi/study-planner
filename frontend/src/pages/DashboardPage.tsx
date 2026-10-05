import { useCallback, useState } from 'react';
import { Link } from 'react-router';
import { dashboardApi, taskApi } from '../api/api';
import { errorMessage } from '../api/client';
import type { Task } from '../api/types';
import { CourseMinutesChart, DailyBarChart } from '../components/Charts';
import { ColorDot, EmptyState, ErrorMessage, Loading, PageHeader, ProgressBar } from '../components/ui';
import { useAuth } from '../auth/useAuth';
import { useApiData } from '../utils/hooks';
import {
  COURSE_TYPE_LABELS,
  daysUntil,
  formatDate,
  formatDateTime,
  formatGrade,
  formatMinutes,
  formatTime,
  PRIORITY_LABELS,
  relativeDays,
} from '../utils/format';
import { useDialogs } from '../dialogs/useDialogs';

const longDateFormatter = new Intl.DateTimeFormat('de-DE', { weekday: 'long', day: 'numeric', month: 'long' });

/**
 * Startseite nach dem Login: Kennzahlen, heutige Veranstaltungen, nächste Klausuren,
 * bald fällige Aufgaben und Diagramme. Alle Daten kommen mit einer Anfrage (GET /api/dashboard).
 */
export function DashboardPage() {
  // Eigene Dialoge statt der Browser-Fenster (siehe DialogProvider)
  const dialogs = useDialogs();
  const { user } = useAuth();
  const loader = useCallback(() => dashboardApi.get(), []);
  const { data, error, loading, reload, setData } = useApiData(loader);
  // Datum einmal beim Öffnen merken (für die Begrüßung)
  const [today] = useState(() => new Date());

  // Checkbox bei "Bald fällig": Aufgabe erledigen (PATCH /api/tasks/{id}/status), danach Dashboard neu laden
  const markDone = async (task: Task) => {
    // Optimistisch: Aufgabe sofort aus der Liste nehmen
    setData((current) =>
      current ? { ...current, dueSoonTasks: current.dueSoonTasks.filter((t) => t.id !== task.id) } : current,
    );
    try {
      await taskApi.updateStatus(task.id, 'DONE');
    } catch (err) {
      dialogs.showError(errorMessage(err));
    }
    await reload();
  };

  return (
    <>
      <PageHeader
        title={`Hallo, ${user?.displayName ?? ''}!`}
        subtitle={`Heute ist ${longDateFormatter.format(today)}.`}
      />
      <ErrorMessage message={error} />
      {!data && loading && <Loading />}

      {data && (
        <>
          {/* Kennzahlen-Kacheln */}
          <section className="stat-grid">
            <div className="stat-card">
              <span className="stat-label">Kurse</span>
              <span className="stat-value">{data.courseCount}</span>
              <Link to="/courses" className="stat-link">
                Stundenplan ansehen
              </Link>
            </div>
            <div className="stat-card">
              <span className="stat-label">Offene Aufgaben</span>
              <span className="stat-value">{data.openTasks}</span>
              <span className={data.overdueTasks > 0 ? 'stat-note danger' : 'stat-note'}>
                {data.overdueTasks > 0 ? `${data.overdueTasks} überfällig` : 'nichts überfällig'}
              </span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Lernzeit (7 Tage)</span>
              <span className="stat-value">{formatMinutes(data.studyMinutesLast7Days)}</span>
              <Link to="/progress" className="stat-link">
                Lerneinheit eintragen
              </Link>
            </div>
            <div className="stat-card">
              <span className="stat-label">Notenschnitt</span>
              <span className="stat-value">{formatGrade(data.averageGrade)}</span>
              <span className="stat-note">
                {data.totalTasks > 0
                  ? `${Math.round((data.doneTasks / data.totalTasks) * 100)} % der Aufgaben erledigt`
                  : 'noch keine Aufgaben'}
              </span>
            </div>
          </section>

          {/* Karten mit Listen und Diagrammen */}
          <div className="dashboard-grid">
            <section className="card">
              <h2>Heute</h2>
              {data.todaysCourses.length === 0 ? (
                <EmptyState title="Heute keine Veranstaltungen" text="Zeit zum Lernen!" />
              ) : (
                <ul className="list">
                  {data.todaysCourses.map((course) => (
                    <li key={course.id} className="list-item">
                      <span className="time-badge">
                        {formatTime(course.startTime)}–{formatTime(course.endTime)}
                      </span>
                      <div className="list-main">
                        <span className="with-dot">
                          <ColorDot color={course.color} />
                          <strong>{course.name}</strong>
                        </span>
                        <span className="muted small">
                          {COURSE_TYPE_LABELS[course.courseType]}
                          {course.room && ` · ${course.room}`}
                        </span>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="card">
              <h2>Nächste Klausuren</h2>
              {data.upcomingExams.length === 0 ? (
                <EmptyState title="Keine anstehenden Klausuren" />
              ) : (
                <ul className="list">
                  {data.upcomingExams.map((exam) => {
                    const days = daysUntil(exam.examDate);
                    return (
                      <li key={exam.id} className="list-item">
                        <span className={days <= 7 ? 'countdown urgent' : 'countdown'}>
                          <strong>{days}</strong>
                          <small>{days === 1 ? 'Tag' : 'Tage'}</small>
                        </span>
                        <div className="list-main">
                          <strong>{exam.title}</strong>
                          <span className="muted small">{formatDateTime(exam.examDate)}</span>
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>

            <section className="card">
              <h2>Bald fällig</h2>
              {data.dueSoonTasks.length === 0 ? (
                <EmptyState title="Alles erledigt" text="Keine Aufgaben in den nächsten 7 Tagen." />
              ) : (
                <ul className="list">
                  {data.dueSoonTasks.map((task) => (
                    <li key={task.id} className="list-item">
                      <input
                        type="checkbox"
                        className="checkbox"
                        checked={false}
                        onChange={() => markDone(task)}
                        aria-label={`${task.title} als erledigt markieren`}
                      />
                      <div className="list-main">
                        <strong>{task.title}</strong>
                        <span className={task.overdue ? 'small danger-text' : 'muted small'}>
                          {task.overdue ? 'Überfällig' : 'Fällig'} {relativeDays(task.dueDate)} ·{' '}
                          {formatDate(task.dueDate)}
                        </span>
                      </div>
                      <span className={`badge priority-${task.priority.toLowerCase()}`}>
                        {PRIORITY_LABELS[task.priority]}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="card">
              <h2>Lernzeit der letzten 7 Tage</h2>
              <DailyBarChart data={data.dailyMinutes} />
            </section>

            <section className="card">
              <h2>Lernzeit pro Kurs</h2>
              {data.minutesPerCourse.length === 0 ? (
                <EmptyState title="Noch keine Lerneinheiten" />
              ) : (
                <CourseMinutesChart data={data.minutesPerCourse} />
              )}
            </section>

            <section className="card">
              <h2>Aufgabenfortschritt</h2>
              {data.taskProgress.length === 0 ? (
                <EmptyState title="Noch keine Aufgaben mit Kurs" />
              ) : (
                <ul className="hbar-list">
                  {data.taskProgress.map((entry) => (
                    <li key={entry.courseId}>
                      <div className="hbar-head">
                        <span className="with-dot">
                          <ColorDot color={entry.color} />
                          {entry.courseName}
                        </span>
                        <span className="muted">
                          {entry.done}/{entry.total}
                        </span>
                      </div>
                      <ProgressBar value={entry.done} max={entry.total} color={entry.color} />
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </>
  );
}

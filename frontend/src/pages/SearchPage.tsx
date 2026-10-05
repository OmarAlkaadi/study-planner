import { useCallback } from 'react';
import { Link, useSearchParams } from 'react-router';
import { searchApi } from '../api/api';
import { ColorDot, EmptyState, ErrorMessage, Loading, PageHeader } from '../components/ui';
import { DAY_LABELS, formatDate, formatDateTime, formatTime, STATUS_LABELS } from '../utils/format';
import { useApiData } from '../utils/hooks';

/**
 * Ergebnisse der globalen Suche. Der Suchtext kommt aus der URL (?q=...), die die Suchleiste im Layout
 * setzt. Ändert er sich, wird automatisch neu gesucht (GET /api/search). Ergebnisse gruppiert nach
 * Kursen, Klausuren und Aufgaben, jeweils mit Link zur passenden Seite.
 */
export function SearchPage() {
  const [params] = useSearchParams();
  // Suchtext aus der URL lesen, z. B. /search?q=mathe
  const query = (params.get('q') ?? '').trim();
  const loader = useCallback(() => searchApi.search(query), [query]);
  const { data, error, loading } = useApiData(loader);

  const total = data ? data.courses.length + data.exams.length + data.tasks.length : 0;

  return (
    <>
      <PageHeader title="Suche" subtitle={query ? `Ergebnisse für „${query}“` : undefined} />
      <ErrorMessage message={error} />
      {query.length < 2 && <EmptyState title="Bitte mindestens 2 Zeichen eingeben" />}
      {query.length >= 2 && !data && loading && <Loading />}
      {query.length >= 2 && data && total === 0 && (
        <EmptyState title="Nichts gefunden" text="Versuche einen anderen Suchbegriff." />
      )}

      {data && data.courses.length > 0 && (
        <section className="card">
          <h2>Kurse ({data.courses.length})</h2>
          <ul className="list">
            {data.courses.map((course) => (
              <li key={course.id} className="list-item">
                <div className="list-main">
                  <span className="with-dot">
                    <ColorDot color={course.color} />
                    <strong>{course.name}</strong>
                  </span>
                  <span className="muted small">
                    {DAY_LABELS[course.dayOfWeek]}, {formatTime(course.startTime)}–{formatTime(course.endTime)}
                    {course.lecturer && ` · ${course.lecturer}`}
                  </span>
                </div>
                <Link to="/courses" className="small">
                  Zu den Kursen
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {data && data.exams.length > 0 && (
        <section className="card">
          <h2>Klausuren ({data.exams.length})</h2>
          <ul className="list">
            {data.exams.map((exam) => (
              <li key={exam.id} className="list-item">
                <div className="list-main">
                  <strong>{exam.title}</strong>
                  <span className="muted small">
                    {exam.courseName} · {formatDateTime(exam.examDate)}
                  </span>
                </div>
                <Link to="/exams" className="small">
                  Zu den Klausuren
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {data && data.tasks.length > 0 && (
        <section className="card">
          <h2>Aufgaben ({data.tasks.length})</h2>
          <ul className="list">
            {data.tasks.map((task) => (
              <li key={task.id} className="list-item">
                <div className="list-main">
                  <strong>{task.title}</strong>
                  <span className="muted small">
                    {STATUS_LABELS[task.status]} · fällig {formatDate(task.dueDate)}
                    {task.courseName && ` · ${task.courseName}`}
                  </span>
                </div>
                <Link to="/tasks" className="small">
                  Zu den Aufgaben
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}

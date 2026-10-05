// Einfache Diagramme aus HTML/CSS (ohne Chart-Bibliothek) für Dashboard und Lernfortschritt.
// Höhe bzw. Breite jedes Balkens ist ein Prozentwert relativ zum größten Wert.

import type { CourseMinutes, DailyMinutes } from '../api/types';
import { formatMinutes, parseDate, todayIso } from '../utils/format';
import { ColorDot } from './ui';

/** Kurzer Wochentag, z. B. "Mo." – für die Beschriftung unter den Säulen. */
const weekdayFormatter = new Intl.DateTimeFormat('de-DE', { weekday: 'short' });

/** Säulendiagramm: Lernminuten pro Tag. */
export function DailyBarChart({ data }: { data: DailyMinutes[] }) {
  // Mindestens 60 als Maximum, damit kleine Werte nicht riesig aussehen
  const max = Math.max(60, ...data.map((day) => day.minutes));
  const today = todayIso();

  return (
    <div className="bar-chart" role="img" aria-label="Lernzeit der letzten 7 Tage">
      {data.map((day) => (
        <div className="bar-column" key={day.date} title={`${day.date}: ${formatMinutes(day.minutes)}`}>
          <span className="bar-value">{day.minutes > 0 ? day.minutes : ''}</span>
          <div className="bar-track">
            <div className="bar" style={{ height: `${(day.minutes / max) * 100}%` }} />
          </div>
          <span className={day.date === today ? 'bar-label today' : 'bar-label'}>
            {weekdayFormatter.format(parseDate(day.date)).replace('.', '')}
          </span>
        </div>
      ))}
    </div>
  );
}

/** Balken pro Kurs: gesamte Lernzeit. */
export function CourseMinutesChart({ data }: { data: CourseMinutes[] }) {
  const max = Math.max(1, ...data.map((entry) => entry.minutes));
  return (
    <ul className="hbar-list">
      {data.map((entry) => (
        <li key={entry.courseId}>
          <div className="hbar-head">
            <span className="with-dot">
              <ColorDot color={entry.color} />
              {entry.courseName}
            </span>
            <span className="muted">{formatMinutes(entry.minutes)}</span>
          </div>
          <div className="hbar-track">
            <div
              className="hbar"
              style={{ width: `${(entry.minutes / max) * 100}%`, background: entry.color ?? 'var(--primary)' }}
            />
          </div>
        </li>
      ))}
    </ul>
  );
}

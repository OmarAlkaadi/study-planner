import { useCallback, useMemo, useState, type CSSProperties } from 'react';
import { Link } from 'react-router';
import { calendarApi } from '../api/api';
import type { CalendarEvent, CalendarEventType } from '../api/types';
import { Icon } from '../components/Icon';
import { EmptyState, ErrorMessage, Loading, PageHeader } from '../components/ui';
import { addDays, formatDate, formatTime, parseDate, toIsoDate, todayIso } from '../utils/format';
import { useApiData } from '../utils/hooks';

/** Spaltenüberschriften – der Kalender beginnt am Montag. */
const WEEKDAYS = ['Mo', 'Di', 'Mi', 'Do', 'Fr', 'Sa', 'So'];
/** So viele Einträge passen in eine Tageszelle, der Rest erscheint als "+n weitere". */
const MAX_CHIPS = 3;
const monthFormatter = new Intl.DateTimeFormat('de-DE', { month: 'long', year: 'numeric' });

/** Beschriftung pro Eintragstyp. */
const TYPE_LABELS: Record<CalendarEventType, string> = {
  LECTURE: 'Veranstaltung',
  EXAM: 'Klausur',
  TASK: 'Deadline',
};

/** Zielseite für den Link "Öffnen" pro Eintragstyp. */
const TYPE_LINKS: Record<CalendarEventType, string> = {
  LECTURE: '/courses',
  EXAM: '/exams',
  TASK: '/tasks',
};

/** Montag der Woche, in der das Datum liegt. */
function startOfWeek(date: Date): Date {
  const mondayIndex = (date.getDay() + 6) % 7; // So=0 -> 6, Mo=1 -> 0
  return addDays(date, -mondayIndex);
}

/**
 * Seite "Kalender": Monatsraster mit allen Einträgen, darunter die Liste für den ausgewählten Tag.
 *
 * Ablauf: month bestimmt die 42 angezeigten Tage → daraus entsteht der loader →
 * useApiData lädt GET /api/calendar?from=...&to=... → die Einträge werden nach Datum gruppiert.
 */
export function CalendarPage() {
  // Angezeigter Monat (immer der 1.) und ausgewählter Tag
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [selected, setSelected] = useState(todayIso());

  // Raster: 6 Wochen ab dem Montag vor dem Monatsersten (42 Tage)
  const days = useMemo(() => {
    const start = startOfWeek(month);
    return Array.from({ length: 42 }, (_, index) => toIsoDate(addDays(start, index)));
  }, [month]);

  // Anderer Monat → neuer loader → useApiData lädt automatisch neu
  const loader = useCallback(() => calendarApi.events(days[0], days[days.length - 1]), [days]);
  const { data: events, error, loading } = useApiData(loader);

  // Einträge nach Datum gruppieren: "2026-10-05" → [Einträge dieses Tages]
  const eventsByDay = useMemo(() => {
    const map = new Map<string, CalendarEvent[]>();
    for (const event of events ?? []) {
      const list = map.get(event.date) ?? [];
      list.push(event);
      map.set(event.date, list);
    }
    return map;
  }, [events]);

  // Einen Monat vor (+1) oder zurück (-1) blättern
  const changeMonth = (delta: number) => {
    setMonth((current) => new Date(current.getFullYear(), current.getMonth() + delta, 1));
  };

  // Zurück zum aktuellen Monat und zum heutigen Tag
  const goToday = () => {
    const now = new Date();
    setMonth(new Date(now.getFullYear(), now.getMonth(), 1));
    setSelected(todayIso());
  };

  const today = todayIso();
  const selectedEvents = eventsByDay.get(selected) ?? [];

  return (
    <>
      <PageHeader title="Kalender" subtitle="Vorlesungen, Klausuren und Deadlines auf einen Blick" />

      <section className="card calendar-card">
        <div className="calendar-head">
          <div className="calendar-nav">
            <button type="button" className="icon-button" onClick={() => changeMonth(-1)} aria-label="Vorheriger Monat">
              <Icon name="left" />
            </button>
            <h2 className="calendar-title">{monthFormatter.format(month)}</h2>
            <button type="button" className="icon-button" onClick={() => changeMonth(1)} aria-label="Nächster Monat">
              <Icon name="right" />
            </button>
          </div>
          <div className="calendar-legend small">
            <span className="legend lecture">Veranstaltung</span>
            <span className="legend exam">Klausur</span>
            <span className="legend task">Deadline</span>
            <button type="button" className="button secondary small" onClick={goToday}>
              Heute
            </button>
          </div>
        </div>

        <ErrorMessage message={error} />

        {/* Monatsraster: 7 Wochentags-Überschriften + 42 Tageszellen (Klick wählt den Tag aus) */}
        <div className={loading ? 'calendar-grid is-loading' : 'calendar-grid'}>
          {WEEKDAYS.map((weekday) => (
            <div key={weekday} className="calendar-weekday">
              {weekday}
            </div>
          ))}
          {days.map((day) => {
            const date = parseDate(day);
            const dayEvents = eventsByDay.get(day) ?? [];
            const classes = ['calendar-day'];
            if (date.getMonth() !== month.getMonth()) classes.push('outside');
            if (day === today) classes.push('today');
            if (day === selected) classes.push('selected');
            return (
              <button
                type="button"
                key={day}
                className={classes.join(' ')}
                onClick={() => setSelected(day)}
                aria-label={`${formatDate(day)}, ${dayEvents.length} Einträge`}
                aria-pressed={day === selected}
              >
                <span className="day-number">{date.getDate()}</span>
                {dayEvents.slice(0, MAX_CHIPS).map((event) => (
                  <span
                    key={event.key}
                    className={`chip-event ${event.type.toLowerCase()}${event.done ? ' done' : ''}`}
                    style={{ '--event-color': event.color ?? 'var(--primary)' } as CSSProperties}
                  >
                    {event.startTime && <b>{formatTime(event.startTime)}</b>} {event.title}
                  </span>
                ))}
                {dayEvents.length > MAX_CHIPS && (
                  <span className="more small">+{dayEvents.length - MAX_CHIPS} weitere</span>
                )}
              </button>
            );
          })}
        </div>
      </section>

      {/* Alle Einträge des ausgewählten Tages */}
      <section className="card">
        <h2>{formatDate(selected)}</h2>
        {!events && loading && <Loading />}
        {events && selectedEvents.length === 0 && <EmptyState title="Keine Einträge an diesem Tag" />}
        {selectedEvents.length > 0 && (
          <ul className="list">
            {selectedEvents.map((event) => (
              <li key={event.key} className="list-item">
                <span className="time-badge">
                  {event.startTime
                    ? `${formatTime(event.startTime)}${event.endTime ? `–${formatTime(event.endTime)}` : ''}`
                    : 'ganztägig'}
                </span>
                <div className="list-main">
                  <strong className={event.done ? 'strike' : undefined}>{event.title}</strong>
                  <span className="muted small">
                    {TYPE_LABELS[event.type]}
                    {event.courseName && event.type !== 'LECTURE' && ` · ${event.courseName}`}
                    {event.location && ` · ${event.location}`}
                    {event.done && ' · erledigt'}
                  </span>
                </div>
                <Link to={TYPE_LINKS[event.type]} className="small">
                  Öffnen
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}

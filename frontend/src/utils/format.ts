// Hilfsfunktionen für Datum, Uhrzeit und Beschriftungen.
// Wichtig: Datumswerte vom Backend sind "lokale" Werte ohne Zeitzone.
// Deshalb NIE new Date("2026-10-05") verwenden (wird als UTC gelesen und kann
// um einen Tag verrutschen), sondern immer die Funktionen hier.

import type { CourseType, DayOfWeek, TaskPriority, TaskStatus } from '../api/types';

/** "2026-10-05" -> Date (lokale Zeit, 00:00 Uhr) */
export function parseDate(isoDate: string): Date {
  const [year, month, day] = isoDate.slice(0, 10).split('-').map(Number);
  return new Date(year, month - 1, day);
}

/** "2026-10-05T09:30:00" -> Date (lokale Zeit) */
export function parseDateTime(isoDateTime: string): Date {
  const date = parseDate(isoDateTime);
  const [hours, minutes] = isoDateTime.slice(11, 16).split(':').map(Number);
  date.setHours(hours || 0, minutes || 0, 0, 0);
  return date;
}

/** Date -> "2026-10-05" (lokale Zeit, nicht UTC) */
export function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/** Heutiges Datum als "2026-10-05". */
export function todayIso(): string {
  return toIsoDate(new Date());
}

/** Neues Datum, um days Tage verschoben (negativ = zurück). Das Original bleibt unverändert. */
export function addDays(date: Date, days: number): Date {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

/** Anzahl Kalendertage von heute bis zum Datum (negativ = Vergangenheit). */
export function daysUntil(isoDate: string): number {
  const today = parseDate(todayIso());
  const target = parseDate(isoDate);
  return Math.round((target.getTime() - today.getTime()) / 86_400_000);
}

/** "in 3 Tagen", "morgen", "heute", "vor 2 Tagen" */
export function relativeDays(isoDate: string): string {
  const days = daysUntil(isoDate);
  if (days === 0) return 'heute';
  if (days === 1) return 'morgen';
  if (days === -1) return 'gestern';
  if (days > 1) return `in ${days} Tagen`;
  return `vor ${-days} Tagen`;
}

/** Formatierer für deutsche Datumsangaben (einmal erzeugt und wiederverwendet). */
const dateFormatter = new Intl.DateTimeFormat('de-DE', {
  weekday: 'short',
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

/** "2026-10-05" -> "Mo., 05.10.2026" */
export function formatDate(isoDate: string): string {
  return dateFormatter.format(parseDate(isoDate));
}

const shortDateFormatter = new Intl.DateTimeFormat('de-DE', { day: '2-digit', month: '2-digit', year: '2-digit' });

/** "2026-10-05" -> "05.10.26" */
export function formatShortDate(isoDate: string): string {
  return shortDateFormatter.format(parseDate(isoDate));
}

/**
 * Zeitraum mit optionalem Anfang/Ende, z. B. für das Semester eines Kurses:
 * beide -> "12.10.26 – 05.02.27", nur Anfang -> "ab 12.10.26", nur Ende -> "bis 05.02.27", keins -> "–"
 */
export function formatPeriod(start: string | null, end: string | null): string {
  if (start && end) return `${formatShortDate(start)} – ${formatShortDate(end)}`;
  if (start) return `ab ${formatShortDate(start)}`;
  if (end) return `bis ${formatShortDate(end)}`;
  return '–';
}

/** "2026-10-05T09:30:00" -> "Mo., 05.10.2026, 09:30 Uhr" */
export function formatDateTime(isoDateTime: string): string {
  return `${formatDate(isoDateTime)}, ${formatTime(isoDateTime.slice(11))} Uhr`;
}

/** "08:15:00" -> "08:15" */
export function formatTime(time: string | null): string {
  return time ? time.slice(0, 5) : '';
}

/** 135 -> "2 h 15 min" */
export function formatMinutes(total: number): string {
  const hours = Math.floor(total / 60);
  const minutes = total % 60;
  if (hours === 0) return `${minutes} min`;
  if (minutes === 0) return `${hours} h`;
  return `${hours} h ${minutes} min`;
}

/** 1.7 -> "1,7"; keine Note -> "–" */
export function formatGrade(grade: number | null): string {
  return grade === null ? '–' : grade.toFixed(1).replace('.', ',');
}

/** Wochentage von Montag bis Sonntag (Reihenfolge für Auswahllisten und den Stundenplan). */
export const DAYS: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

// Deutsche Beschriftungen für die Enum-Werte aus dem Backend
export const DAY_LABELS: Record<DayOfWeek, string> = {
  MONDAY: 'Montag',
  TUESDAY: 'Dienstag',
  WEDNESDAY: 'Mittwoch',
  THURSDAY: 'Donnerstag',
  FRIDAY: 'Freitag',
  SATURDAY: 'Samstag',
  SUNDAY: 'Sonntag',
};

export const COURSE_TYPE_LABELS: Record<CourseType, string> = {
  VORLESUNG: 'Vorlesung',
  UEBUNG: 'Übung',
  PRAKTIKUM: 'Praktikum',
  SEMINAR: 'Seminar',
  SONSTIGES: 'Sonstiges',
};

export const PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: 'Niedrig',
  MEDIUM: 'Mittel',
  HIGH: 'Hoch',
};

export const STATUS_LABELS: Record<TaskStatus, string> = {
  OPEN: 'Offen',
  IN_PROGRESS: 'In Arbeit',
  DONE: 'Erledigt',
};

// Standardfarbe eines Kurses (wie CourseService.DEFAULT_COLOR im Backend) und die Auswahlfarben im Kursformular
export const DEFAULT_COURSE_COLOR = '#4f46e5';

export const COURSE_COLORS = [
  '#4f46e5',
  '#2563eb',
  '#0d9488',
  '#16a34a',
  '#ca8a04',
  '#ea580c',
  '#db2777',
  '#7c3aed',
];

// Unit-Tests für die Datums- und Formatierungsfunktionen (Vitest, Aufruf: npm test).
// Wichtig sind vor allem die Datumsfunktionen: Sie dürfen nie um einen Tag "verrutschen".

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  addDays,
  daysUntil,
  formatDate,
  formatDateTime,
  formatGrade,
  formatMinutes,
  formatPeriod,
  formatShortDate,
  formatTime,
  parseDate,
  parseDateTime,
  relativeDays,
  toIsoDate,
  todayIso,
} from './format';

describe('Datum lesen und schreiben', () => {
  it('liest "2026-10-05" als lokales Datum ohne Zeitverschiebung', () => {
    const date = parseDate('2026-10-05');
    expect(date.getFullYear()).toBe(2026);
    expect(date.getMonth()).toBe(9); // Oktober (Monate zählen ab 0)
    expect(date.getDate()).toBe(5);
    expect(date.getHours()).toBe(0);
  });

  it('liest Datum mit Uhrzeit', () => {
    const date = parseDateTime('2026-10-05T09:30:00');
    expect(date.getDate()).toBe(5);
    expect(date.getHours()).toBe(9);
    expect(date.getMinutes()).toBe(30);
  });

  it('wandelt hin und zurück ohne Änderung', () => {
    expect(toIsoDate(parseDate('2026-01-31'))).toBe('2026-01-31');
    expect(toIsoDate(new Date(2026, 1, 3))).toBe('2026-02-03');
  });

  it('addiert Tage über Monats- und Jahresgrenzen, ohne das Original zu ändern', () => {
    const start = parseDate('2026-12-30');
    expect(toIsoDate(addDays(start, 3))).toBe('2027-01-02');
    expect(toIsoDate(addDays(start, -30))).toBe('2026-11-30');
    expect(toIsoDate(start)).toBe('2026-12-30');
  });
});

describe('relative Angaben (abhängig vom heutigen Datum)', () => {
  // "Heute" auf einen festen Tag stellen, damit die Tests immer gleich laufen
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 9, 5, 14, 0));
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('kennt das heutige Datum', () => {
    expect(todayIso()).toBe('2026-10-05');
  });

  it('zählt Tage bis zu einem Datum (negativ = Vergangenheit)', () => {
    expect(daysUntil('2026-10-05')).toBe(0);
    expect(daysUntil('2026-10-08')).toBe(3);
    expect(daysUntil('2026-10-01')).toBe(-4);
    // Uhrzeit spielt keine Rolle
    expect(daysUntil('2026-10-06T23:59:00')).toBe(1);
  });

  it('beschreibt Abstände in Worten', () => {
    expect(relativeDays('2026-10-05')).toBe('heute');
    expect(relativeDays('2026-10-06')).toBe('morgen');
    expect(relativeDays('2026-10-04')).toBe('gestern');
    expect(relativeDays('2026-10-15')).toBe('in 10 Tagen');
    expect(relativeDays('2026-10-02')).toBe('vor 3 Tagen');
  });
});

describe('Anzeige-Formate', () => {
  it('formatiert Datum und Uhrzeit auf Deutsch', () => {
    expect(formatDate('2026-10-05')).toBe('Mo., 05.10.2026');
    expect(formatDateTime('2026-10-05T09:30:00')).toBe('Mo., 05.10.2026, 09:30 Uhr');
    expect(formatTime('08:15:00')).toBe('08:15');
    expect(formatTime(null)).toBe('');
  });

  it('formatiert Minuten als Stunden und Minuten', () => {
    expect(formatMinutes(45)).toBe('45 min');
    expect(formatMinutes(120)).toBe('2 h');
    expect(formatMinutes(135)).toBe('2 h 15 min');
    expect(formatMinutes(0)).toBe('0 min');
  });

  it('formatiert Noten mit Komma', () => {
    expect(formatGrade(1.7)).toBe('1,7');
    expect(formatGrade(2)).toBe('2,0');
    expect(formatGrade(null)).toBe('–');
  });

  it('formatiert Zeiträume mit optionalem Anfang und Ende (Semester)', () => {
    expect(formatShortDate('2026-10-12')).toBe('12.10.26');
    expect(formatPeriod('2026-10-12', '2027-02-05')).toBe('12.10.26 – 05.02.27');
    expect(formatPeriod('2026-10-12', null)).toBe('ab 12.10.26');
    expect(formatPeriod(null, '2027-02-05')).toBe('bis 05.02.27');
    expect(formatPeriod(null, null)).toBe('–');
  });
});

package de.studyplanner.calendar;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ein Eintrag im Kalender.
 *
 * @param key       eindeutiger Schlüssel, z. B. "lecture-3-2026-10-05"
 * @param refId     ID des Kurses, der Klausur oder der Aufgabe
 * @param startTime bei Aufgaben leer (ganztägig)
 * @param done      nur bei Aufgaben relevant
 */
public record CalendarEvent(
        String key,
        CalendarEventType type,
        Long refId,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String color,
        String courseName,
        String location,
        boolean done
) {
}

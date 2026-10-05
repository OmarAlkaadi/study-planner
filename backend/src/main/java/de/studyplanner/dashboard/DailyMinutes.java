package de.studyplanner.dashboard;

import java.time.LocalDate;

/** Lernzeit (Summe in Minuten) an einem Tag. */
public record DailyMinutes(LocalDate date, Long minutes) {
}

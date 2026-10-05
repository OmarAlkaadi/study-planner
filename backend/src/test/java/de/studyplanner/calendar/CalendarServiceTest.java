package de.studyplanner.calendar;

import de.studyplanner.course.Course;
import de.studyplanner.course.CourseType;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Reiner Unit-Test ohne Spring und ohne Datenbank. */
class CalendarServiceTest {

    /** Ein Mittwochskurs erzeugt im Oktober 2026 genau vier Termine. */
    @Test
    void expandsWeeklyLecturesIntoRange() {
        Course course = new Course();
        course.setName("Mathe");
        course.setCourseType(CourseType.VORLESUNG);
        course.setDayOfWeek(DayOfWeek.WEDNESDAY);
        course.setStartTime(LocalTime.of(8, 15));
        course.setEndTime(LocalTime.of(9, 45));

        // 01.10.2026 ist ein Donnerstag -> Mittwoche im Oktober: 7., 14., 21., 28.
        List<CalendarEvent> events = CalendarService.expandLectures(
                List.of(course), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(events).extracting(CalendarEvent::date).containsExactly(
                LocalDate.of(2026, 10, 7),
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 28));
        assertThat(events).allSatisfy(event -> {
            assertThat(event.type()).isEqualTo(CalendarEventType.LECTURE);
            assertThat(event.startTime()).isEqualTo(LocalTime.of(8, 15));
        });
    }

    /** Liegt der Wochentag nicht im Zeitraum, entstehen keine Termine. */
    @Test
    void noEventsIfDayIsNotInRange() {
        Course course = new Course();
        course.setName("Mathe");
        course.setDayOfWeek(DayOfWeek.MONDAY);
        course.setStartTime(LocalTime.of(8, 0));
        course.setEndTime(LocalTime.of(9, 0));

        // Di 06.10. bis Fr 09.10.2026 enthält keinen Montag
        assertThat(CalendarService.expandLectures(
                List.of(course), LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 9))).isEmpty();
    }

    /** Außerhalb des Semesterzeitraums (z. B. in den Ferien) entstehen keine Termine; Grenzen zählen mit. */
    @Test
    void respectsSemesterPeriod() {
        Course course = new Course();
        course.setName("Mathe");
        course.setDayOfWeek(DayOfWeek.WEDNESDAY);
        course.setStartTime(LocalTime.of(8, 15));
        course.setEndTime(LocalTime.of(9, 45));
        course.setSemesterStart(LocalDate.of(2026, 10, 14));
        course.setSemesterEnd(LocalDate.of(2026, 10, 21));

        List<CalendarEvent> events = CalendarService.expandLectures(
                List.of(course), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(events).extracting(CalendarEvent::date).containsExactly(
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 21));
    }
}

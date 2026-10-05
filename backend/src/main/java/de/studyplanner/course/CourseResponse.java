package de.studyplanner.course;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Kursdaten, wie sie als JSON an das Frontend gehen (ohne den Benutzer). */
public record CourseResponse(
        Long id,
        String name,
        String lecturer,
        String room,
        CourseType courseType,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer ects,
        String color,
        LocalDate semesterStart,
        LocalDate semesterEnd
) {

    /** Wandelt die Course-Entity in dieses DTO um. */
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getName(),
                course.getLecturer(),
                course.getRoom(),
                course.getCourseType(),
                course.getDayOfWeek(),
                course.getStartTime(),
                course.getEndTime(),
                course.getEcts(),
                course.getColor(),
                course.getSemesterStart(),
                course.getSemesterEnd()
        );
    }
}

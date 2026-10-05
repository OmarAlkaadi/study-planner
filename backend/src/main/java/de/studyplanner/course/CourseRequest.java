package de.studyplanner.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * JSON-Body für POST/PUT /api/courses. Die Annotationen prüfen Pflichtfelder, Längen, ECTS-Bereich
 * und das Farbformat (#RRGGBB). Dass die Endzeit nach der Startzeit und das Semesterende nicht vor dem
 * Semesterbeginn liegt, prüft der CourseService.
 */
public record CourseRequest(
        @NotBlank(message = "Name ist erforderlich")
        @Size(max = 150, message = "Name darf höchstens 150 Zeichen lang sein")
        String name,

        @Size(max = 150, message = "Dozent darf höchstens 150 Zeichen lang sein")
        String lecturer,

        @Size(max = 50, message = "Raum darf höchstens 50 Zeichen lang sein")
        String room,

        @NotNull(message = "Typ ist erforderlich")
        CourseType courseType,

        @NotNull(message = "Wochentag ist erforderlich")
        DayOfWeek dayOfWeek,

        @NotNull(message = "Startzeit ist erforderlich")
        LocalTime startTime,

        @NotNull(message = "Endzeit ist erforderlich")
        LocalTime endTime,

        @Min(value = 0, message = "ECTS darf nicht negativ sein")
        @Max(value = 30, message = "ECTS darf höchstens 30 sein")
        Integer ects,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Farbe muss im Format #RRGGBB sein")
        String color,

        /* optional: Semesterzeitraum, null = unbegrenzt */
        LocalDate semesterStart,

        LocalDate semesterEnd
) {
}

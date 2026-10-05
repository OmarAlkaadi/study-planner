package de.studyplanner.studysession;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** JSON-Body für POST/PUT /api/study-sessions. Dauer zwischen 1 und 1440 Minuten, Thema optional. */
public record StudySessionRequest(
        @NotNull(message = "Kurs ist erforderlich")
        Long courseId,

        @NotNull(message = "Datum ist erforderlich")
        LocalDate sessionDate,

        @NotNull(message = "Dauer ist erforderlich")
        @Min(value = 1, message = "Dauer muss mindestens 1 Minute sein")
        @Max(value = 1440, message = "Dauer darf höchstens 1440 Minuten (24 h) sein")
        Integer durationMinutes,

        @Size(max = 200, message = "Thema darf höchstens 200 Zeichen lang sein")
        String topic
) {
}

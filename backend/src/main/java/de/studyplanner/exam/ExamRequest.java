package de.studyplanner.exam;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JSON-Body für POST/PUT /api/exams. examDate kommt als "2026-10-05T09:00";
 * die Note ist optional und muss zwischen 1,0 und 5,0 mit höchstens einer Nachkommastelle liegen.
 */
public record ExamRequest(
        @NotNull(message = "Kurs ist erforderlich")
        Long courseId,

        @NotBlank(message = "Titel ist erforderlich")
        @Size(max = 150, message = "Titel darf höchstens 150 Zeichen lang sein")
        String title,

        @NotNull(message = "Datum ist erforderlich")
        LocalDateTime examDate,

        @Size(max = 50, message = "Raum darf höchstens 50 Zeichen lang sein")
        String room,

        @Size(max = 1000, message = "Notizen dürfen höchstens 1000 Zeichen lang sein")
        String notes,

        @DecimalMin(value = "1.0", message = "Note muss zwischen 1,0 und 5,0 liegen")
        @DecimalMax(value = "5.0", message = "Note muss zwischen 1,0 und 5,0 liegen")
        @Digits(integer = 1, fraction = 1, message = "Note darf nur eine Nachkommastelle haben")
        BigDecimal grade
) {
}

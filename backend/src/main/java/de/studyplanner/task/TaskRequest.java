package de.studyplanner.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** JSON-Body für POST/PUT /api/tasks. courseId ist optional (Aufgabe ohne Kurs, z. B. private To-dos). */
public record TaskRequest(
        @NotBlank(message = "Titel ist erforderlich")
        @Size(max = 150, message = "Titel darf höchstens 150 Zeichen lang sein")
        String title,

        @Size(max = 2000, message = "Beschreibung darf höchstens 2000 Zeichen lang sein")
        String description,

        /* optional */
        Long courseId,

        @NotNull(message = "Deadline ist erforderlich")
        LocalDate dueDate,

        @NotNull(message = "Priorität ist erforderlich")
        TaskPriority priority,

        @NotNull(message = "Status ist erforderlich")
        TaskStatus status
) {
}

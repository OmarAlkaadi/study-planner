package de.studyplanner.task;

import jakarta.validation.constraints.NotNull;

/** JSON-Body für PATCH /api/tasks/{id}/status, z. B. {"status": "DONE"}. */
public record TaskStatusRequest(
        @NotNull(message = "Status ist erforderlich")
        TaskStatus status
) {
}

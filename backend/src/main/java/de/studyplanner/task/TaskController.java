package de.studyplanner.task;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-Controller für Aufgaben (Basis-Pfad /api/tasks) – CRUD, Filter und schnelles Status-Ändern.
 * Leitet alle Anfragen an den TaskService weiter.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** Beispiel: GET /api/tasks?status=OPEN&amp;priority=HIGH&amp;courseId=3&amp;q=blatt */
    @GetMapping
    public List<TaskResponse> findAll(@RequestParam(required = false) TaskStatus status,
                                      @RequestParam(required = false) TaskPriority priority,
                                      @RequestParam(required = false) Long courseId,
                                      @RequestParam(required = false) String q) {
        return taskService.findAll(status, priority, courseId, q);
    }

    /** GET /api/tasks/{id} – eine einzelne Aufgabe. */
    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    /** POST /api/tasks – legt eine Aufgabe an (HTTP 201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request);
    }

    /** PUT /api/tasks/{id} – ändert alle Felder einer Aufgabe. */
    @PutMapping("/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    /** PATCH /api/tasks/{id}/status – ändert nur den Status (Checkbox "erledigt" im Frontend). */
    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        return taskService.updateStatus(id, request.status());
    }

    /** DELETE /api/tasks/{id} – löscht eine Aufgabe (HTTP 204). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}

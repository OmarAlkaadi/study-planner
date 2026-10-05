package de.studyplanner.task;

import de.studyplanner.common.CurrentUser;
import de.studyplanner.common.NotFoundException;
import de.studyplanner.common.TextUtils;
import de.studyplanner.course.CourseService;
import de.studyplanner.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Geschäftslogik für Aufgaben: gefilterte Liste, Anlegen, Ändern, Status ändern, Löschen.
 * Die Filter werden als Specifications zusammengesetzt und von der Datenbank ausgewertet (SQL WHERE).
 * Wird vom TaskController und von der globalen Suche (SearchController) aufgerufen.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final CourseService courseService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public TaskService(TaskRepository taskRepository, CourseService courseService,
                       UserRepository userRepository, CurrentUser currentUser) {
        this.taskRepository = taskRepository;
        this.courseService = courseService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    /**
     * Aufgaben mit optionalen Filtern. Sortierung: nach Deadline, erledigte Aufgaben zuletzt.
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> findAll(TaskStatus status, TaskPriority priority, Long courseId, String query) {
        Specification<Task> spec = TaskSpecifications.belongsTo(currentUser.id());
        if (status != null) {
            spec = spec.and(TaskSpecifications.hasStatus(status));
        }
        if (priority != null) {
            spec = spec.and(TaskSpecifications.hasPriority(priority));
        }
        if (courseId != null) {
            spec = spec.and(TaskSpecifications.hasCourse(courseId));
        }
        if (query != null && !query.isBlank()) {
            spec = spec.and(TaskSpecifications.matchesText(TextUtils.likePattern(query)));
        }

        Sort sort = Sort.by("dueDate").ascending().and(Sort.by("id").ascending());
        return taskRepository.findAll(spec, sort).stream()
                .sorted(Comparator.comparing((Task task) -> task.getStatus() == TaskStatus.DONE))
                .map(TaskResponse::from)
                .toList();
    }

    /** Eine einzelne eigene Aufgabe. */
    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return TaskResponse.from(getOwnedTask(id));
    }

    /** Legt eine Aufgabe für den angemeldeten Benutzer an. */
    public TaskResponse create(TaskRequest request) {
        Task task = new Task();
        task.setUser(userRepository.getReferenceById(currentUser.id()));
        apply(task, request);
        return TaskResponse.from(taskRepository.save(task));
    }

    /** Ändert eine eigene Aufgabe. */
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getOwnedTask(id);
        apply(task, request);
        return TaskResponse.from(task);
    }

    /** Schnelles Ändern nur des Status (z. B. Checkbox "erledigt"). */
    public TaskResponse updateStatus(Long id, TaskStatus status) {
        Task task = getOwnedTask(id);
        task.changeStatus(status);
        return TaskResponse.from(task);
    }

    /** Löscht eine eigene Aufgabe. */
    public void delete(Long id) {
        taskRepository.delete(getOwnedTask(id));
    }

    /** Lädt eine Aufgabe nur, wenn sie dem Benutzer gehört – sonst 404. */
    private Task getOwnedTask(Long id) {
        return taskRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Aufgabe nicht gefunden."));
    }

    /** Überträgt die Formulardaten auf die Entity. Ein angegebener Kurs muss dem Benutzer gehören. */
    private void apply(Task task, TaskRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(TextUtils.blankToNull(request.description()));
        task.setCourse(request.courseId() != null ? courseService.getOwnedCourse(request.courseId()) : null);
        task.setDueDate(request.dueDate());
        task.setPriority(request.priority());
        task.changeStatus(request.status());
    }
}

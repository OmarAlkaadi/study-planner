package de.studyplanner.search;

import de.studyplanner.course.CourseService;
import de.studyplanner.exam.ExamService;
import de.studyplanner.task.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Globale Suche über Kurse, Klausuren und Aufgaben (GET /api/search?q=...).
 * Hat keinen eigenen Service, sondern nutzt die Suchfunktionen der vorhandenen Services
 * (CourseService, ExamService, TaskService) und kürzt die Ergebnisse auf 10 pro Typ.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int MAX_RESULTS_PER_TYPE = 10;

    private final CourseService courseService;
    private final ExamService examService;
    private final TaskService taskService;

    public SearchController(CourseService courseService, ExamService examService, TaskService taskService) {
        this.courseService = courseService;
        this.examService = examService;
        this.taskService = taskService;
    }

    /** Sucht ab 2 Zeichen in allen drei Bereichen; bei kürzerem Suchtext kommt eine leere Antwort. */
    @GetMapping
    public SearchResponse search(@RequestParam(required = false) String q) {
        if (q == null || q.trim().length() < MIN_QUERY_LENGTH) {
            return SearchResponse.empty();
        }
        return new SearchResponse(
                limit(courseService.findAll(q)),
                limit(examService.findAll(null, null, q)),
                limit(taskService.findAll(null, null, null, q))
        );
    }

    /** Schneidet eine Ergebnisliste nach MAX_RESULTS_PER_TYPE Einträgen ab. */
    private static <T> List<T> limit(List<T> list) {
        return list.stream().limit(MAX_RESULTS_PER_TYPE).toList();
    }
}

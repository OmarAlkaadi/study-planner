package de.studyplanner.exam;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
 * REST-Controller für Klausuren (Basis-Pfad /api/exams) – CRUD plus Filter.
 * Leitet alle Anfragen an den ExamService weiter.
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /** GET /api/exams?courseId=&amp;upcoming=&amp;q= – alle eigenen Klausuren, alle Filter sind optional. */
    @GetMapping
    public List<ExamResponse> findAll(@RequestParam(required = false) Long courseId,
                                      @RequestParam(required = false) Boolean upcoming,
                                      @RequestParam(required = false) String q) {
        return examService.findAll(courseId, upcoming, q);
    }

    /** GET /api/exams/{id} – eine einzelne Klausur. */
    @GetMapping("/{id}")
    public ExamResponse findById(@PathVariable Long id) {
        return examService.findById(id);
    }

    /** POST /api/exams – legt eine Klausur an (HTTP 201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse create(@Valid @RequestBody ExamRequest request) {
        return examService.create(request);
    }

    /** PUT /api/exams/{id} – ändert eine Klausur (z. B. Note nachtragen). */
    @PutMapping("/{id}")
    public ExamResponse update(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return examService.update(id, request);
    }

    /** DELETE /api/exams/{id} – löscht eine Klausur (HTTP 204). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        examService.delete(id);
    }
}

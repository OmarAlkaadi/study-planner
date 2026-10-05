package de.studyplanner.course;

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
 * REST-Controller für Kurse (Basis-Pfad /api/courses) – klassisches CRUD.
 * Leitet alle Anfragen an den CourseService weiter; alle Endpunkte brauchen ein JWT.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /** GET /api/courses?q=... – alle eigenen Kurse, optional gefiltert nach Suchtext (Name oder Dozent). */
    @GetMapping
    public List<CourseResponse> findAll(@RequestParam(required = false) String q) {
        return courseService.findAll(q);
    }

    /** GET /api/courses/{id} – ein einzelner Kurs (404, wenn fremd oder nicht vorhanden). */
    @GetMapping("/{id}")
    public CourseResponse findById(@PathVariable Long id) {
        return courseService.findById(id);
    }

    /** POST /api/courses – legt einen Kurs an (HTTP 201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return courseService.create(request);
    }

    /** PUT /api/courses/{id} – überschreibt alle Felder eines Kurses. */
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.update(id, request);
    }

    /** DELETE /api/courses/{id} – löscht einen Kurs (HTTP 204 ohne Inhalt). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.delete(id);
    }
}

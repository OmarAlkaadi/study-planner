package de.studyplanner.studysession;

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
 * REST-Controller für Lerneinheiten (Basis-Pfad /api/study-sessions).
 * Leitet alle Anfragen an den StudySessionService weiter.
 */
@RestController
@RequestMapping("/api/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    /** GET /api/study-sessions?courseId= – alle Lerneinheiten, optional nur für einen Kurs. */
    @GetMapping
    public List<StudySessionResponse> findAll(@RequestParam(required = false) Long courseId) {
        return studySessionService.findAll(courseId);
    }

    /** POST /api/study-sessions – trägt eine Lerneinheit ein (HTTP 201). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudySessionResponse create(@Valid @RequestBody StudySessionRequest request) {
        return studySessionService.create(request);
    }

    /** PUT /api/study-sessions/{id} – ändert eine Lerneinheit. */
    @PutMapping("/{id}")
    public StudySessionResponse update(@PathVariable Long id, @Valid @RequestBody StudySessionRequest request) {
        return studySessionService.update(id, request);
    }

    /** DELETE /api/study-sessions/{id} – löscht eine Lerneinheit (HTTP 204). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studySessionService.delete(id);
    }
}

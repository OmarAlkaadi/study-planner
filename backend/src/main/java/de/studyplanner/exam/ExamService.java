package de.studyplanner.exam;

import de.studyplanner.common.CurrentUser;
import de.studyplanner.common.NotFoundException;
import de.studyplanner.common.TextUtils;
import de.studyplanner.course.CourseService;
import de.studyplanner.user.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Geschäftslogik für Klausuren: Auflisten mit Filtern, Anlegen, Ändern, Löschen.
 * Nutzt CourseService.getOwnedCourse, damit Klausuren nur eigenen Kursen zugeordnet werden können.
 * Wird vom ExamController und von der globalen Suche (SearchController) aufgerufen.
 */
@Service
@Transactional
public class ExamService {

    private final ExamRepository examRepository;
    private final CourseService courseService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public ExamService(ExamRepository examRepository, CourseService courseService,
                       UserRepository userRepository, CurrentUser currentUser) {
        this.examRepository = examRepository;
        this.courseService = courseService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    /**
     * Alle Klausuren, optional gefiltert – die Filter werden als Specifications zusammengesetzt
     * und von der Datenbank ausgewertet (SQL WHERE), sortiert nach Datum.
     *
     * @param courseId nur Klausuren dieses Kurses (optional)
     * @param upcoming true = nur zukünftige, false = nur vergangene, null = alle
     * @param query    Suchtext für Titel und Kursname (optional)
     */
    @Transactional(readOnly = true)
    public List<ExamResponse> findAll(Long courseId, Boolean upcoming, String query) {
        Specification<Exam> spec = ExamSpecifications.belongsTo(currentUser.id());
        if (courseId != null) {
            spec = spec.and(ExamSpecifications.hasCourse(courseId));
        }
        if (upcoming != null) {
            LocalDateTime now = LocalDateTime.now();
            spec = spec.and(upcoming ? ExamSpecifications.notBefore(now) : ExamSpecifications.before(now));
        }
        if (query != null && !query.isBlank()) {
            spec = spec.and(ExamSpecifications.matchesText(TextUtils.likePattern(query)));
        }
        Sort sort = Sort.by("examDate").ascending().and(Sort.by("id").ascending());
        return examRepository.findAll(spec, sort).stream()
                .map(ExamResponse::from)
                .toList();
    }

    /** Eine einzelne eigene Klausur. */
    @Transactional(readOnly = true)
    public ExamResponse findById(Long id) {
        return ExamResponse.from(getOwnedExam(id));
    }

    /** Legt eine Klausur für den angemeldeten Benutzer an. */
    public ExamResponse create(ExamRequest request) {
        Exam exam = new Exam();
        exam.setUser(userRepository.getReferenceById(currentUser.id()));
        apply(exam, request);
        return ExamResponse.from(examRepository.save(exam));
    }

    /** Ändert eine eigene Klausur; die Transaktion speichert die Änderungen automatisch. */
    public ExamResponse update(Long id, ExamRequest request) {
        Exam exam = getOwnedExam(id);
        apply(exam, request);
        return ExamResponse.from(exam);
    }

    /** Löscht eine eigene Klausur. */
    public void delete(Long id) {
        examRepository.delete(getOwnedExam(id));
    }

    /** Lädt eine Klausur nur, wenn sie dem Benutzer gehört – sonst 404. */
    private Exam getOwnedExam(Long id) {
        return examRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Klausur nicht gefunden."));
    }

    /** Überträgt die Formulardaten auf die Entity. */
    private void apply(Exam exam, ExamRequest request) {
        // getOwnedCourse stellt sicher, dass der Kurs dem Benutzer gehört
        exam.setCourse(courseService.getOwnedCourse(request.courseId()));
        exam.setTitle(request.title().trim());
        exam.setExamDate(request.examDate());
        exam.setRoom(TextUtils.blankToNull(request.room()));
        exam.setNotes(TextUtils.blankToNull(request.notes()));
        exam.setGrade(request.grade());
    }
}

package de.studyplanner.course;

import de.studyplanner.common.BadRequestException;
import de.studyplanner.common.CurrentUser;
import de.studyplanner.common.NotFoundException;
import de.studyplanner.common.TextUtils;
import de.studyplanner.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Geschäftslogik für Kurse: Lesen, Suchen, Anlegen, Ändern, Löschen.
 *
 * @Transactional an der Klasse: Jede öffentliche Methode läuft in einer Datenbanktransaktion.
 * Änderungen an geladenen Entities (z. B. in update) werden am Ende automatisch gespeichert.
 * getOwnedCourse wird auch von ExamService, TaskService und StudySessionService benutzt, um
 * sicherzustellen, dass man nur eigene Kurse verknüpfen kann.
 */
@Service
@Transactional
public class CourseService {

    /** Farbe, falls beim Anlegen keine gewählt wurde. */
    public static final String DEFAULT_COLOR = "#4f46e5";

    /** Sortierung wie im Stundenplan: erst Wochentag, dann Uhrzeit. */
    public static final Comparator<Course> SCHEDULE_ORDER =
            Comparator.comparing(Course::getDayOfWeek).thenComparing(Course::getStartTime);

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository, CurrentUser currentUser) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    /**
     * Alle Kurse des Benutzers (ohne Suchtext) bzw. nur passende (mit Suchtext), sortiert wie im Stundenplan.
     */
    @Transactional(readOnly = true)
    public List<CourseResponse> findAll(String query) {
        Long userId = currentUser.id();
        List<Course> courses = (query == null || query.isBlank())
                ? courseRepository.findByUserId(userId)
                : courseRepository.search(userId, TextUtils.likePattern(query));
        return courses.stream()
                .sorted(SCHEDULE_ORDER)
                .map(CourseResponse::from)
                .toList();
    }

    /** Ein einzelner eigener Kurs. */
    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        return CourseResponse.from(getOwnedCourse(id));
    }

    /**
     * Legt einen Kurs für den angemeldeten Benutzer an. getReferenceById lädt den User nicht,
     * sondern erzeugt nur einen Verweis auf seine ID – das reicht für den Fremdschlüssel.
     */
    public CourseResponse create(CourseRequest request) {
        validateTimes(request);
        Course course = new Course();
        course.setUser(userRepository.getReferenceById(currentUser.id()));
        apply(course, request);
        return CourseResponse.from(courseRepository.save(course));
    }

    /** Ändert einen eigenen Kurs. Kein save() nötig: Die Transaktion schreibt die Änderungen automatisch. */
    public CourseResponse update(Long id, CourseRequest request) {
        validateTimes(request);
        Course course = getOwnedCourse(id);
        apply(course, request);
        return CourseResponse.from(course);
    }

    /** Löscht den Kurs. Klausuren und Lerneinheiten werden per ON DELETE CASCADE mitgelöscht. */
    public void delete(Long id) {
        courseRepository.delete(getOwnedCourse(id));
    }

    /**
     * Lädt einen Kurs, aber nur wenn er dem angemeldeten Benutzer gehört – sonst NotFoundException (404).
     * Wird auch von den Services für Klausuren, Aufgaben und Lerneinheiten verwendet.
     */
    @Transactional(readOnly = true)
    public Course getOwnedCourse(Long id) {
        return courseRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Kurs nicht gefunden."));
    }

    /**
     * Fachliche Prüfungen, die mit Annotationen nicht gehen (sonst 400): Endzeit nach der Startzeit,
     * Semesterende nicht vor dem Semesterbeginn (falls beide angegeben sind).
     */
    private static void validateTimes(CourseRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("Die Endzeit muss nach der Startzeit liegen.");
        }
        if (request.semesterStart() != null && request.semesterEnd() != null
                && request.semesterEnd().isBefore(request.semesterStart())) {
            throw new BadRequestException("Das Semesterende darf nicht vor dem Semesterbeginn liegen.");
        }
    }

    /** Überträgt die Formulardaten auf die Entity (Texte getrimmt, leere optionale Felder als null). */
    private static void apply(Course course, CourseRequest request) {
        course.setName(request.name().trim());
        course.setLecturer(TextUtils.blankToNull(request.lecturer()));
        course.setRoom(TextUtils.blankToNull(request.room()));
        course.setCourseType(request.courseType());
        course.setDayOfWeek(request.dayOfWeek());
        course.setStartTime(request.startTime());
        course.setEndTime(request.endTime());
        course.setEcts(request.ects());
        course.setColor(request.color() != null ? request.color() : DEFAULT_COLOR);
        course.setSemesterStart(request.semesterStart());
        course.setSemesterEnd(request.semesterEnd());
    }
}

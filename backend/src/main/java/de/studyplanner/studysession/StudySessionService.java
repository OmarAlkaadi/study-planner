package de.studyplanner.studysession;

import de.studyplanner.common.CurrentUser;
import de.studyplanner.common.NotFoundException;
import de.studyplanner.common.TextUtils;
import de.studyplanner.course.CourseService;
import de.studyplanner.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Geschäftslogik für Lerneinheiten: Auflisten, Anlegen, Ändern, Löschen.
 * Die Statistiken (Summen pro Tag/Kurs) berechnet der DashboardService.
 */
@Service
@Transactional
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final CourseService courseService;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public StudySessionService(StudySessionRepository studySessionRepository, CourseService courseService,
                               UserRepository userRepository, CurrentUser currentUser) {
        this.studySessionRepository = studySessionRepository;
        this.courseService = courseService;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    /** Neueste Lerneinheiten zuerst, optional nur für einen Kurs. */
    @Transactional(readOnly = true)
    public List<StudySessionResponse> findAll(Long courseId) {
        Long userId = currentUser.id();
        List<StudySession> sessions = courseId == null
                ? studySessionRepository.findByUserIdOrderBySessionDateDescIdDesc(userId)
                : studySessionRepository.findByUserIdAndCourseIdOrderBySessionDateDescIdDesc(userId, courseId);
        return sessions.stream().map(StudySessionResponse::from).toList();
    }

    /** Trägt eine Lerneinheit für den angemeldeten Benutzer ein. */
    public StudySessionResponse create(StudySessionRequest request) {
        StudySession session = new StudySession();
        session.setUser(userRepository.getReferenceById(currentUser.id()));
        apply(session, request);
        return StudySessionResponse.from(studySessionRepository.save(session));
    }

    /** Ändert eine eigene Lerneinheit. */
    public StudySessionResponse update(Long id, StudySessionRequest request) {
        StudySession session = getOwnedSession(id);
        apply(session, request);
        return StudySessionResponse.from(session);
    }

    /** Löscht eine eigene Lerneinheit. */
    public void delete(Long id) {
        studySessionRepository.delete(getOwnedSession(id));
    }

    /** Lädt eine Lerneinheit nur, wenn sie dem Benutzer gehört – sonst 404. */
    private StudySession getOwnedSession(Long id) {
        return studySessionRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Lerneinheit nicht gefunden."));
    }

    /** Überträgt die Formulardaten auf die Entity; der Kurs muss dem Benutzer gehören. */
    private void apply(StudySession session, StudySessionRequest request) {
        session.setCourse(courseService.getOwnedCourse(request.courseId()));
        session.setSessionDate(request.sessionDate());
        session.setDurationMinutes(request.durationMinutes());
        session.setTopic(TextUtils.blankToNull(request.topic()));
    }
}

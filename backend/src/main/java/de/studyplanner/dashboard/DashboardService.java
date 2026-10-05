package de.studyplanner.dashboard;

import de.studyplanner.common.CurrentUser;
import de.studyplanner.course.CourseRepository;
import de.studyplanner.course.CourseResponse;
import de.studyplanner.course.CourseService;
import de.studyplanner.exam.ExamRepository;
import de.studyplanner.exam.ExamResponse;
import de.studyplanner.studysession.StudySessionRepository;
import de.studyplanner.task.TaskRepository;
import de.studyplanner.task.TaskResponse;
import de.studyplanner.task.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Berechnet alle Dashboard-Statistiken. Die Zahlen werden direkt in der Datenbank berechnet
 * (COUNT, SUM, AVG, GROUP BY in den Repositories) statt alle Datensätze in Java zu laden.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** "Bald fällig" = Deadline in den nächsten 7 Tagen. */
    private static final int DUE_SOON_DAYS = 7;
    /** Höchstens so viele Aufgaben in der Liste "Bald fällig". */
    private static final int DUE_SOON_LIMIT = 6;

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final TaskRepository taskRepository;
    private final StudySessionRepository studySessionRepository;
    private final CurrentUser currentUser;

    public DashboardService(CourseRepository courseRepository, ExamRepository examRepository,
                            TaskRepository taskRepository, StudySessionRepository studySessionRepository,
                            CurrentUser currentUser) {
        this.courseRepository = courseRepository;
        this.examRepository = examRepository;
        this.taskRepository = taskRepository;
        this.studySessionRepository = studySessionRepository;
        this.currentUser = currentUser;
    }

    /** Sammelt alle Werte aus den Repositories und baut daraus die DashboardResponse. */
    public DashboardResponse getDashboard() {
        Long userId = currentUser.id();
        LocalDate today = LocalDate.now();

        // Kennzahlen für die Kacheln oben (SQL: COUNT)
        long totalTasks = taskRepository.countByUserId(userId);
        long doneTasks = taskRepository.countByUserIdAndStatus(userId, TaskStatus.DONE);
        long overdueTasks = taskRepository.countByUserIdAndStatusNotAndDueDateBefore(userId, TaskStatus.DONE, today);

        // Heutige Veranstaltungen (nur innerhalb des Semesterzeitraums), sortiert nach Uhrzeit
        List<CourseResponse> todaysCourses = courseRepository.findByUserIdAndDayOfWeek(userId, today.getDayOfWeek())
                .stream()
                .filter(course -> course.takesPlaceOn(today))
                .sorted(CourseService.SCHEDULE_ORDER)
                .map(CourseResponse::from)
                .toList();

        // Die nächsten 5 Klausuren ab jetzt
        List<ExamResponse> upcomingExams = examRepository
                .findTop5ByUserIdAndExamDateGreaterThanEqualOrderByExamDateAsc(userId, LocalDateTime.now())
                .stream()
                .map(ExamResponse::from)
                .toList();

        // Offene Aufgaben, die in den nächsten 7 Tagen fällig oder schon überfällig sind (höchstens 6)
        List<TaskResponse> dueSoonTasks = taskRepository
                .findByUserIdAndStatusNotAndDueDateLessThanEqualOrderByDueDateAsc(
                        userId, TaskStatus.DONE, today.plusDays(DUE_SOON_DAYS))
                .stream()
                .limit(DUE_SOON_LIMIT)
                .map(TaskResponse::from)
                .toList();

        // Lernzeit pro Tag für das Säulendiagramm und die Summe der letzten 7 Tage
        List<DailyMinutes> dailyMinutes = lastSevenDays(userId, today);
        long minutesLast7Days = dailyMinutes.stream().mapToLong(DailyMinutes::minutes).sum();

        return new DashboardResponse(
                courseRepository.countByUserId(userId),
                totalTasks,
                totalTasks - doneTasks,
                doneTasks,
                overdueTasks,
                minutesLast7Days,
                roundGrade(examRepository.averageGrade(userId)),
                todaysCourses,
                upcomingExams,
                dueSoonTasks,
                dailyMinutes,
                studySessionRepository.minutesPerCourse(userId),
                taskRepository.taskProgressPerCourse(userId, TaskStatus.DONE)
        );
    }

    /**
     * Nur die Lern-Statistiken für die Seite "Lernfortschritt" – ohne Kurse, Klausuren und Aufgabenlisten
     * des Dashboards. Spart dort vier Datenbankabfragen und unnötige Daten in der Antwort.
     */
    public ProgressResponse getProgress() {
        Long userId = currentUser.id();
        List<DailyMinutes> dailyMinutes = lastSevenDays(userId, LocalDate.now());
        return new ProgressResponse(
                dailyMinutes.stream().mapToLong(DailyMinutes::minutes).sum(),
                dailyMinutes,
                studySessionRepository.minutesPerCourse(userId),
                taskRepository.taskProgressPerCourse(userId, TaskStatus.DONE)
        );
    }

    /** Liefert genau 7 Einträge (heute und die 6 Tage davor), auch für Tage ohne Lernzeit. */
    private List<DailyMinutes> lastSevenDays(Long userId, LocalDate today) {
        LocalDate from = today.minusDays(6);
        Map<LocalDate, Long> minutesByDate = studySessionRepository.minutesPerDay(userId, from, today).stream()
                .collect(Collectors.toMap(DailyMinutes::date, DailyMinutes::minutes));
        return from.datesUntil(today.plusDays(1))
                .map(date -> new DailyMinutes(date, minutesByDate.getOrDefault(date, 0L)))
                .toList();
    }

    /** Rundet den Notenschnitt auf zwei Nachkommastellen (null bleibt null). */
    private static Double roundGrade(Double average) {
        return average == null ? null : Math.round(average * 100) / 100.0;
    }
}

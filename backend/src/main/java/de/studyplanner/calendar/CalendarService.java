package de.studyplanner.calendar;

import de.studyplanner.common.BadRequestException;
import de.studyplanner.common.CurrentUser;
import de.studyplanner.course.Course;
import de.studyplanner.course.CourseRepository;
import de.studyplanner.exam.Exam;
import de.studyplanner.exam.ExamRepository;
import de.studyplanner.task.Task;
import de.studyplanner.task.TaskRepository;
import de.studyplanner.task.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fasst Vorlesungen (wöchentlich), Klausuren und Aufgaben-Deadlines zu Kalendereinträgen zusammen.
 *
 * Es gibt keine eigene Kalender-Tabelle: Die Einträge werden bei jeder Anfrage aus den Tabellen
 * courses, exams und tasks berechnet. Kurse werden dabei für jede Woche im Zeitraum "ausgerollt".
 */
@Service
@Transactional(readOnly = true)
public class CalendarService {

    /** Maximal abfragbarer Zeitraum (reicht für die 6-Wochen-Monatsansicht im Frontend). */
    static final int MAX_RANGE_DAYS = 62;

    /** Sortierung: nach Datum, dann Uhrzeit (ganztägige Einträge ohne Uhrzeit zuerst), dann Titel. */
    private static final Comparator<CalendarEvent> EVENT_ORDER = Comparator
            .comparing(CalendarEvent::date)
            .thenComparing(CalendarEvent::startTime, Comparator.nullsFirst(Comparator.<LocalTime>naturalOrder()))
            .thenComparing(CalendarEvent::title);

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final TaskRepository taskRepository;
    private final CurrentUser currentUser;

    public CalendarService(CourseRepository courseRepository, ExamRepository examRepository,
                           TaskRepository taskRepository, CurrentUser currentUser) {
        this.courseRepository = courseRepository;
        this.examRepository = examRepository;
        this.taskRepository = taskRepository;
        this.currentUser = currentUser;
    }

    /**
     * Alle Kalendereinträge von from bis to (jeweils einschließlich).
     * Prüft zuerst den Zeitraum (400 bei ungültigem oder zu langem Zeitraum), lädt dann Kurse, Klausuren
     * und Aufgaben des Benutzers, wandelt alles in CalendarEvent um und sortiert die Liste.
     */
    public List<CalendarEvent> getEvents(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("'to' darf nicht vor 'from' liegen.");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BadRequestException("Der Zeitraum darf höchstens " + MAX_RANGE_DAYS + " Tage umfassen.");
        }
        Long userId = currentUser.id();
        List<CalendarEvent> events = new ArrayList<>();

        events.addAll(expandLectures(courseRepository.findByUserId(userId), from, to));

        examRepository.findByUserIdAndExamDateGreaterThanEqualAndExamDateLessThanOrderByExamDateAsc(
                        userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .forEach(exam -> events.add(fromExam(exam)));

        taskRepository.findByUserIdAndDueDateBetween(userId, from, to)
                .forEach(task -> events.add(fromTask(task)));

        events.sort(EVENT_ORDER);
        return events;
    }

    /**
     * Erzeugt für jede Woche im Zeitraum einen Termin pro Kurs an dessen Wochentag –
     * aber nur innerhalb des Semesterzeitraums des Kurses (siehe Course.takesPlaceOn).
     */
    static List<CalendarEvent> expandLectures(List<Course> courses, LocalDate from, LocalDate to) {
        List<CalendarEvent> events = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (Course course : courses) {
                if (course.takesPlaceOn(date)) {
                    events.add(new CalendarEvent(
                            "lecture-" + course.getId() + "-" + date,
                            CalendarEventType.LECTURE,
                            course.getId(),
                            course.getName(),
                            date,
                            course.getStartTime(),
                            course.getEndTime(),
                            course.getColor(),
                            course.getName(),
                            course.getRoom(),
                            false));
                }
            }
        }
        return events;
    }

    /** Klausur → Kalendereintrag (Startzeit aus dem Klausurdatum, Farbe vom Kurs). */
    private static CalendarEvent fromExam(Exam exam) {
        LocalTime start = exam.getExamDate().toLocalTime();
        return new CalendarEvent(
                "exam-" + exam.getId(),
                CalendarEventType.EXAM,
                exam.getId(),
                exam.getTitle(),
                exam.getExamDate().toLocalDate(),
                start,
                null,
                exam.getCourse().getColor(),
                exam.getCourse().getName(),
                exam.getRoom(),
                false);
    }

    /** Aufgabe → ganztägiger Kalendereintrag am Tag der Deadline (done = erledigt). */
    private static CalendarEvent fromTask(Task task) {
        Course course = task.getCourse();
        return new CalendarEvent(
                "task-" + task.getId(),
                CalendarEventType.TASK,
                task.getId(),
                task.getTitle(),
                task.getDueDate(),
                null,
                null,
                course != null ? course.getColor() : null,
                course != null ? course.getName() : null,
                null,
                task.getStatus() == TaskStatus.DONE);
    }
}

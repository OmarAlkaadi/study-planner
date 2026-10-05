package de.studyplanner.demo;

import de.studyplanner.course.Course;
import de.studyplanner.course.CourseRepository;
import de.studyplanner.course.CourseType;
import de.studyplanner.exam.Exam;
import de.studyplanner.exam.ExamRepository;
import de.studyplanner.studysession.StudySession;
import de.studyplanner.studysession.StudySessionRepository;
import de.studyplanner.task.Task;
import de.studyplanner.task.TaskPriority;
import de.studyplanner.task.TaskRepository;
import de.studyplanner.task.TaskStatus;
import de.studyplanner.user.User;
import de.studyplanner.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Legt beim Start einen Demo-Account mit Beispieldaten an, damit man die App sofort ausprobieren kann.
 * Aktiv nur mit app.demo-data=true (Umgebungsvariable APP_DEMO_DATA=true).
 * Alle Termine sind relativ zum heutigen Datum, damit das Dashboard immer "lebendig" aussieht.
 *
 * Besucher dürfen in der Demo Kurse, Klausuren usw. ändern. Damit sie trotzdem für alle brauchbar bleibt,
 * werden die Demo-Daten jede Nacht komplett neu angelegt (app.demo-reset-cron, Standard 4:00 Uhr).
 * Die Kontodaten selbst (Name, Passwort, Konto löschen) sind gesperrt – siehe DemoAccount.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final TaskRepository taskRepository;
    private final StudySessionRepository studySessionRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserRepository userRepository, CourseRepository courseRepository,
                               ExamRepository examRepository, TaskRepository taskRepository,
                               StudySessionRepository studySessionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.examRepository = examRepository;
        this.taskRepository = taskRepository;
        this.studySessionRepository = studySessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Wird von Spring Boot einmal nach dem Start aufgerufen (CommandLineRunner).
     * Existiert der Demo-Account schon, passiert nichts – die Daten werden also nur einmal angelegt.
     */
    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(DemoAccount.EMAIL)) {
            return;
        }
        createDemoData();
        log.info("Demo-Daten angelegt: Login mit {} / {}", DemoAccount.EMAIL, DemoAccount.PASSWORD);
    }

    /**
     * Setzt die Demo jede Nacht zurück: Demo-Account samt aller Daten löschen (die Datenbank löscht Kurse,
     * Klausuren, Aufgaben, Lerneinheiten und Tokens per ON DELETE CASCADE mit) und frisch anlegen –
     * mit Terminen passend zum neuen Tag. Alles in einer Transaktion: Schlägt etwas fehl, bleibt der alte Stand.
     */
    @Scheduled(cron = "${app.demo-reset-cron}")
    @Transactional
    public void resetDemoData() {
        userRepository.deleteByEmail(DemoAccount.EMAIL);
        createDemoData();
        log.info("Demo-Daten zurückgesetzt");
    }

    /** Legt den Demo-Account mit Stundenplan, Klausuren, Aufgaben und Lerneinheiten an. */
    private void createDemoData() {
        User user = new User(DemoAccount.EMAIL, passwordEncoder.encode(DemoAccount.PASSWORD), "Demo Student");
        // Die Demo-Adresse kann keine Mails empfangen – deshalb gilt sie direkt als bestätigt
        user.markEmailVerified();
        user = userRepository.save(user);
        LocalDate today = LocalDate.now();

        // Stundenplan: 6 Kurse von Montag bis Freitag
        Course mathe = course(user, "Mathematik 1", "Prof. Dr. Weber", "A 1.12", CourseType.VORLESUNG,
                DayOfWeek.MONDAY, "08:15", "09:45", 5, "#2563eb");
        Course prog = course(user, "Programmierung 2", "Prof. Dr. Schmidt", "C 0.03", CourseType.VORLESUNG,
                DayOfWeek.TUESDAY, "10:00", "11:30", 7, "#16a34a");
        Course progLab = course(user, "Programmierung 2 – Praktikum", "M. Sc. Yilmaz", "PC-Pool 2", CourseType.PRAKTIKUM,
                DayOfWeek.WEDNESDAY, "14:00", "17:00", null, "#0d9488");
        Course db = course(user, "Datenbanken", "Prof. Dr. Neumann", "B 2.05", CourseType.VORLESUNG,
                DayOfWeek.WEDNESDAY, "08:15", "09:45", 5, "#db2777");
        Course web = course(user, "Webentwicklung", "Dr. Fischer", "A 0.20", CourseType.SEMINAR,
                DayOfWeek.THURSDAY, "12:00", "13:30", 5, "#ea580c");
        Course matheUe = course(user, "Mathematik 1 – Übung", "B. Sc. Wagner", "A 1.08", CourseType.UEBUNG,
                DayOfWeek.FRIDAY, "10:00", "11:30", null, "#7c3aed");

        // Klausuren: 3 anstehende (ohne Note) und 2 vergangene (mit Note → Notenschnitt)
        exam(user, db, "Klausur Datenbanken", today.plusDays(12), "10:00", "Audimax", null);
        exam(user, mathe, "Klausur Mathematik 1", today.plusDays(19), "09:00", "Audimax", null);
        exam(user, prog, "Klausur Programmierung 2", today.plusDays(33), "13:00", "C 0.03", null);
        exam(user, web, "Projektabgabe Webentwicklung", today.minusDays(40), "12:00", "A 0.20", new BigDecimal("1.7"));
        exam(user, matheUe, "Testat Mathematik", today.minusDays(55), "10:00", "A 1.08", new BigDecimal("2.3"));

        // Aufgaben: überfällige, bald fällige, eine ohne Kurs und bereits erledigte
        task(user, mathe, "Übungsblatt 4 abgeben", today.minusDays(2), TaskPriority.HIGH, TaskStatus.OPEN);
        task(user, prog, "Praktikumsaufgabe 3: Collections", today.plusDays(1), TaskPriority.HIGH, TaskStatus.IN_PROGRESS);
        task(user, progLab, "Praktikumsprotokoll 3 hochladen", today.plusDays(2), TaskPriority.MEDIUM, TaskStatus.OPEN);
        task(user, db, "ER-Diagramm für Bibliothek erstellen", today.plusDays(3), TaskPriority.MEDIUM, TaskStatus.OPEN);
        task(user, web, "Portfolio-Seite responsiv machen", today.plusDays(6), TaskPriority.LOW, TaskStatus.OPEN);
        task(user, db, "SQL-Übungen Joins & GROUP BY", today.plusDays(10), TaskPriority.MEDIUM, TaskStatus.OPEN);
        task(user, null, "Semesterbeitrag überweisen", today.plusDays(4), TaskPriority.MEDIUM, TaskStatus.OPEN);
        task(user, mathe, "Übungsblatt 3 abgeben", today.minusDays(9), TaskPriority.HIGH, TaskStatus.DONE);
        task(user, prog, "Praktikumsaufgabe 2: Vererbung", today.minusDays(6), TaskPriority.MEDIUM, TaskStatus.DONE);
        task(user, db, "Normalisierung wiederholen", today.minusDays(3), TaskPriority.LOW, TaskStatus.DONE);

        // Lerneinheiten der letzten 12 Tage für die Diagramme (Kurs und Thema wechseln reihum)
        int[][] plan = {
                // {Tage zurück, Minuten}
                {0, 45}, {1, 90}, {2, 60}, {3, 30}, {5, 120}, {6, 75}, {8, 60}, {10, 90}, {12, 45}
        };
        Course[] rotation = {mathe, prog, db, web, mathe, db, prog, mathe, web};
        String[] topics = {"Integralrechnung", "Generics", "Joins", "Flexbox & Grid", "Folgen und Reihen",
                "Normalformen", "Streams", "Lineare Algebra", "React Hooks"};
        for (int i = 0; i < plan.length; i++) {
            session(user, rotation[i], today.minusDays(plan[i][0]), plan[i][1], topics[i]);
        }

    }

    /** Hilfsmethode: legt einen Kurs (mit Semesterzeitraum) an und speichert ihn. */
    private Course course(User user, String name, String lecturer, String room, CourseType type,
                          DayOfWeek day, String start, String end, Integer ects, String color) {
        Course course = new Course();
        course.setUser(user);
        course.setName(name);
        course.setLecturer(lecturer);
        course.setRoom(room);
        course.setCourseType(type);
        course.setDayOfWeek(day);
        course.setStartTime(LocalTime.parse(start));
        course.setEndTime(LocalTime.parse(end));
        course.setEcts(ects);
        course.setColor(color);
        // Semester: begann vor 4 Wochen und endet in 12 Wochen – so zeigt die Demo auch den Semesterzeitraum
        course.setSemesterStart(LocalDate.now().minusWeeks(4));
        course.setSemesterEnd(LocalDate.now().plusWeeks(12));
        return courseRepository.save(course);
    }

    /** Hilfsmethode: legt eine Klausur an und speichert sie. */
    private void exam(User user, Course course, String title, LocalDate date, String time, String room, BigDecimal grade) {
        Exam exam = new Exam();
        exam.setUser(user);
        exam.setCourse(course);
        exam.setTitle(title);
        exam.setExamDate(date.atTime(LocalTime.parse(time)));
        exam.setRoom(room);
        exam.setGrade(grade);
        examRepository.save(exam);
    }

    /** Hilfsmethode: legt eine Aufgabe an und speichert sie. */
    private void task(User user, Course course, String title, LocalDate dueDate, TaskPriority priority, TaskStatus status) {
        Task task = new Task();
        task.setUser(user);
        task.setCourse(course);
        task.setTitle(title);
        task.setDueDate(dueDate);
        task.setPriority(priority);
        task.changeStatus(status);
        taskRepository.save(task);
    }

    /** Hilfsmethode: legt eine Lerneinheit an und speichert sie. */
    private void session(User user, Course course, LocalDate date, int minutes, String topic) {
        StudySession session = new StudySession();
        session.setUser(user);
        session.setCourse(course);
        session.setSessionDate(date);
        session.setDurationMinutes(minutes);
        session.setTopic(topic);
        studySessionRepository.save(session);
    }
}

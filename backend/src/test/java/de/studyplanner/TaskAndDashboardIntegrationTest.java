package de.studyplanner;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Integrationstests für Aufgaben, Dashboard-Statistiken, Kalender und Suche. */
class TaskAndDashboardIntegrationTest extends IntegrationTestBase {

    /** Hilfsmethode: legt eine Aufgabe an und gibt ihre ID zurück. */
    private long createTask(String token, Long courseId, String title, LocalDate dueDate,
                            String priority, String status) throws Exception {
        String body = """
                {"title": "%s", "description": "Beschreibung", "courseId": %s, "dueDate": "%s",
                 "priority": "%s", "status": "%s"}
                """.formatted(title, courseId == null ? "null" : courseId.toString(), dueDate, priority, status);
        String response = postJson("/api/tasks", token, body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    /** Alle Aufgabenfilter einzeln und kombiniert sowie die Sortierung. */
    @Test
    void filterTasks() throws Exception {
        String token = registerUser();
        long mathe = createCourse(token, "Mathe", "MONDAY");
        LocalDate today = LocalDate.now();

        createTask(token, mathe, "Übungsblatt 1", today.plusDays(2), "HIGH", "OPEN");
        createTask(token, mathe, "Übungsblatt 2", today.plusDays(9), "LOW", "DONE");
        createTask(token, null, "Wohnung putzen", today.plusDays(1), "MEDIUM", "IN_PROGRESS");

        getJson("/api/tasks", token).andExpect(jsonPath("$", hasSize(3)));
        getJson("/api/tasks?status=OPEN", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Übungsblatt 1"));
        getJson("/api/tasks?priority=MEDIUM", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseName", nullValue()));
        getJson("/api/tasks?courseId=" + mathe, token).andExpect(jsonPath("$", hasSize(2)));
        getJson("/api/tasks?q=BLATT", token).andExpect(jsonPath("$", hasSize(2)));
        getJson("/api/tasks?q=blatt&status=DONE", token).andExpect(jsonPath("$", hasSize(1)));

        // Sortierung: offene Aufgaben nach Deadline, erledigte zuletzt
        getJson("/api/tasks", token)
                .andExpect(jsonPath("$[0].title").value("Wohnung putzen"))
                .andExpect(jsonPath("$[1].title").value("Übungsblatt 1"))
                .andExpect(jsonPath("$[2].title").value("Übungsblatt 2"));

        getJson("/api/tasks?status=UNBEKANNT", token).andExpect(status().isBadRequest());
    }

    /** Status DONE setzt completedAt und hebt "überfällig" auf; zurück auf OPEN löscht completedAt. */
    @Test
    void changingStatusSetsCompletedAt() throws Exception {
        String token = registerUser();
        long id = createTask(token, null, "Bericht", LocalDate.now().minusDays(1), "HIGH", "OPEN");

        getJson("/api/tasks/" + id, token)
                .andExpect(jsonPath("$.overdue").value(true))
                .andExpect(jsonPath("$.completedAt", nullValue()));

        patchJson("/api/tasks/" + id + "/status", token, """
                {"status": "DONE"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.completedAt", notNullValue()));

        patchJson("/api/tasks/" + id + "/status", token, """
                {"status": "OPEN"}
                """)
                .andExpect(jsonPath("$.completedAt", nullValue()));
    }

    /** Kurs löschen lässt die Aufgabe bestehen, nur courseId wird null (ON DELETE SET NULL). */
    @Test
    void deletingCourseKeepsTasks() throws Exception {
        String token = registerUser();
        long course = createCourse(token, "Physik", "FRIDAY");
        long task = createTask(token, course, "Laborbericht", LocalDate.now().plusDays(3), "MEDIUM", "OPEN");

        deleteRequest("/api/courses/" + course, token).andExpect(status().isNoContent());

        getJson("/api/tasks/" + task, token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId", nullValue()));
    }

    /** Prüft alle Kennzahlen und Listen des Dashboards mit bekannten Testdaten. */
    @Test
    void dashboardStatistics() throws Exception {
        String token = registerUser();
        LocalDate today = LocalDate.now();
        long mathe = createCourse(token, "Mathe", today.getDayOfWeek().name());
        long db = createCourse(token, "Datenbanken", today.plusDays(1).getDayOfWeek().name());

        createTask(token, mathe, "Blatt 1", today.minusDays(3), "HIGH", "OPEN");     // überfällig
        createTask(token, mathe, "Blatt 2", today.plusDays(2), "MEDIUM", "DONE");
        createTask(token, db, "ER-Modell", today.plusDays(5), "LOW", "OPEN");

        postJson("/api/study-sessions", token, """
                {"courseId": %d, "sessionDate": "%s", "durationMinutes": 90, "topic": "Analysis"}
                """.formatted(mathe, today)).andExpect(status().isCreated());
        postJson("/api/study-sessions", token, """
                {"courseId": %d, "sessionDate": "%s", "durationMinutes": 30}
                """.formatted(mathe, today.minusDays(1))).andExpect(status().isCreated());
        postJson("/api/study-sessions", token, """
                {"courseId": %d, "sessionDate": "%s", "durationMinutes": 45}
                """.formatted(db, today.minusDays(20))).andExpect(status().isCreated());

        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Klausur DB", "examDate": "%s"}
                """.formatted(db, LocalDateTime.now().plusDays(10).withNano(0))).andExpect(status().isCreated());
        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Alte Klausur", "examDate": "2020-01-01T10:00", "grade": 1.3}
                """.formatted(mathe)).andExpect(status().isCreated());
        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Alte Klausur 2", "examDate": "2020-02-01T10:00", "grade": 2.0}
                """.formatted(db)).andExpect(status().isCreated());

        getJson("/api/dashboard", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCount").value(2))
                .andExpect(jsonPath("$.totalTasks").value(3))
                .andExpect(jsonPath("$.openTasks").value(2))
                .andExpect(jsonPath("$.doneTasks").value(1))
                .andExpect(jsonPath("$.overdueTasks").value(1))
                .andExpect(jsonPath("$.studyMinutesLast7Days").value(120))
                .andExpect(jsonPath("$.averageGrade").value(1.65))
                .andExpect(jsonPath("$.todaysCourses", hasSize(1)))
                .andExpect(jsonPath("$.todaysCourses[0].name").value("Mathe"))
                .andExpect(jsonPath("$.upcomingExams", hasSize(1)))
                .andExpect(jsonPath("$.dueSoonTasks", hasSize(2)))
                .andExpect(jsonPath("$.dailyMinutes", hasSize(7)))
                .andExpect(jsonPath("$.dailyMinutes[6].minutes").value(90))
                .andExpect(jsonPath("$.minutesPerCourse", hasSize(2)))
                .andExpect(jsonPath("$.minutesPerCourse[0].courseName").value("Mathe"))
                .andExpect(jsonPath("$.minutesPerCourse[0].minutes").value(120))
                .andExpect(jsonPath("$.taskProgress", hasSize(2)))
                .andExpect(jsonPath("$.taskProgress[0].courseName").value("Datenbanken"))
                .andExpect(jsonPath("$.taskProgress[1].total").value(2))
                .andExpect(jsonPath("$.taskProgress[1].done").value(1));

        // Der schlanke Endpunkt für "Lernfortschritt" liefert dieselben Lern-Statistiken – und nur diese
        getJson("/api/dashboard/progress", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyMinutesLast7Days").value(120))
                .andExpect(jsonPath("$.dailyMinutes", hasSize(7)))
                .andExpect(jsonPath("$.minutesPerCourse[0].minutes").value(120))
                .andExpect(jsonPath("$.taskProgress", hasSize(2)))
                .andExpect(jsonPath("$.upcomingExams").doesNotExist())
                .andExpect(jsonPath("$.todaysCourses").doesNotExist());

        // Kurs, dessen Semester schon vorbei ist, erscheint nicht mehr unter "Heute"
        postJson("/api/courses", token, """
                {"name": "Altes Fach", "courseType": "VORLESUNG", "dayOfWeek": "%s",
                 "startTime": "14:00", "endTime": "15:00",
                 "semesterStart": "%s", "semesterEnd": "%s"}
                """.formatted(today.getDayOfWeek(), today.minusDays(60), today.minusDays(1)))
                .andExpect(status().isCreated());
        getJson("/api/dashboard", token).andExpect(jsonPath("$.todaysCourses", hasSize(1)));
    }

    /**
     * Kalender: wöchentliche Vorlesungen, Sortierung und Zeitraumprüfung; globale Suche inkl. Mindestlänge.
     */
    @Test
    void calendarAndSearch() throws Exception {
        String token = registerUser();
        LocalDate monday = LocalDate.of(2030, 4, 1); // ein Montag
        long mathe = createCourse(token, "Mathematik", "MONDAY");
        createTask(token, mathe, "Blatt zur Integration", monday.plusDays(2), "HIGH", "OPEN");
        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Mathe-Klausur", "examDate": "%sT09:00"}
                """.formatted(mathe, monday.plusDays(3))).andExpect(status().isCreated());

        // 2 Wochen: 2 Vorlesungen + 1 Aufgabe + 1 Klausur, sortiert nach Datum
        getJson("/api/calendar?from=" + monday + "&to=" + monday.plusDays(13), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].type").value("LECTURE"))
                .andExpect(jsonPath("$[1].type").value("TASK"))
                .andExpect(jsonPath("$[2].type").value("EXAM"))
                .andExpect(jsonPath("$[3].type").value("LECTURE"));

        getJson("/api/calendar?from=2030-04-10&to=2030-04-01", token).andExpect(status().isBadRequest());
        getJson("/api/calendar?from=2030-01-01&to=2030-12-31", token).andExpect(status().isBadRequest());

        getJson("/api/search?q=mathe", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses", hasSize(1)))
                .andExpect(jsonPath("$.exams", hasSize(1)))
                .andExpect(jsonPath("$.tasks", hasSize(0)));
        getJson("/api/search?q=integration", token).andExpect(jsonPath("$.tasks", hasSize(1)));
        getJson("/api/search?q=m", token).andExpect(jsonPath("$.courses", hasSize(0)));
    }
}

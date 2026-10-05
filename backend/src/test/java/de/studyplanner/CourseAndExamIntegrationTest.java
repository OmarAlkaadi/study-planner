package de.studyplanner;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstests für Kurse und Klausuren: CRUD, Validierung, Filter und Datenisolation zwischen Benutzern.
 */
class CourseAndExamIntegrationTest extends IntegrationTestBase {

    /** Anlegen, Lesen, Ändern (Standardfarbe), Suchen und Löschen eines Kurses. */
    @Test
    void courseCrud() throws Exception {
        String token = registerUser();
        long id = createCourse(token, "Mathematik 1", "MONDAY");

        getJson("/api/courses", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Mathematik 1"))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));

        putJson("/api/courses/" + id, token, """
                {"name": "Mathematik 2", "courseType": "UEBUNG", "dayOfWeek": "FRIDAY",
                 "startTime": "10:00", "endTime": "11:30"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mathematik 2"))
                .andExpect(jsonPath("$.color").value("#4f46e5"));

        getJson("/api/courses?q=mathe", token).andExpect(jsonPath("$", hasSize(1)));
        getJson("/api/courses?q=physik", token).andExpect(jsonPath("$", hasSize(0)));

        deleteRequest("/api/courses/" + id, token).andExpect(status().isNoContent());
        getJson("/api/courses/" + id, token).andExpect(status().isNotFound());
    }

    /** Feldfehler (leerer Name, falsche Farbe) und fachlicher Fehler (Endzeit vor Startzeit) → 400. */
    @Test
    void invalidCourseReturns400() throws Exception {
        String token = registerUser();
        postJson("/api/courses", token, """
                {"name": "", "courseType": "VORLESUNG", "dayOfWeek": "MONDAY",
                 "startTime": "08:00", "endTime": "09:00", "color": "blau"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.color", notNullValue()));

        postJson("/api/courses", token, """
                {"name": "Test", "courseType": "VORLESUNG", "dayOfWeek": "MONDAY",
                 "startTime": "10:00", "endTime": "09:00"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Die Endzeit muss nach der Startzeit liegen."));
    }

    /** Ein Benutzer sieht, ändert und verknüpft keine Daten eines anderen Benutzers (immer 404). */
    @Test
    void usersCannotSeeDataOfOtherUsers() throws Exception {
        String alice = registerUser();
        String bob = registerUser();
        long aliceCourse = createCourse(alice, "Datenbanken", "TUESDAY");

        getJson("/api/courses", bob).andExpect(jsonPath("$", hasSize(0)));
        getJson("/api/courses/" + aliceCourse, bob).andExpect(status().isNotFound());
        deleteRequest("/api/courses/" + aliceCourse, bob).andExpect(status().isNotFound());

        // Bob darf auch keine Klausur für Alices Kurs anlegen
        postJson("/api/exams", bob, """
                {"courseId": %d, "title": "Hack", "examDate": "2030-01-10T09:00"}
                """.formatted(aliceCourse))
                .andExpect(status().isNotFound());
    }

    /** Klausuren anlegen, nach anstehend/vergangen filtern, Notenprüfung und Löschen per CASCADE. */
    @Test
    void examCrudAndFilter() throws Exception {
        String token = registerUser();
        long courseId = createCourse(token, "Datenbanken", "WEDNESDAY");

        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Klausur DB", "examDate": "2099-02-10T09:00", "room": "Audimax"}
                """.formatted(courseId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseName").value("Datenbanken"))
                .andExpect(jsonPath("$.examDate", startsWith("2099-02-10T09:00")));

        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Alte Klausur", "examDate": "2020-02-10T09:00", "grade": 1.7}
                """.formatted(courseId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.grade").value(1.7));

        getJson("/api/exams?upcoming=true", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Klausur DB"));
        getJson("/api/exams?upcoming=false", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Alte Klausur"));

        // Suche (per SQL LIKE) in Titel und Kursname, sortiert nach Datum; Filter lassen sich kombinieren
        long otherCourse = createCourse(token, "Physik", "FRIDAY");
        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Physik-Klausur", "examDate": "2098-01-01T09:00"}
                """.formatted(otherCourse)).andExpect(status().isCreated());
        getJson("/api/exams", token)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Alte Klausur"))
                .andExpect(jsonPath("$[2].title").value("Klausur DB"));
        getJson("/api/exams?q=DATENBANK", token).andExpect(jsonPath("$", hasSize(2)));
        getJson("/api/exams?q=alte", token).andExpect(jsonPath("$", hasSize(1)));
        getJson("/api/exams?courseId=" + otherCourse, token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Physik-Klausur"));
        getJson("/api/exams?courseId=" + courseId + "&upcoming=true&q=klausur", token)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Klausur DB"));
        // Sonderzeichen wie % werden wörtlich gesucht, nicht als Platzhalter
        getJson("/api/exams?q=%25", token).andExpect(jsonPath("$", hasSize(0)));

        postJson("/api/exams", token, """
                {"courseId": %d, "title": "Falsche Note", "examDate": "2020-02-10T09:00", "grade": 6.0}
                """.formatted(courseId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.grade", notNullValue()));

        // Kurs löschen löscht auch seine Klausuren (ON DELETE CASCADE)
        deleteRequest("/api/courses/" + courseId, token).andExpect(status().isNoContent());
        getJson("/api/exams", token).andExpect(jsonPath("$", hasSize(1)));
    }

    /**
     * Semesterzeitraum: wird gespeichert, Ende vor Beginn → 400, und der Kalender zeigt
     * Vorlesungen nur innerhalb des Zeitraums.
     */
    @Test
    void courseSemesterLimitsCalendar() throws Exception {
        String token = registerUser();
        postJson("/api/courses", token, """
                {"name": "Analysis", "courseType": "VORLESUNG", "dayOfWeek": "MONDAY",
                 "startTime": "08:00", "endTime": "09:30",
                 "semesterStart": "2030-04-08", "semesterEnd": "2030-04-22"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.semesterStart").value("2030-04-08"))
                .andExpect(jsonPath("$.semesterEnd").value("2030-04-22"));

        // April 2030: Montage sind 1., 8., 15., 22. und 29. → nur 8., 15. und 22. liegen im Semester
        getJson("/api/calendar?from=2030-04-01&to=2030-04-30", token)
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].date").value("2030-04-08"))
                .andExpect(jsonPath("$[2].date").value("2030-04-22"));

        postJson("/api/courses", token, """
                {"name": "Falsch", "courseType": "VORLESUNG", "dayOfWeek": "MONDAY",
                 "startTime": "08:00", "endTime": "09:30",
                 "semesterStart": "2030-04-22", "semesterEnd": "2030-04-08"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Das Semesterende darf nicht vor dem Semesterbeginn liegen."));
    }
}

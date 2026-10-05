package de.studyplanner.dashboard;

import java.util.List;

/**
 * Antwort für GET /api/dashboard/progress: nur die Lern-Statistiken für die Seite "Lernfortschritt"
 * (Lernzeit der letzten 7 Tage, Lernzeit pro Kurs, Aufgabenfortschritt pro Kurs).
 */
public record ProgressResponse(
        long studyMinutesLast7Days,
        List<DailyMinutes> dailyMinutes,
        List<CourseMinutes> minutesPerCourse,
        List<CourseTaskProgress> taskProgress
) {
}

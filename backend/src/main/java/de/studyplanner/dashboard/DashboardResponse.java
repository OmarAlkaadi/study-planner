package de.studyplanner.dashboard;

import de.studyplanner.course.CourseResponse;
import de.studyplanner.exam.ExamResponse;
import de.studyplanner.task.TaskResponse;

import java.util.List;

/**
 * Gesamte Antwort für das Dashboard: Kennzahlen (Anzahl Kurse/Aufgaben, Lernzeit, Notenschnitt),
 * heutige Kurse, nächste Klausuren, bald fällige Aufgaben und die Daten für die Diagramme.
 * Wird auch von der Seite "Lernfortschritt" im Frontend benutzt.
 */
public record DashboardResponse(
        long courseCount,
        long totalTasks,
        long openTasks,
        long doneTasks,
        long overdueTasks,
        long studyMinutesLast7Days,
        Double averageGrade,
        List<CourseResponse> todaysCourses,
        List<ExamResponse> upcomingExams,
        List<TaskResponse> dueSoonTasks,
        List<DailyMinutes> dailyMinutes,
        List<CourseMinutes> minutesPerCourse,
        List<CourseTaskProgress> taskProgress
) {
}

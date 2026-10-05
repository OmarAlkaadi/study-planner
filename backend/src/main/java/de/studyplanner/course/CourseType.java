package de.studyplanner.course;

/**
 * Art der Veranstaltung. Wird als Text in der Spalte course_type gespeichert;
 * ein CHECK-Constraint in der Datenbank erlaubt genau diese Werte.
 */
public enum CourseType {
    VORLESUNG,
    UEBUNG,
    PRAKTIKUM,
    SEMINAR,
    SONSTIGES
}

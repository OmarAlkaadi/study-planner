package de.studyplanner.course;

import de.studyplanner.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * JPA-Entity für die Tabelle "courses": eine wöchentlich stattfindende Lehrveranstaltung
 * (Vorlesung, Übung, Praktikum, ...) mit Wochentag, Uhrzeit, Raum, Dozent, ECTS und Farbe.
 *
 * Beziehungen: gehört genau einem User; Klausuren und Lerneinheiten gehören zu einem Kurs
 * (werden beim Löschen per ON DELETE CASCADE mitgelöscht), Aufgaben optional (course_id wird NULL).
 */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Besitzer des Kurses. LAZY: Der User wird erst aus der DB geladen, wenn man wirklich darauf zugreift.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    private String lecturer;

    private String room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseType courseType;

    /** Wochentag, an dem der Kurs jede Woche stattfindet (als Text gespeichert, z. B. "MONDAY"). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private Integer ects;

    private String color;

    /** Erster Tag, an dem der Kurs stattfindet (optional – null = unbegrenzt). */
    private LocalDate semesterStart;

    /** Letzter Tag, an dem der Kurs stattfindet (optional – null = unbegrenzt). */
    private LocalDate semesterEnd;

    /**
     * true, wenn der Kurs an diesem Datum stattfindet: richtiger Wochentag und innerhalb des Semesterzeitraums.
     * Wird vom Kalender und vom Dashboard ("Heute") benutzt.
     */
    public boolean takesPlaceOn(LocalDate date) {
        return date.getDayOfWeek() == dayOfWeek
                && (semesterStart == null || !date.isBefore(semesterStart))
                && (semesterEnd == null || !date.isAfter(semesterEnd));
    }

    // Getter und Setter (werden von JPA, den Services und den DTOs benutzt)
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLecturer() {
        return lecturer;
    }

    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public CourseType getCourseType() {
        return courseType;
    }

    public void setCourseType(CourseType courseType) {
        this.courseType = courseType;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Integer getEcts() {
        return ects;
    }

    public void setEcts(Integer ects) {
        this.ects = ects;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDate getSemesterStart() {
        return semesterStart;
    }

    public void setSemesterStart(LocalDate semesterStart) {
        this.semesterStart = semesterStart;
    }

    public LocalDate getSemesterEnd() {
        return semesterEnd;
    }

    public void setSemesterEnd(LocalDate semesterEnd) {
        this.semesterEnd = semesterEnd;
    }
}

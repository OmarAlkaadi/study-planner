package de.studyplanner.task;

import de.studyplanner.course.Course;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA-Entity für die Tabelle "tasks": eine Aufgabe mit Deadline, Priorität und Status
 * (z. B. "Übungsblatt 4 abgeben"). Der Kurs ist optional; wird er gelöscht, bleibt die Aufgabe
 * erhalten und course_id wird NULL (ON DELETE SET NULL).
 * Der Status wird nur über changeStatus() geändert, damit completedAt immer stimmt.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Optional: eine Aufgabe kann auch ohne Kurs existieren. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    /** Setzt das Erstellungsdatum automatisch vor dem ersten Speichern. */
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Setzt den Status und pflegt dabei automatisch das Erledigt-Datum. */
    public void changeStatus(TaskStatus newStatus) {
        if (newStatus == TaskStatus.DONE && status != TaskStatus.DONE) {
            completedAt = LocalDateTime.now();
        } else if (newStatus != TaskStatus.DONE) {
            completedAt = null;
        }
        status = newStatus;
    }

    /**
     * Überfällig = nicht erledigt und Deadline liegt vor heute. Wird nicht gespeichert, sondern berechnet.
     */
    public boolean isOverdue() {
        return status != TaskStatus.DONE && dueDate.isBefore(LocalDate.now());
    }

    // Getter und Setter (für status gibt es bewusst keinen Setter, siehe changeStatus)
    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}

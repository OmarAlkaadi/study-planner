-- ============================================================
-- Semesterzeitraum für Kurse
-- Vorlesungen finden nur zwischen semester_start und semester_end
-- statt (z. B. nicht in den Semesterferien). Beide Felder sind
-- optional: NULL bedeutet "unbegrenzt" (bisheriges Verhalten).
-- ============================================================

ALTER TABLE courses ADD COLUMN semester_start DATE;
ALTER TABLE courses ADD COLUMN semester_end DATE;

ALTER TABLE courses ADD CONSTRAINT chk_courses_semester
    CHECK (semester_start IS NULL OR semester_end IS NULL OR semester_end >= semester_start);

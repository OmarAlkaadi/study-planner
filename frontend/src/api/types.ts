// Typen passend zu den DTOs des Spring-Boot-Backends.
// Datumswerte kommen als Text: LocalDate = "2026-10-05", LocalTime = "08:15:00",
// LocalDateTime = "2026-10-05T09:00:00".

// Aufzählungen – entsprechen genau den Java-Enums im Backend
export type CourseType = 'VORLESUNG' | 'UEBUNG' | 'PRAKTIKUM' | 'SEMINAR' | 'SONSTIGES';
export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE';
export type CalendarEventType = 'LECTURE' | 'EXAM' | 'TASK';

/** Angemeldeter Benutzer (UserResponse). */
export interface User {
  id: number;
  email: string;
  displayName: string;
  /** true beim öffentlichen Demo-Account – seine Kontodaten sind gesperrt */
  demo: boolean;
}

/** Antwort von Login/Registrierung: Token + Benutzer. */
export interface AuthResponse {
  token: string;
  user: User;
}

/** Einfache Antwort mit einer Meldung, z. B. nach der Registrierung oder bei "Passwort vergessen". */
export interface MessageResponse {
  message: string;
}

/** Kurs, wie ihn das Backend liefert (CourseResponse). */
export interface Course {
  id: number;
  name: string;
  lecturer: string | null;
  room: string | null;
  courseType: CourseType;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  ects: number | null;
  color: string | null;
  /** Semesterzeitraum als "2026-10-12" – null = unbegrenzt */
  semesterStart: string | null;
  semesterEnd: string | null;
}

/** Daten zum Anlegen/Ändern eines Kurses (CourseRequest). */
export interface CourseInput {
  name: string;
  lecturer: string | null;
  room: string | null;
  courseType: CourseType;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  ects: number | null;
  color: string;
  semesterStart: string | null;
  semesterEnd: string | null;
}

/** Klausur inkl. Kursname und -farbe (ExamResponse). */
export interface Exam {
  id: number;
  courseId: number;
  courseName: string;
  courseColor: string | null;
  title: string;
  examDate: string;
  room: string | null;
  notes: string | null;
  grade: number | null;
}

/** Daten zum Anlegen/Ändern einer Klausur (ExamRequest). */
export interface ExamInput {
  courseId: number;
  title: string;
  examDate: string;
  room: string | null;
  notes: string | null;
  grade: number | null;
}

/** Aufgabe inkl. Kursdaten und berechnetem overdue (TaskResponse). */
export interface Task {
  id: number;
  title: string;
  description: string | null;
  courseId: number | null;
  courseName: string | null;
  courseColor: string | null;
  dueDate: string;
  priority: TaskPriority;
  status: TaskStatus;
  createdAt: string;
  completedAt: string | null;
  overdue: boolean;
}

/** Daten zum Anlegen/Ändern einer Aufgabe (TaskRequest). */
export interface TaskInput {
  title: string;
  description: string | null;
  courseId: number | null;
  dueDate: string;
  priority: TaskPriority;
  status: TaskStatus;
}

/** Lerneinheit inkl. Kursname und -farbe (StudySessionResponse). */
export interface StudySession {
  id: number;
  courseId: number;
  courseName: string;
  courseColor: string | null;
  sessionDate: string;
  durationMinutes: number;
  topic: string | null;
}

/** Daten zum Anlegen/Ändern einer Lerneinheit (StudySessionRequest). */
export interface StudySessionInput {
  courseId: number;
  sessionDate: string;
  durationMinutes: number;
  topic: string | null;
}

/** Summe der Lernminuten pro Kurs (Diagramm). */
export interface CourseMinutes {
  courseId: number;
  courseName: string;
  color: string | null;
  minutes: number;
}

/** Summe der Lernminuten pro Tag (Diagramm). */
export interface DailyMinutes {
  date: string;
  minutes: number;
}

/** Erledigte von allen Aufgaben pro Kurs (Fortschrittsbalken). */
export interface CourseTaskProgress {
  courseId: number;
  courseName: string;
  color: string | null;
  total: number;
  done: number;
}

/** Gesamte Antwort von GET /api/dashboard (DashboardResponse). */
export interface Dashboard {
  courseCount: number;
  totalTasks: number;
  openTasks: number;
  doneTasks: number;
  overdueTasks: number;
  studyMinutesLast7Days: number;
  averageGrade: number | null;
  todaysCourses: Course[];
  upcomingExams: Exam[];
  dueSoonTasks: Task[];
  dailyMinutes: DailyMinutes[];
  minutesPerCourse: CourseMinutes[];
  taskProgress: CourseTaskProgress[];
}

/** Nur die Lern-Statistiken für die Seite "Lernfortschritt" (GET /api/dashboard/progress). */
export interface ProgressStats {
  studyMinutesLast7Days: number;
  dailyMinutes: DailyMinutes[];
  minutesPerCourse: CourseMinutes[];
  taskProgress: CourseTaskProgress[];
}

/** Ein Eintrag im Kalender: Veranstaltung, Klausur oder Deadline. */
export interface CalendarEvent {
  key: string;
  type: CalendarEventType;
  refId: number;
  title: string;
  date: string;
  startTime: string | null;
  endTime: string | null;
  color: string | null;
  courseName: string | null;
  location: string | null;
  done: boolean;
}

/** Ergebnis der globalen Suche, getrennt nach Typ. */
export interface SearchResult {
  courses: Course[];
  exams: Exam[];
  tasks: Task[];
}

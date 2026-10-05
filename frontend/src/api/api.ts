// Alle Backend-Endpunkte als typisierte Funktionen, gruppiert nach Bereich.
// Die Seiten rufen z. B. courseApi.list() auf, statt selbst URLs zusammenzubauen.
// Jede Funktion nutzt request() aus client.ts (JWT anhängen, Fehler umwandeln) und liefert
// ein Promise mit dem passenden Typ aus types.ts.

import { queryString, request } from './client';
import type {
  AuthResponse,
  CalendarEvent,
  Course,
  CourseInput,
  Dashboard,
  Exam,
  ExamInput,
  MessageResponse,
  ProgressStats,
  SearchResult,
  StudySession,
  StudySessionInput,
  Task,
  TaskInput,
  TaskPriority,
  TaskStatus,
  User,
} from './types';

/**
 * Registrierung, Login, Abmelden, E-Mail-Bestätigung, "Passwort vergessen" und aktueller Benutzer (/api/auth).
 * register und verifyEmail liefern nur eine Meldung – angemeldet wird ausschließlich über login.
 * login setzt zusätzlich das Refresh-Token-Cookie; das Erneuern übernimmt refreshSession() in client.ts.
 */
export const authApi = {
  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: { email, password } }),
  register: (email: string, password: string, displayName: string) =>
    request<MessageResponse>('/auth/register', { method: 'POST', body: { email, password, displayName } }),
  verifyEmail: (token: string) =>
    request<MessageResponse>('/auth/verify-email', { method: 'POST', body: { token } }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
  resendVerification: (email: string) =>
    request<MessageResponse>('/auth/resend-verification', { method: 'POST', body: { email } }),
  forgotPassword: (email: string) =>
    request<MessageResponse>('/auth/forgot-password', { method: 'POST', body: { email } }),
  resetPassword: (token: string, password: string) =>
    request<MessageResponse>('/auth/reset-password', { method: 'POST', body: { token, password } }),
  me: () => request<User>('/auth/me'),
};

/** Eigenes Konto: Namen ändern, Passwort ändern, Konto löschen (/api/account). */
export const accountApi = {
  updateProfile: (displayName: string) =>
    request<User>('/account/profile', { method: 'PUT', body: { displayName } }),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<void>('/account/password', { method: 'PUT', body: { currentPassword, newPassword } }),
  deleteAccount: (password: string) => request<void>('/account', { method: 'DELETE', body: { password } }),
};

/** Kurse: Liste (optional mit Suchtext), anlegen, ändern, löschen (/api/courses). */
export const courseApi = {
  list: (q?: string) => request<Course[]>(`/courses${queryString({ q })}`),
  create: (input: CourseInput) => request<Course>('/courses', { method: 'POST', body: input }),
  update: (id: number, input: CourseInput) => request<Course>(`/courses/${id}`, { method: 'PUT', body: input }),
  remove: (id: number) => request<void>(`/courses/${id}`, { method: 'DELETE' }),
};

/** Filter für die Klausurliste – leere Werte werden nicht an die URL angehängt. */
export interface ExamFilter {
  courseId?: number | null;
  upcoming?: boolean | null;
  q?: string;
}

/** Klausuren: gefilterte Liste, anlegen, ändern, löschen (/api/exams). */
export const examApi = {
  list: (filter: ExamFilter = {}) => request<Exam[]>(`/exams${queryString({ ...filter })}`),
  create: (input: ExamInput) => request<Exam>('/exams', { method: 'POST', body: input }),
  update: (id: number, input: ExamInput) => request<Exam>(`/exams/${id}`, { method: 'PUT', body: input }),
  remove: (id: number) => request<void>(`/exams/${id}`, { method: 'DELETE' }),
};

/** Filter für die Aufgabenliste ('' = Filter nicht gesetzt). */
export interface TaskFilter {
  status?: TaskStatus | '';
  priority?: TaskPriority | '';
  courseId?: number | null;
  q?: string;
}

/** Aufgaben (/api/tasks). updateStatus ändert nur den Status (PATCH) – für die Checkbox "erledigt". */
export const taskApi = {
  list: (filter: TaskFilter = {}) => request<Task[]>(`/tasks${queryString({ ...filter })}`),
  create: (input: TaskInput) => request<Task>('/tasks', { method: 'POST', body: input }),
  update: (id: number, input: TaskInput) => request<Task>(`/tasks/${id}`, { method: 'PUT', body: input }),
  updateStatus: (id: number, status: TaskStatus) =>
    request<Task>(`/tasks/${id}/status`, { method: 'PATCH', body: { status } }),
  remove: (id: number) => request<void>(`/tasks/${id}`, { method: 'DELETE' }),
};

/** Lerneinheiten: Liste (optional pro Kurs), anlegen, ändern, löschen. */
export const studySessionApi = {
  list: (courseId?: number | null) => request<StudySession[]>(`/study-sessions${queryString({ courseId })}`),
  create: (input: StudySessionInput) =>
    request<StudySession>('/study-sessions', { method: 'POST', body: input }),
  update: (id: number, input: StudySessionInput) =>
    request<StudySession>(`/study-sessions/${id}`, { method: 'PUT', body: input }),
  remove: (id: number) => request<void>(`/study-sessions/${id}`, { method: 'DELETE' }),
};

/** Dashboard (alle Statistiken in einer Anfrage) und nur die Lern-Statistiken für "Lernfortschritt". */
export const dashboardApi = {
  get: () => request<Dashboard>('/dashboard'),
  progress: () => request<ProgressStats>('/dashboard/progress'),
};

/** Kalendereinträge im Zeitraum from–to (Datum als "2026-10-01"). */
export const calendarApi = {
  events: (from: string, to: string) => request<CalendarEvent[]>(`/calendar${queryString({ from, to })}`),
};

/** Globale Suche über Kurse, Klausuren und Aufgaben. */
export const searchApi = {
  search: (q: string) => request<SearchResult>(`/search${queryString({ q })}`),
};

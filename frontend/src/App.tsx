import { BrowserRouter, Navigate, Route, Routes } from 'react-router';
import { AuthProvider } from './auth/AuthProvider';
import { ProtectedLayout } from './components/Layout';
import { DialogProvider } from './dialogs/DialogProvider';
import { AccountPage } from './pages/AccountPage';
import { AuthPage } from './pages/AuthPage';
import { CalendarPage } from './pages/CalendarPage';
import { CoursesPage } from './pages/CoursesPage';
import { DashboardPage } from './pages/DashboardPage';
import { ExamsPage } from './pages/ExamsPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ProgressPage } from './pages/ProgressPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { SearchPage } from './pages/SearchPage';
import { TasksPage } from './pages/TasksPage';
import { VerifyEmailPage } from './pages/VerifyEmailPage';

/**
 * Wurzelkomponente: legt fest, welche URL welche Seite anzeigt (Routing).
 *
 * - BrowserRouter: Seitenwechsel über die URL, ohne die Seite neu zu laden
 * - AuthProvider: stellt den Login-Zustand für alle Seiten bereit (useAuth)
 * - DialogProvider: eigene Dialoge für Sicherheitsabfragen und Fehlermeldungen (useDialogs)
 * - Öffentlich: /login, /register sowie die Seiten hinter den Links aus den E-Mails
 *   (/verify-email, /reset-password) und /forgot-password
 * - Alle anderen Seiten liegen in ProtectedLayout: ohne Login → Weiterleitung zu /login
 * - Unbekannte URLs → Weiterleitung zum Dashboard ("/")
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <DialogProvider>
          <Routes>
            {/* key: beim Wechsel zwischen Login und Registrierung wird das Formular neu erzeugt (Felder geleert) */}
            <Route path="/login" element={<AuthPage key="login" mode="login" />} />
            <Route path="/register" element={<AuthPage key="register" mode="register" />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
            {/* Geschützte Seiten: werden im Layout (Navigation + Suchleiste) an der Stelle <Outlet /> angezeigt */}
            <Route element={<ProtectedLayout />}>
              <Route index element={<DashboardPage />} />
              <Route path="calendar" element={<CalendarPage />} />
              <Route path="courses" element={<CoursesPage />} />
              <Route path="exams" element={<ExamsPage />} />
              <Route path="tasks" element={<TasksPage />} />
              <Route path="progress" element={<ProgressPage />} />
              <Route path="search" element={<SearchPage />} />
              <Route path="account" element={<AccountPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </DialogProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

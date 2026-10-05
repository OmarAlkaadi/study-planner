import { useState, type FormEvent } from 'react';
import { NavLink, Navigate, Outlet, useNavigate } from 'react-router';
import { useAuth } from '../auth/useAuth';
import { Icon, type IconName } from './Icon';
import { Loading } from './ui';

/** Einträge der Seitennavigation (Ziel-URL, Beschriftung, Icon). */
const NAV_ITEMS: { to: string; label: string; icon: IconName }[] = [
  { to: '/', label: 'Dashboard', icon: 'dashboard' },
  { to: '/calendar', label: 'Kalender', icon: 'calendar' },
  { to: '/courses', label: 'Vorlesungen', icon: 'book' },
  { to: '/exams', label: 'Klausuren', icon: 'exam' },
  { to: '/tasks', label: 'Aufgaben', icon: 'tasks' },
  { to: '/progress', label: 'Lernfortschritt', icon: 'chart' },
];

/**
 * Rahmen für alle Seiten nach dem Login: Seitennavigation, Suchleiste, Benutzername und Abmelden.
 * Ein Klick auf den Namen oben rechts öffnet die Seite "Konto".
 *
 * Gleichzeitig der "Türsteher" für geschützte Seiten: Während der Login geprüft wird → Ladeanzeige,
 * ohne Benutzer → Weiterleitung zu /login. An der Stelle <Outlet /> setzt React Router die
 * jeweilige Seite ein (Dashboard, Kalender, ...).
 */
export function ProtectedLayout() {
  const { user, loading, logout } = useAuth();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  if (loading) {
    return (
      <div className="center-screen">
        <Loading />
      </div>
    );
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Suche abschicken → zur Suchseite wechseln; der Suchtext steht in der URL (?q=...)
  const onSearch = (event: FormEvent) => {
    event.preventDefault();
    const q = search.trim();
    if (q) navigate(`/search?q=${encodeURIComponent(q)}`);
  };

  return (
    <div className="app-shell">
      {/* Seitenleiste: Logo und Navigation (NavLink markiert den aktiven Eintrag automatisch) */}
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">SP</span>
          <span className="brand-name">Study Planner</span>
        </div>
        <nav className="nav">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.to === '/'} className="nav-link">
              <Icon name={item.icon} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="main">
        {/* Obere Leiste: globale Suche, Benutzername (Link zur Konto-Seite), Abmelden */}
        <div className="topbar">
          <form className="search-form" onSubmit={onSearch} role="search">
            <Icon name="search" />
            <input
              type="search"
              placeholder="Kurse, Klausuren, Aufgaben suchen …"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              aria-label="Suche"
            />
          </form>
          <div className="user-box">
            {/* Auf dem Handy wird der Name ausgeblendet – das Icon bleibt als Link sichtbar */}
            <NavLink to="/account" className="user-link" title="Konto" aria-label={`Konto von ${user.displayName}`}>
              <Icon name="user" />
              <span className="user-name">{user.displayName}</span>
            </NavLink>
            <button type="button" className="icon-button" onClick={logout} title="Abmelden" aria-label="Abmelden">
              <Icon name="logout" />
            </button>
          </div>
        </div>
        <main className="content">
          {/* Hier erscheint die aktuelle Seite */}
          <Outlet />
        </main>
      </div>
    </div>
  );
}

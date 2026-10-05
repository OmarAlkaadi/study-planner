import { useCallback, useState } from 'react';
import { courseApi, taskApi } from '../api/api';
import { errorMessage } from '../api/client';
import type { Task, TaskPriority, TaskStatus } from '../api/types';
import { TaskForm } from '../components/forms/TaskForm';
import { Icon } from '../components/Icon';
import { Modal } from '../components/Modal';
import { ColorDot, EmptyState, ErrorMessage, Loading, PageHeader } from '../components/ui';
import { formatDate, PRIORITY_LABELS, relativeDays, STATUS_LABELS } from '../utils/format';
import { useApiData, useDebouncedValue } from '../utils/hooks';
import { useDialogs } from '../dialogs/useDialogs';

/**
 * Seite "Aufgaben": Liste mit Checkbox zum Abhaken, Filter (Status, Priorität, Kurs, Suchtext)
 * und Dialog zum Anlegen/Bearbeiten. Die Filter gehen als Query-Parameter an GET /api/tasks
 * und werden in der Datenbank ausgewertet.
 */
export function TasksPage() {
  // Eigene Dialoge statt der Browser-Fenster (siehe DialogProvider)
  const dialogs = useDialogs();
  const [status, setStatus] = useState<TaskStatus | ''>('');
  const [priority, setPriority] = useState<TaskPriority | ''>('');
  const [courseId, setCourseId] = useState('');
  const [search, setSearch] = useState('');
  const query = useDebouncedValue(search.trim());
  const [editing, setEditing] = useState<{ task: Task | null } | null>(null);

  const coursesLoader = useCallback(() => courseApi.list(), []);
  const { data: courses } = useApiData(coursesLoader);

  // Jede Filteränderung erzeugt einen neuen loader → automatisches Neuladen
  const tasksLoader = useCallback(
    () => taskApi.list({ status, priority, courseId: courseId === '' ? null : Number(courseId), q: query }),
    [status, priority, courseId, query],
  );
  const { data: tasks, error, loading, reload, setData: setTasks } = useApiData(tasksLoader);

  // Ist ein Filter aktiv? Steuert den Button "Filter zurücksetzen" und den Text bei leerer Liste
  const hasFilter = status !== '' || priority !== '' || courseId !== '' || query !== '';

  // Checkbox: zwischen erledigt und offen umschalten
  const toggleDone = async (task: Task) => {
    const newStatus: TaskStatus = task.status === 'DONE' ? 'OPEN' : 'DONE';
    // Optimistisch: Haken sofort anzeigen, nicht erst nach der Serverantwort
    setTasks((current) => current?.map((t) => (t.id === task.id ? { ...t, status: newStatus } : t)) ?? null);
    try {
      await taskApi.updateStatus(task.id, newStatus);
    } catch (err) {
      dialogs.showError(errorMessage(err));
    }
    await reload();
  };

  // Löschen mit Sicherheitsabfrage, danach Liste neu laden
  const onDelete = async (task: Task) => {
    const confirmed = await dialogs.confirm({
      title: 'Aufgabe löschen?',
      message: `„${task.title}“ wirklich löschen?`,
      confirmLabel: 'Löschen',
      danger: true,
    });
    if (!confirmed) return;
    try {
      await taskApi.remove(task.id);
      await reload();
    } catch (err) {
      dialogs.showError(errorMessage(err));
    }
  };

  // Nach dem Speichern: Dialog schließen und Liste neu laden
  const onSaved = () => {
    setEditing(null);
    void reload();
  };

  // Alle Filter zurücksetzen
  const resetFilters = () => {
    setStatus('');
    setPriority('');
    setCourseId('');
    setSearch('');
  };

  return (
    <>
      <PageHeader
        title="Aufgaben"
        subtitle="Übungsblätter, Abgaben und To-dos mit Deadline"
        actions={
          <button type="button" className="button" onClick={() => setEditing({ task: null })}>
            <Icon name="plus" /> Neue Aufgabe
          </button>
        }
      />

      {/* Filterleiste */}
      <div className="toolbar">
        <select value={status} onChange={(e) => setStatus(e.target.value as TaskStatus | '')} aria-label="Status">
          <option value="">Alle Status</option>
          {Object.entries(STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <select
          value={priority}
          onChange={(e) => setPriority(e.target.value as TaskPriority | '')}
          aria-label="Priorität"
        >
          <option value="">Alle Prioritäten</option>
          {Object.entries(PRIORITY_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <select value={courseId} onChange={(e) => setCourseId(e.target.value)} aria-label="Kurs">
          <option value="">Alle Kurse</option>
          {courses?.map((course) => (
            <option key={course.id} value={course.id}>
              {course.name}
            </option>
          ))}
        </select>
        <input
          type="search"
          className="toolbar-search"
          placeholder="In Titel und Beschreibung suchen …"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Aufgaben durchsuchen"
        />
        {hasFilter && (
          <button type="button" className="button secondary small" onClick={resetFilters}>
            Filter zurücksetzen
          </button>
        )}
      </div>

      <ErrorMessage message={error} />
      {!tasks && loading && <Loading />}

      {tasks && tasks.length === 0 && (
        <EmptyState
          title={hasFilter ? 'Keine passenden Aufgaben' : 'Noch keine Aufgaben'}
          text={hasFilter ? 'Ändere die Filter oder setze sie zurück.' : 'Lege deine erste Aufgabe an.'}
        />
      )}

      {/* Aufgabenliste – erledigte stehen am Ende (sortiert vom Backend) */}
      {tasks && tasks.length > 0 && (
        <ul className="task-list">
          {tasks.map((task) => (
            <li key={task.id} className={task.status === 'DONE' ? 'task-item done' : 'task-item'}>
              <input
                type="checkbox"
                className="checkbox"
                checked={task.status === 'DONE'}
                onChange={() => toggleDone(task)}
                aria-label={`${task.title} erledigt`}
              />
              <div className="task-main">
                <div className="task-title-row">
                  <strong className="task-title">{task.title}</strong>
                  <span className={`badge priority-${task.priority.toLowerCase()}`}>
                    {PRIORITY_LABELS[task.priority]}
                  </span>
                  {task.status === 'IN_PROGRESS' && <span className="badge info">In Arbeit</span>}
                </div>
                {task.description && <p className="small muted task-description">{task.description}</p>}
                <div className="task-meta small">
                  <span className={task.overdue ? 'danger-text' : 'muted'}>
                    {task.overdue ? 'Überfällig · ' : ''}
                    {formatDate(task.dueDate)} ({relativeDays(task.dueDate)})
                  </span>
                  {task.courseName && (
                    <span className="with-dot muted">
                      <ColorDot color={task.courseColor} />
                      {task.courseName}
                    </span>
                  )}
                </div>
              </div>
              <div className="row-actions">
                <button
                  type="button"
                  className="icon-button"
                  onClick={() => setEditing({ task })}
                  aria-label={`${task.title} bearbeiten`}
                >
                  <Icon name="edit" />
                </button>
                <button
                  type="button"
                  className="icon-button danger"
                  onClick={() => onDelete(task)}
                  aria-label={`${task.title} löschen`}
                >
                  <Icon name="trash" />
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {editing && (
        <Modal title={editing.task ? 'Aufgabe bearbeiten' : 'Neue Aufgabe'} onClose={() => setEditing(null)}>
          <TaskForm
            task={editing.task}
            courses={courses ?? []}
            onSaved={onSaved}
            onCancel={() => setEditing(null)}
          />
        </Modal>
      )}
    </>
  );
}

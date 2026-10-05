// Kleine, selbst gezeichnete Icons (24x24, Strich-Stil), damit keine Icon-Bibliothek nötig ist.

/** SVG-Pfade der Icons (Zeichenbefehle für <path d="...">). */
const PATHS = {
  dashboard: 'M4 4h7v8H4zM13 4h7v5h-7zM13 11h7v9h-7zM4 14h7v6H4z',
  calendar: 'M4 6h16v14H4zM4 10h16M8 3v5M16 3v5',
  book: 'M5 4h10a3 3 0 0 1 3 3v13H8a3 3 0 0 1-3-3zM5 17a3 3 0 0 1 3-3h10',
  exam: 'M7 3h7l4 4v14H7zM14 3v4h4M10 12h5M10 16h5',
  tasks: 'M4 5h4v4H4zM4 15h4v4H4zM11 7h9M11 17h9M5 7l1 1 2-2',
  chart: 'M4 20h16M7 16v-5M12 16V6M17 16v-8',
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20 20l-4-4',
  plus: 'M12 5v14M5 12h14',
  edit: 'M4 20h4L19 9l-4-4L4 16zM13 7l4 4',
  trash: 'M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13',
  logout: 'M14 4h5v16h-5M10 8l-4 4 4 4M6 12h10',
  left: 'M15 5l-7 7 7 7',
  right: 'M9 5l7 7-7 7',
  close: 'M6 6l12 12M18 6L6 18',
  clock: 'M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16zM12 8v4l3 2',
  pin: 'M12 21s-6-6-6-11a6 6 0 0 1 12 0c0 5-6 11-6 11zM12 8a2 2 0 1 0 0 4 2 2 0 0 0 0-4z',
  user: 'M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM4 20a8 8 0 0 1 16 0',
} as const;

/** Erlaubte Icon-Namen – TypeScript meldet Tippfehler wie name="trsh" schon beim Kompilieren. */
export type IconName = keyof typeof PATHS;

/**
 * Zeichnet ein Icon als SVG. stroke="currentColor" übernimmt die Textfarbe des umgebenden Elements;
 * aria-hidden, weil Icons nur Deko sind (die Beschriftung steht am Button).
 */
export function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}

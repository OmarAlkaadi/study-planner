// Konfiguration für Vite (Entwicklungsserver und Build-Werkzeug des Frontends).
// - npm run dev:   startet den Dev-Server auf http://localhost:5173 mit Hot Reload
// - npm run build: erzeugt die fertigen Dateien in dist/ (im Docker-Image liefert nginx sie aus)

import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Im Entwicklungsmodus werden API-Aufrufe an das Spring-Boot-Backend weitergeleitet
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});

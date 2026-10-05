// Einstiegspunkt des Frontends: wird von index.html geladen und hängt die React-App
// in das Element <div id="root"> ein. index.css enthält alle Styles der App.
// StrictMode macht im Entwicklungsmodus zusätzliche Prüfungen (z. B. Effekte doppelt ausführen).

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

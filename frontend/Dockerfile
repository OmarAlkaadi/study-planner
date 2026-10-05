# ---- Build: erzeugt die statischen Dateien mit Vite ----
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Run: nginx liefert die Dateien aus und leitet /api an das Backend weiter ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Gemeinsame Bausteine (Sicherheits-Header, Weiterleitung ans Backend), die nginx.conf einbindet
COPY nginx/ /etc/nginx/snippets/
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

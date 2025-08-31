## Frontend + Nginx Dockerfile (multi-stage)
# Stage 1: build static assets
FROM node:20-alpine AS build
WORKDIR /app
# Copy manifests first for better layer caching
COPY package.json ./
RUN npm install --no-audit --no-fund || true
COPY frontend/. .
RUN npm run build

# Stage 2: lightweight nginx serving the built SPA + reverse proxy config
FROM nginx:1.27-alpine AS runtime
WORKDIR /usr/share/nginx/html

# Copy custom nginx config (expects it at project root under nginx/)
COPY nginx/nginx.conf /etc/nginx/nginx.conf

# Copy build output
COPY --from=build /app/dist .

# Optional: basic healthcheck file (can be probed by container orchestrator)
RUN echo '{"status":"ok"}' > /usr/share/nginx/html/healthz.json

EXPOSE 80

# NOTE: No CMD needed; base nginx image already defines it

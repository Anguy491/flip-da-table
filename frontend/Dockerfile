## Frontend + Nginx Dockerfile (multi-stage)
# Stage 1: build static assets
FROM node:20-alpine AS build
WORKDIR /app
# Copy manifests first for better layer caching (context is ./frontend now)
COPY package.json ./
RUN npm install --no-audit --no-fund || true
COPY . .
RUN npm run build

# Stage 2: lightweight nginx serving the built SPA + reverse proxy config
FROM nginx:1.27-alpine AS runtime
WORKDIR /usr/share/nginx/html

# Inline nginx config (can't copy from parent dir with narrowed build context)
RUN cat > /etc/nginx/nginx.conf <<'EOF'
worker_processes  1;
events { worker_connections 1024; }
http {
	include       mime.types;
	default_type  application/octet-stream;
	sendfile        on;
	keepalive_timeout  65;

	server {
		listen 80;
		server_name _;

		root /usr/share/nginx/html;
		index index.html;

		location /api/ {
			proxy_pass http://backend:8080/api/;
			proxy_set_header Host $host;
			proxy_set_header X-Real-IP $remote_addr;
			proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
			proxy_set_header X-Forwarded-Proto $scheme;
		}

		location /ws {
			proxy_pass http://backend:8080/ws;
			proxy_http_version 1.1;
			proxy_set_header Upgrade $http_upgrade;
			proxy_set_header Connection "Upgrade";
			proxy_set_header Host $host;
		}

		location / {
			try_files $uri /index.html;
			add_header Cache-Control "no-store";
		}
	}
}
EOF

# Copy build output
COPY --from=build /app/dist .

# Optional: basic healthcheck file (can be probed by container orchestrator)
RUN echo '{"status":"ok"}' > /usr/share/nginx/html/healthz.json

EXPOSE 80

# NOTE: No CMD needed; base nginx image already defines it

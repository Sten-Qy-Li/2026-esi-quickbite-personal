# syntax=docker/dockerfile:1

# Build stage -- compile the Vue 3 app to static assets.
FROM node:20-alpine AS build
WORKDIR /app

# Install deps first so the layer can cache across source edits.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .

# Production build uses .env.production (empty VUE_APP_API_BASE_URL)
# so the browser hits /api/** same-origin and nginx proxies to the
# gateway. Override at build time if you need a different target.
ARG VUE_APP_API_BASE_URL=""
ENV VUE_APP_API_BASE_URL=${VUE_APP_API_BASE_URL}
RUN npm run build

# Runtime stage -- serve static assets plus a /api -> gateway proxy.
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# `GATEWAY_UPSTREAM` is substituted into the nginx template at
# container start (nginx:1.27-alpine ships envsubst + the
# /etc/nginx/templates pipeline).
ENV GATEWAY_UPSTREAM=http://api-gateway:8080

EXPOSE 80

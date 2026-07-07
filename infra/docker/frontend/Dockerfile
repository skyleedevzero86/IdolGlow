# syntax=docker/dockerfile:1
# docker-compose 에서 repo 루트(context: ..) 기준으로 빌드합니다.
# FRONTEND_DIR: docs/acc-webzine (Vite) 또는 frontend (Next.js)
# target: static (기본) | next

ARG FRONTEND_DIR=docs/acc-webzine

FROM node:22-bookworm-slim AS builder

WORKDIR /app
ARG FRONTEND_DIR

RUN corepack enable && corepack prepare pnpm@9.15.9 --activate

COPY ${FRONTEND_DIR}/package.json ${FRONTEND_DIR}/pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile

COPY ${FRONTEND_DIR}/ ./
ENV NEXT_TELEMETRY_DISABLED=1
RUN if [ -d dist ] && [ -f dist/index.html ]; then \
      echo "기존 dist 산출물을 사용합니다."; \
    else \
      pnpm build; \
    fi

# --- Vite / 정적 SPA (docs/acc-webzine) ---
FROM nginx:1.27-alpine AS static

COPY infra/docker/frontend/nginx.static.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -qO- http://localhost:80/ >/dev/null || exit 1

# --- Next.js (frontend/) ---
FROM node:22-bookworm-slim AS next-deps

WORKDIR /app
ARG FRONTEND_DIR

RUN corepack enable && corepack prepare pnpm@9.15.9 --activate

COPY ${FRONTEND_DIR}/package.json ${FRONTEND_DIR}/pnpm-lock.yaml* ./
RUN pnpm install --frozen-lockfile --prod

FROM nginx:1.27-alpine AS next

RUN apk add --no-cache curl nodejs npm \
    && npm install -g corepack \
    && corepack enable \
    && corepack prepare pnpm@9.15.9 --activate

COPY infra/docker/frontend/nginx.next.conf /etc/nginx/conf.d/default.conf
COPY infra/docker/frontend/start-next.sh /start.sh
RUN chmod +x /start.sh

COPY --from=builder /app/.next /app/.next
COPY --from=builder /app/public /app/public
COPY --from=builder /app/package.json /app/package.json
COPY --from=next-deps /app/node_modules /app/node_modules

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=127.0.0.1

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost:80/ >/dev/null || exit 1

ENTRYPOINT ["/start.sh"]

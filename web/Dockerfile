FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/web/browser /usr/share/nginx/html
COPY --chmod=755 docker-entrypoint.d/99-gop-runtime.sh /docker-entrypoint.d/99-gop-runtime.sh
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=5 \
	CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1

ARG DOCKER_LIBRARY_REGISTRY=docker.io/library
FROM ${DOCKER_LIBRARY_REGISTRY}/node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi
COPY angular.json tsconfig.json ./
COPY public public
COPY src src
RUN npm run build

FROM ${DOCKER_LIBRARY_REGISTRY}/nginx:1.25-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/portal/browser /usr/share/nginx/html

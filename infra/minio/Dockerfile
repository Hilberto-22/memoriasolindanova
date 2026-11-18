# Build the pinned official sources: legacy MinIO registry images are unavailable.
ARG DOCKER_LIBRARY_REGISTRY=docker.io/library
FROM ${DOCKER_LIBRARY_REGISTRY}/golang:1.24-alpine AS build
ENV CGO_ENABLED=0

FROM build AS minio-build
RUN go install -p 2 github.com/minio/minio@RELEASE.2025-04-22T22-12-26Z \
    && mkdir /licenses \
    && cp /go/pkg/mod/github.com/minio/minio@*/LICENSE /licenses/LICENSE \
    && cp /go/pkg/mod/github.com/minio/minio@*/NOTICE /licenses/NOTICE

FROM build AS mc-build
RUN go install -p 2 github.com/minio/mc@RELEASE.2025-04-16T18-13-26Z \
    && mkdir /licenses \
    && cp /go/pkg/mod/github.com/minio/mc@*/LICENSE /licenses/LICENSE \
    && cp /go/pkg/mod/github.com/minio/mc@*/NOTICE /licenses/NOTICE

FROM ${DOCKER_LIBRARY_REGISTRY}/alpine:3.22 AS runtime
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt

FROM runtime AS minio
COPY --from=minio-build /go/bin/minio /usr/local/bin/minio
COPY --from=minio-build /licenses /licenses
EXPOSE 9000 9001
ENTRYPOINT ["minio"]
CMD ["server", "/data", "--console-address", ":9001"]

FROM runtime AS mc
COPY --from=mc-build /go/bin/mc /usr/local/bin/mc
COPY --from=mc-build /licenses /licenses
ENTRYPOINT ["mc"]

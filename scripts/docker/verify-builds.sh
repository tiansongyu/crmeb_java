#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MAVEN_IMAGE="${MAVEN_IMAGE:-maven:3.8.8-eclipse-temurin-8}"
TAG_SUFFIX="${TAG_SUFFIX:-local}"
MAVEN_CACHE_VOLUME="${MAVEN_CACHE_VOLUME:-crmeb-maven-cache}"

docker volume create "${MAVEN_CACHE_VOLUME}" >/dev/null

docker run --rm \
  -v "${MAVEN_CACHE_VOLUME}:/root/.m2" \
  -v "${ROOT_DIR}/crmeb:/workspace" \
  -w /workspace \
  "${MAVEN_IMAGE}" \
  mvn -pl crmeb-common,crmeb-service test

docker build \
  -f "${ROOT_DIR}/crmeb/Dockerfile" \
  --build-arg MODULE=crmeb-admin \
  --build-arg JAR_NAME=Crmeb-admin.jar \
  -t "crmeb-admin-verify:${TAG_SUFFIX}" \
  "${ROOT_DIR}/crmeb"

docker build \
  -f "${ROOT_DIR}/crmeb/Dockerfile" \
  --build-arg MODULE=crmeb-front \
  --build-arg JAR_NAME=Crmeb-front.jar \
  -t "crmeb-front-verify:${TAG_SUFFIX}" \
  "${ROOT_DIR}/crmeb"

docker build \
  -f "${ROOT_DIR}/admin/Dockerfile" \
  -t "crmeb-admin-web-verify:${TAG_SUFFIX}" \
  "${ROOT_DIR}/admin"

docker build \
  -f "${ROOT_DIR}/app/Dockerfile" \
  -t "crmeb-app-h5-verify:${TAG_SUFFIX}" \
  "${ROOT_DIR}/app"

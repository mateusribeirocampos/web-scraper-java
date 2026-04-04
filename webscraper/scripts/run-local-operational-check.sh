#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

APP_URL="${APP_URL:-http://localhost:8080}"
PROFILE_KEY="${PROFILE_KEY:-greenhouse_bitso}"
SMOKE_RUN="${SMOKE_RUN:-true}"
DAYS_BACK="${DAYS_BACK:-60}"
JOB_POSTINGS_CATEGORY="${JOB_POSTINGS_CATEGORY:-PRIVATE_SECTOR}"
JOB_POSTINGS_PROFILE="${JOB_POSTINGS_PROFILE:-JAVA_JUNIOR_BACKEND}"
JOB_POSTINGS_SENIORITY="${JOB_POSTINGS_SENIORITY:-}"
PUBLIC_CONTEST_STATUS="${PUBLIC_CONTEST_STATUS:-OPEN}"
PUBLIC_CONTEST_ORDER_BY="${PUBLIC_CONTEST_ORDER_BY:-registrationEndDate}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health}"
APP_LOG="${APP_LOG:-/tmp/webscraper-operational-check.log}"
KEEP_APP_RUNNING="${KEEP_APP_RUNNING:-false}"
WAIT_SECONDS="${WAIT_SECONDS:-60}"
app_host_port="${APP_URL#*://}"
app_host_port="${app_host_port%%/*}"
if [[ "${app_host_port}" == *:* ]]; then
  app_port="${app_host_port##*:}"
else
  app_port="8080"
fi

started_app=false
app_pid=""

cleanup() {
  if [[ "${started_app}" == "true" && "${KEEP_APP_RUNNING}" != "true" && -n "${app_pid}" ]]; then
    kill "${app_pid}" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

health_url="${APP_URL}${HEALTH_PATH}"
operational_check_url="${APP_URL}/api/v1/onboarding-profiles/${PROFILE_KEY}/operational-check?smokeRun=${SMOKE_RUN}&daysBack=${DAYS_BACK}"
read_model_label="job-postings"
read_model_url="${APP_URL}/api/v1/job-postings?category=${JOB_POSTINGS_CATEGORY}&daysBack=${DAYS_BACK}&profile=${JOB_POSTINGS_PROFILE}"
if [[ "${JOB_POSTINGS_CATEGORY}" == "PUBLIC_CONTEST" ]]; then
  read_model_label="public-contests"
  read_model_url="${APP_URL}/api/v1/public-contests?status=${PUBLIC_CONTEST_STATUS}&orderBy=${PUBLIC_CONTEST_ORDER_BY}"
elif [[ -n "${JOB_POSTINGS_SENIORITY}" ]]; then
  read_model_url="${read_model_url}&seniority=${JOB_POSTINGS_SENIORITY}"
fi

echo "[operational-check] profile=${PROFILE_KEY} smokeRun=${SMOKE_RUN} daysBack=${DAYS_BACK}"
if [[ "${JOB_POSTINGS_CATEGORY}" == "PUBLIC_CONTEST" ]]; then
  echo "[operational-check] user-query category=${JOB_POSTINGS_CATEGORY} status=${PUBLIC_CONTEST_STATUS} orderBy=${PUBLIC_CONTEST_ORDER_BY}"
else
  echo "[operational-check] user-query category=${JOB_POSTINGS_CATEGORY} profile=${JOB_POSTINGS_PROFILE} seniority=${JOB_POSTINGS_SENIORITY:-JUNIOR_AND_MID}"
fi

if ! curl -sS --max-time 2 "${health_url}" >/dev/null 2>&1; then
  if curl -sS --max-time 2 "${APP_URL}" >/dev/null 2>&1; then
    echo "[operational-check] application is already listening but health is not UP yet"
  else
  echo "[operational-check] application is down, starting spring-boot:run"
  (
    cd "${PROJECT_ROOT}"
    ./mvnw -q -DskipTests spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=${app_port}"
  ) >"${APP_LOG}" 2>&1 &
  app_pid=$!
  started_app=true
  fi
fi

for _ in $(seq 1 "${WAIT_SECONDS}"); do
  if curl -fsS "${health_url}" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! curl -fsS "${health_url}" >/dev/null 2>&1; then
  echo "[operational-check] application did not become healthy in ${WAIT_SECONDS}s" >&2
  if [[ -f "${APP_LOG}" ]]; then
    echo "[operational-check] last application log lines:" >&2
    tail -n 40 "${APP_LOG}" >&2 || true
  fi
  exit 1
fi

echo "[operational-check] health=UP"
echo "[operational-check] calling ${operational_check_url}"
curl -fsS -X POST "${operational_check_url}"
echo
echo "[operational-check] calling ${read_model_url}"
curl -fsS "${read_model_url}"
echo

if [[ "${started_app}" == "true" && "${KEEP_APP_RUNNING}" == "true" ]]; then
  echo "[operational-check] application left running with pid=${app_pid}"
fi

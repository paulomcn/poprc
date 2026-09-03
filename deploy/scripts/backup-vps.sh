#!/usr/bin/env bash
set -Eeuo pipefail

ENV_FILE="${POPRC_ENV_FILE:-/etc/poprc/poprc.env}"
if [[ ! -r "$ENV_FILE" ]]; then
    echo "Arquivo de ambiente nao encontrado ou sem leitura: $ENV_FILE" >&2
    exit 1
fi

set -a
# O arquivo e administrado pelo root e usa valores compativeis com shell.
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

DATABASE_URL="${DB_URL:-${SPRING_DATASOURCE_URL:-}}"
if [[ -n "$DATABASE_URL" ]]; then
    if [[ "$DATABASE_URL" != jdbc:postgresql://* ]]; then
        echo "DB_URL/SPRING_DATASOURCE_URL deve usar jdbc:postgresql://" >&2
        exit 1
    fi
    JDBC_ADDRESS="${DATABASE_URL#jdbc:postgresql://}"
    JDBC_AUTHORITY="${JDBC_ADDRESS%%/*}"
    JDBC_DATABASE="${JDBC_ADDRESS#*/}"
    JDBC_DATABASE="${JDBC_DATABASE%%\?*}"

    if [[ "$JDBC_AUTHORITY" =~ ^\[([^]]+)\](:([0-9]+))?$ ]]; then
        JDBC_HOST="${BASH_REMATCH[1]}"
        JDBC_PORT="${BASH_REMATCH[3]:-5432}"
    else
        JDBC_HOST="${JDBC_AUTHORITY%%:*}"
        if [[ "$JDBC_AUTHORITY" == *:* ]]; then
            JDBC_PORT="${JDBC_AUTHORITY##*:}"
        else
            JDBC_PORT="5432"
        fi
    fi

    DB_HOST="${DB_HOST:-$JDBC_HOST}"
    DB_PORT="${DB_PORT:-$JDBC_PORT}"
    DB_NAME="${DB_NAME:-$JDBC_DATABASE}"
fi

DB_USERNAME="${DB_USERNAME:-${SPRING_DATASOURCE_USERNAME:-}}"
DB_PASSWORD="${DB_PASSWORD:-${SPRING_DATASOURCE_PASSWORD:-}}"
APP_UPLOAD_DIR="${APP_UPLOAD_DIR:-/var/lib/poprc/uploads}"

: "${DB_HOST:?DB_HOST nao configurado}"
: "${DB_PORT:?DB_PORT nao configurado}"
: "${DB_NAME:?DB_NAME nao configurado}"
: "${DB_USERNAME:?DB_USERNAME nao configurado}"
: "${DB_PASSWORD:?DB_PASSWORD nao configurado}"
: "${APP_UPLOAD_DIR:?APP_UPLOAD_DIR nao configurado}"

BACKUP_DIR="${BACKUP_DIR:-/var/backups/poprc}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
install -d -m 0700 "$BACKUP_DIR"
BACKUP_DIR="$(cd "$BACKUP_DIR" && pwd -P)"
STAGE="$(mktemp -d "${BACKUP_DIR}/.stage-${TIMESTAMP}-XXXXXX")"
ARCHIVE_TMP="${BACKUP_DIR}/.poprc-${TIMESTAMP}.tar.gz.tmp"
ARCHIVE="${BACKUP_DIR}/poprc-${TIMESTAMP}.tar.gz"

cleanup() {
    rm -rf -- "$STAGE"
    rm -f -- "$ARCHIVE_TMP"
}
trap cleanup EXIT

PGPASSWORD="$DB_PASSWORD" pg_dump \
    --host "$DB_HOST" \
    --port "$DB_PORT" \
    --username "$DB_USERNAME" \
    --no-password \
    --format custom \
    --compress 9 \
    --no-owner \
    --no-privileges \
    --file "$STAGE/database.dump" \
    "$DB_NAME"

install -d -m 0700 "$STAGE/uploads"
if [[ -d "$APP_UPLOAD_DIR" ]]; then
    cp -a "$APP_UPLOAD_DIR"/. "$STAGE/uploads/"
fi

cat > "$STAGE/manifest.txt" <<EOF
created_at_utc=$TIMESTAMP
database=$DB_NAME
upload_directory=$APP_UPLOAD_DIR
git_revision=$(git -C /opt/poprc/current rev-parse HEAD 2>/dev/null || echo desconhecida)
EOF

(
    cd "$STAGE"
    find . -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > SHA256SUMS
    sha256sum --check SHA256SUMS
    tar -czf "$ARCHIVE_TMP" .
)

tar -tzf "$ARCHIVE_TMP" >/dev/null

mv "$ARCHIVE_TMP" "$ARCHIVE"
chmod 0600 "$ARCHIVE"
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'poprc-*.tar.gz' -mtime "+$RETENTION_DAYS" -delete

echo "$ARCHIVE"

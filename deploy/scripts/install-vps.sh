#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
    echo "Execute como root: sudo deploy/scripts/install-vps.sh dominio" >&2
    exit 1
fi

DOMAIN="${1:-${DOMAIN:-}}"
if [[ ! "$DOMAIN" =~ ^[A-Za-z0-9.-]+$ ]]; then
    echo "Informe um dominio ou IP valido como primeiro argumento." >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${PROJECT_ROOT:-$(cd "$SCRIPT_DIR/../.." && pwd)}"
ENV_FILE="/etc/poprc/poprc.env"

for command in java nginx curl pg_dump; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "Dependencia ausente: $command" >&2
        exit 1
    fi
done

JAR_PATH="${JAR_PATH:-}"
if [[ -z "$JAR_PATH" ]]; then
    JAR_PATH="$(find "$PROJECT_ROOT/target" -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)"
fi
if [[ -z "$JAR_PATH" || ! -f "$JAR_PATH" ]]; then
    echo "JAR nao encontrado. Execute ./mvnw --batch-mode -DskipTests package." >&2
    exit 1
fi
if [[ ! -f "$PROJECT_ROOT/frontend/dist/index.html" ]]; then
    echo "Frontend compilado nao encontrado. Execute npm ci e npm run build em frontend/." >&2
    exit 1
fi

if ! id -u poprc >/dev/null 2>&1; then
    useradd --system --home-dir /var/lib/poprc --create-home --shell /usr/sbin/nologin poprc
fi

install -d -o root -g poprc -m 0750 /etc/poprc
install -d -o poprc -g poprc -m 0750 /var/lib/poprc/uploads
install -d -o root -g root -m 0755 /opt/poprc/releases
install -d -o root -g root -m 0700 /var/backups/poprc

if [[ ! -f "$ENV_FILE" ]]; then
    install -o root -g poprc -m 0640 "$PROJECT_ROOT/deploy/env/poprc.env.example" "$ENV_FILE"
    echo "Arquivo criado em $ENV_FILE. Ajuste dominio, banco e senha e execute novamente." >&2
    exit 2
fi
if grep -q 'SUBSTITUA_' "$ENV_FILE"; then
    echo "$ENV_FILE ainda possui valores de exemplo." >&2
    exit 2
fi
if ! grep -Fxq "FRONTEND_URL=https://$DOMAIN" "$ENV_FILE"; then
    echo "Ajuste FRONTEND_URL=https://$DOMAIN em $ENV_FILE." >&2
    exit 2
fi
if ! grep -Fxq "APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://$DOMAIN" "$ENV_FILE"; then
    echo "Restrinja APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://$DOMAIN em $ENV_FILE." >&2
    exit 2
fi

REVISION="$(git -C "$PROJECT_ROOT" rev-parse --short=12 HEAD 2>/dev/null || date -u +%Y%m%d%H%M%S)"
RELEASE_ID="$(date -u +%Y%m%dT%H%M%SZ)-${REVISION}"
RELEASE_DIR="/opt/poprc/releases/$RELEASE_ID"
install -d -o root -g root -m 0755 "$RELEASE_DIR/backend" "$RELEASE_DIR/frontend"
install -o root -g root -m 0644 "$JAR_PATH" "$RELEASE_DIR/backend/poprc.jar"
cp -a "$PROJECT_ROOT/frontend/dist"/. "$RELEASE_DIR/frontend/"
find "$RELEASE_DIR/frontend" -type d -exec chmod 0755 {} +
find "$RELEASE_DIR/frontend" -type f -exec chmod 0644 {} +

PREVIOUS_RELEASE="$(readlink -f /opt/poprc/current 2>/dev/null || true)"

rollback_release() {
    trap - ERR
    if [[ -n "$PREVIOUS_RELEASE" && -d "$PREVIOUS_RELEASE" ]]; then
        echo "Restaurando release anterior: $PREVIOUS_RELEASE" >&2
        ln -sfn "$PREVIOUS_RELEASE" /opt/poprc/current
        systemctl restart poprc-backend.service || true
        systemctl reload nginx || true
    fi
}

on_error() {
    local exit_code=$?
    rollback_release
    exit "$exit_code"
}

trap on_error ERR
ln -sfn "$RELEASE_DIR" /opt/poprc/current

install -o root -g root -m 0644 "$PROJECT_ROOT/deploy/systemd/poprc-backend.service" /etc/systemd/system/poprc-backend.service
install -o root -g root -m 0644 "$PROJECT_ROOT/deploy/systemd/poprc-backup.service" /etc/systemd/system/poprc-backup.service
install -o root -g root -m 0644 "$PROJECT_ROOT/deploy/systemd/poprc-backup.timer" /etc/systemd/system/poprc-backup.timer
install -o root -g root -m 0750 "$PROJECT_ROOT/deploy/scripts/backup-vps.sh" /usr/local/sbin/poprc-backup

sed "s/__DOMAIN__/$DOMAIN/g" "$PROJECT_ROOT/deploy/nginx/poprc.conf.template" > /etc/nginx/sites-available/poprc
ln -sfn /etc/nginx/sites-available/poprc /etc/nginx/sites-enabled/poprc
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl daemon-reload
systemctl enable --now poprc-backup.timer
systemctl enable poprc-backend.service
systemctl restart poprc-backend.service
systemctl reload nginx

for attempt in {1..30}; do
    if curl --fail --silent http://127.0.0.1:8085/actuator/health >/dev/null; then
        trap - ERR
        echo "Release ativa: $RELEASE_DIR"
        echo "Aplicacao disponivel em: http://$DOMAIN"
        echo "Ative HTTPS com: certbot --nginx -d $DOMAIN --redirect"
        exit 0
    fi
    sleep 2
done

rollback_release
echo "O backend nao ficou saudavel. Consulte: journalctl -u poprc-backend -n 200 --no-pager" >&2
exit 1

#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

VITE=false
PREPARED_DOCS=false
RESET_DATA=false
FORCE=false
STOP=false

for arg in "$@"; do
    case "$arg" in
        --vite|-Vite) VITE=true ;;
        --prepared-documents|-PreparedDocuments) PREPARED_DOCS=true ;;
        --reset-data|-ResetData) RESET_DATA=true ;;
        --force|-Force|-f) FORCE=true ;;
        --stop|-Stop) STOP=true ;;
        *) echo "Argumento no reconocido: $arg" >&2; exit 1 ;;
    esac
done

if [ "$STOP" = true ]; then
    echo "Deteniendo el entorno UTAPED (conservando volúmenes y datos)..."
    docker compose -p utaped -f compose.yml stop
    echo "Contenedores detenidos correctamente."
    exit 0
fi

ENV_FILE="$ROOT_DIR/.env"
ENV_EXAMPLE="$ROOT_DIR/.env.example"

if [ ! -f "$ENV_FILE" ]; then
    if [ ! -f "$ENV_EXAMPLE" ]; then
        echo "No se encontró .env.example" >&2
        exit 1
    fi
    if command -v openssl >/dev/null 2>&1; then
        DB_SECRET="$(openssl rand -base64 32)"
        SEED_SECRET="$(openssl rand -base64 24)"
    else
        DB_SECRET="$(head -c 32 /dev/urandom | base64)"
        SEED_SECRET="$(head -c 24 /dev/urandom | base64)"
    fi
    sed -e "s|DB_PASSWORD=replace-with-a-random-local-password|DB_PASSWORD=$DB_SECRET|" \
        -e "s|UTAPED_SEED_PASSWORD=replace-with-a-random-account-seed-password|UTAPED_SEED_PASSWORD=$SEED_SECRET|" \
        "$ENV_EXAMPLE" > "$ENV_FILE"
    echo "Se ha generado un nuevo archivo .env con credenciales seguras únicas."
fi

if [ "$RESET_DATA" = true ]; then
    COMPOSE_CONFIG="$(docker compose -p utaped -f compose.yml config --format json 2>/dev/null || docker compose -p utaped -f compose.yml config 2>/dev/null || true)"
    if ! echo "$COMPOSE_CONFIG" | grep -qE '("SPRING_PROFILES_ACTIVE"|SPRING_PROFILES_ACTIVE):.*development-seed'; then
        echo "El reinicio de datos (-ResetData) solo está permitido en un entorno con SPRING_PROFILES_ACTIVE=development-seed en la configuración efectiva de Docker." >&2
        exit 1
    fi
    if echo "$COMPOSE_CONFIG" | grep -qE '("COOKIE_SECURE"|COOKIE_SECURE):[[:space:]]*("true"|true)'; then
        echo "El reinicio de datos (-ResetData) está bloqueado porque COOKIE_SECURE=true en la configuración efectiva de Docker (entorno productivo detectado)." >&2
        exit 1
    fi
    if [ "$FORCE" != true ]; then
        echo "ADVERTENCIA: -ResetData truncará todas las tablas operativas de la base de datos de desarrollo (conservando las 7 cuentas de desarrollo y sus credenciales)."
        echo "Esta acción es irreversible y eliminará todos los documentos, revisiones y datos existentes."
        read -rp "Escriba 'CONFIRMAR' para proceder con el reinicio de datos: " CONFIRMATION
        if [ "$CONFIRMATION" != "CONFIRMAR" ]; then
            echo "Operación de reinicio cancelada por el usuario."
            exit 0
        fi
    fi
    echo "Deteniendo backend y frontend antes de reiniciar datos..."
    docker compose -p utaped -f compose.yml stop backend frontend
    docker compose -p utaped -f compose.yml up -d db
    docker compose -p utaped -f compose.yml exec -T db psql -v ON_ERROR_STOP=1 -U utaped -d utaped << 'EOF'
BEGIN;
DO $$
DECLARE
    targets text;
BEGIN
    IF current_database() NOT IN ('utaped', 'utaped_dev') THEN
        RAISE EXCEPTION 'Reset requires utaped or utaped_dev database';
    END IF;
    IF EXISTS (SELECT 1 FROM app_user WHERE system_role='ADMIN' AND email NOT LIKE '%@utaped.example.invalid') THEN
        RAISE EXCEPTION 'Reinicio abortado: la base contiene cuentas administrativas no pertenecientes a las semillas de desarrollo.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM app_user WHERE email='admin@utaped.example.invalid') THEN
        RAISE EXCEPTION 'Reinicio abortado: no se encontró la cuenta administrativa semilla dev.admin; la base no parece ser un entorno de desarrollo.';
    END IF;
    SELECT string_agg(format('public.%I', tablename), ',') INTO targets
    FROM pg_tables
    WHERE schemaname = 'public' AND tablename NOT IN ('app_user', 'flyway_schema_history');
    IF targets IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || targets || ' CASCADE';
    END IF;
END;
$$;
DELETE FROM app_user WHERE id NOT IN (
    '6544faba-e4ec-3b8d-a800-38fad0a5647a',
    'e99ce527-0aa3-38be-b511-93e5bf7421bb',
    '08580022-d519-306e-89e3-26608b289c46',
    '1f3429a2-6531-3fe8-9bf1-2974f431646f',
    '42b355a8-42b2-3193-b2ba-2dcf1666ed5f',
    'e6c0b3cf-ab90-3d3f-a30f-4c24ef1db381',
    '286a182e-c0b8-32f0-b6c3-4dd8c37d5751'
);
COMMIT;
EOF
    echo "Datos reiniciados a valores semilla limpios (credenciales de desarrollo conservadas)."
fi

WEB_PORT="$(grep -E '^WEB_PORT=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 || echo '64430')"
BACKEND_PORT="$(grep -E '^BACKEND_PORT=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 || echo '18080')"
MAIL_UI_PORT="$(grep -E '^MAIL_UI_PORT=' "$ENV_FILE" 2>/dev/null | cut -d'=' -f2 || echo '18025')"
WEB_PORT="${WEB_PORT:-64430}"
BACKEND_PORT="${BACKEND_PORT:-18080}"
MAIL_UI_PORT="${MAIL_UI_PORT:-18025}"

export UTAPED_SEED_DOCUMENTS="$([ "$PREPARED_DOCS" = true ] && echo 'true' || echo 'false')"
export APP_PUBLIC_URL="$([ "$VITE" = true ] && echo 'http://127.0.0.1:5173' || echo "http://127.0.0.1:$WEB_PORT")"

if [ "$VITE" = true ]; then
    docker compose -p utaped -f compose.yml stop frontend 2>/dev/null || true
    docker compose -p utaped -f compose.yml up -d --build db mail backend
else
    docker compose -p utaped -f compose.yml up -d --build
fi

if [ "$RESET_DATA" = true ]; then
    docker compose -p utaped -f compose.yml restart backend
fi

echo "Esperando a que el backend esté disponible..."
READY=false
for i in $(seq 1 120); do
    if curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:$BACKEND_PORT/api/auth/csrf" 2>/dev/null | grep -q '200'; then
        if [ "$PREPARED_DOCS" = true ]; then
            COUNT="$(docker compose -p utaped -f compose.yml exec -T db psql -U utaped -d utaped -At -c "SELECT count(*) FROM audit_event WHERE action='DEVELOPMENT_DOCUMENTS_CREATED'" 2>/dev/null || echo '0')"
            if [ "$COUNT" = "1" ]; then
                READY=true
                break
            fi
        else
            READY=true
            break
        fi
    fi
    sleep 2
done

if [ "$READY" != true ]; then
    echo "El backend no estuvo disponible a tiempo. Revisa sus logs: docker compose -p utaped logs backend" >&2
    exit 1
fi

echo "=================================================="
echo " UTAPED está listo para su uso local"
echo "=================================================="
if [ "$VITE" = true ]; then
    echo "Backend listo (puerto $BACKEND_PORT)."
    echo "Para el frontend interactivo: cd frontend && npm run dev"
    echo "URL de desarrollo Vite: http://127.0.0.1:5173"
else
    echo "Aplicación web: http://127.0.0.1:$WEB_PORT"
fi
echo "Bandeja de correo local (Mailpit): http://127.0.0.1:$MAIL_UI_PORT"
echo "Usuarios de desarrollo:"
echo "  dev.admin (Administrador), dev.principal (Elaborador),"
echo "  dev.coordinator (Coordinador), dev.reviewer (Revisor),"
echo "  dev.approver (Aprobador), dev.member (Integrante), dev.outsider"
echo "Contraseña de desarrollo: consultar UTAPED_SEED_PASSWORD en el archivo local .env"
echo "Para detener el entorno conservando datos: ./scripts/start-development.sh --stop"
echo "=================================================="


"""Exercise production HTTPS against an already verified isolated recovery, without sending mail."""
import argparse
import hashlib
import http.cookiejar
import json
import os
import re
from pathlib import Path
import ssl
import subprocess
import sys
import tempfile
import time
import urllib.request
import urllib.parse
import uuid
from concurrent.futures import ThreadPoolExecutor
from operations import ROOT, one, environment, sql, run, containers


def test_transport(project, port):
    if not re.fullmatch(r"utaped-restore-[a-z0-9][a-z0-9-]{0,40}", project):
        raise RuntimeError("La prueba solo admite un proyecto de recuperación aislado.")
    db, backend, frontend = one(project, "db"), one(project, "backend"), one(project, "frontend")
    if sql(db, "SELECT current_database()") != "utaped_restore":
        raise RuntimeError("La prueba requiere utaped_restore.")
    volume = next(mount["Name"] for mount in backend["Mounts"] if mount["Destination"] == "/app/runtime-data")
    fixture = {}
    for line in (ROOT / ".env.sample").read_text(encoding="utf-8-sig").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            name, value = line.split("=", 1);fixture[name.strip()] = value.strip().strip('"').strip("'")
    name = "utaped-ops-tls-" + uuid.uuid4().hex[:10]
    running = [item["Id"] for item in (frontend, backend) if item["State"]["Running"]]
    target = (ROOT / "backend/target").resolve()
    with tempfile.TemporaryDirectory(prefix="utaped-transport-", dir=target) as temporary:
        directory = Path(temporary).resolve()
        if not directory.is_relative_to(target):
            raise RuntimeError("Directorio temporal fuera del espacio permitido.")
        certificate, key = directory / "certificate.pem", directory / "private.key"
        config = directory / "openssl.cnf";config.write_text("[req]\ndistinguished_name=dn\n[dn]\n")
        run(["openssl", "req", "-config", str(config), "-x509", "-newkey", "rsa:2048", "-nodes", "-keyout", str(key), "-out", str(certificate), "-days", "10", "-subj", "/CN=localhost", "-addext", "subjectAltName=DNS:localhost,IP:127.0.0.1"])
        variables = {"DB_PASSWORD": environment(db)["POSTGRES_PASSWORD"], "APP_PUBLIC_URL": "https://localhost:" + str(port),
                     "WEB_PORT": str(port), "WEB_BIND_ADDRESS": "127.0.0.1", "TLS_CERTIFICATE_FILE": certificate.as_posix(),
                     "TLS_PRIVATE_KEY_FILE": key.as_posix(), "SMTP_HOST": "mail", "SMTP_PORT": "1025", "SMTP_AUTH": "false", "SMTP_STARTTLS": "false", "SMTP_SSL": "false", "MAIL_FROM": "test@example.invalid", "AI_WRITING_ENABLED": "false"}
        env_file = directory / "private.env";env_file.write_text("\n".join(k + "=" + v for k, v in variables.items()) + "\n")
        override_file = directory / "override.yaml"
        # Image IDs must never be reused as build tags; use the exact backed-up images without rebuilding.
        override_file.write_text("services:\n  backend:\n    build: !reset null\n    image: " + json.dumps(backend["Image"]) + "\n    pull_policy: never\n    environment:\n      DB_URL: jdbc:postgresql://db:5432/utaped_restore\n      DEADLINE_NOTICES_ENABLED: 'false'\n  frontend:\n    build: !reset null\n    image: " + json.dumps(frontend["Image"]) + "\n    pull_policy: never\nnetworks:\n  default:\n    external: true\n    name: " + json.dumps(project + "_default") + "\nvolumes:\n  document-data:\n    external: true\n    name: " + json.dumps(volume) + "\n")
        validation_values = dict(variables, APP_PUBLIC_URL="https://localhost:64430", WEB_PORT="64430", SMTP_HOST="smtp.fixture.test", SMTP_PORT="587", SMTP_AUTH="true", SMTP_STARTTLS="true", SMTP_USERNAME="test-only-account", SMTP_PASSWORD="test-only-smtp-password", MAIL_FROM="test@fixture.test")
        validation_file = directory / "validation.env";validation_file.write_text("\n".join(k + "=" + v for k, v in validation_values.items()) + "\n")
        validation = json.loads(run([sys.executable, "scripts/production-preflight.py", "--env-file", str(validation_file)]))
        if not validation["configurationValid"]:raise RuntimeError("La configuración de prueba no pasó el preflight.")
        command = ["docker", "compose", "-p", name, "--env-file", str(env_file), "-f", "compose.yml", "-f", "compose.production.yml", "-f", str(override_file), "--profile", "web"]
        try:
            print("Deteniendo únicamente el frontend/backend del destino recuperado…", flush=True)
            if running:run(["docker", "stop", "--time", "60", *running])
            print("Iniciando la configuración HTTPS aislada…", flush=True)
            startup = subprocess.run([*command, "up", "-d", "--no-deps", "backend", "frontend"], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            if startup.returncode:
                terms = [term for term in ("invalid reference", "build", "pull", "port", "network", "volume", "certificate", "mount", "profile", "image", "service", "undefined") if term in startup.stderr.decode(errors="replace").lower()]
                print(json.dumps({"startupFailureCategories": terms}), flush=True)
                diagnostic = startup.stderr.decode(errors="replace").splitlines()[-1]
                for source in (variables, fixture, os.environ):
                    for identifier, value in source.items():
                        if value and any(marker in identifier.upper() for marker in ("PASSWORD", "KEY", "TOKEN", "SECRET")):
                            diagnostic = diagnostic.replace(value, "[private]")
                print(json.dumps({"sanitizedStartupDiagnostic": diagnostic}), flush=True)
                raise RuntimeError("Falló el arranque HTTPS aislado; no se muestra la salida privada.")
            print("Verificando puertos y transporte…", flush=True)
            merged = json.loads(run([*command, "config", "--format", "json"]))
            for service in ("db", "backend", "mail"):
                if merged["services"].get(service, {}).get("ports"):raise RuntimeError("Un servicio interno tiene puertos publicados.")
            cookies = http.cookiejar.CookieJar()
            opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies), urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=str(certificate))))
            base = "https://localhost:" + str(port)
            for attempt in range(90):
                try:
                    with opener.open(base + "/healthz", timeout=3) as response:
                        if json.loads(response.read())["status"] == "UP":break
                except Exception:
                    if attempt == 89:raise RuntimeError("HTTPS/backend no quedó disponible.")
                    time.sleep(1)
            with opener.open(base + "/api/auth/csrf") as response:
                csrf = json.loads(response.read())
                if "max-age=" not in response.headers.get("Strict-Transport-Security", "") or not response.headers.get("Content-Security-Policy"):
                    raise RuntimeError("Faltan las cabeceras HTTPS esperadas.")
            if not any(cookie.name == "SESSION" and cookie.secure for cookie in cookies):raise RuntimeError("La sesión no recibió una cookie Secure.")
            with opener.open(base + "/api/auth/captcha") as response:
                captcha = str(uuid.UUID(response.headers["X-Captcha-Id"]));response.read()
            digest = hashlib.sha256((captcha + ":ABC234").encode()).hexdigest()
            sql(db, "UPDATE login_captcha SET answer_hash='" + digest + "' WHERE id='" + captcha + "' AND current_database()='utaped_restore'")
            sql(db, "DELETE FROM auth_throttle WHERE current_database()='utaped_restore' AND key_hash IN (SELECT encode(sha256(convert_to('login:'||email,'UTF8')),'hex') FROM app_user WHERE username='dev.principal')")
            body = urllib.parse.urlencode({"username": "dev.principal", "password": fixture["UTAPED_SEED_PASSWORD"], "captcha": "ABC234"}).encode()
            request = urllib.request.Request(base + "/api/auth/login", data=body, headers={csrf["headerName"]: csrf["token"], "Content-Type": "application/x-www-form-urlencoded"})
            with opener.open(request) as response:
                if response.status != 204:raise RuntimeError("El acceso HTTPS de prueba falló.")
            with opener.open(base + "/api/auth/me") as response:
                if json.loads(response.read())["username"] != "dev.principal":raise RuntimeError("Identidad HTTPS inesperada.")
            query = """SELECT json_build_object('id',r.id,'sha256',f.sha256) FROM document_review r JOIN work_plan w ON w.id=r.work_plan_id
                JOIN stored_file f ON f.id=r.current_file_id JOIN organizational_scope s ON s.id=w.scope_id JOIN app_user u ON u.id=s.principal_user_id
                WHERE r.status='APPROVED' AND u.username='dev.principal' ORDER BY r.created_at DESC LIMIT 1"""
            signed = json.loads(sql(db, query))
            with opener.open(base + "/api/reviews/" + signed["id"] + "/content") as response:
                if hashlib.sha256(response.read()).hexdigest() != signed["sha256"]:raise RuntimeError("El PDF recuperado no conserva sus bytes.")
            def check_health(_):
                start = time.monotonic()
                with urllib.request.urlopen(base + "/healthz", context=ssl.create_default_context(cafile=str(certificate)), timeout=10) as response:
                    if response.status != 200 or json.loads(response.read())["status"] != "UP":raise RuntimeError("Falló una lectura concurrente del estado.")
                return time.monotonic() - start
            with ThreadPoolExecutor(max_workers=8) as pool:
                times = sorted(pool.map(check_health, range(40)))
            print(json.dumps({"httpsVerified": True, "trustedTestCertificate": True, "secureSessionCookie": True, "loginAndIdentity": True, "restoredSignedPdfHash": True, "privateServicesUnpublished": True, "mailSent": False, "concurrentHealthChecks": 40, "workers": 8, "p95Seconds": round(times[37], 3)}))
        finally:
            if containers(name):run([*command, "down"])
            if running:run(["docker", "start", *reversed(running)])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--restored-project", required=True)
    parser.add_argument("--port", type=int, default=64433)
    args = parser.parse_args()
    try:
        if not 1024 <= args.port <= 65535:raise RuntimeError("Puerto de prueba inválido.")
        test_transport(args.restored_project, args.port)
    except Exception as error:
        parser.exit(1, "La prueba aislada no se completó: " + (str(error) if isinstance(error, RuntimeError) else type(error).__name__) + "\n")

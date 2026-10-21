"""Read-only validation of the production Compose configuration, HTTPS identity and private credentials."""
import argparse
import ipaddress
import json
from pathlib import Path
from urllib.parse import urlparse
from operations import ROOT, run


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def preflight(env_file, allow_bootstrap=False):
    config = json.loads(run(["docker", "compose", "--env-file", str(env_file), "-f", "compose.yml", "-f", "compose.production.yml", "--profile", "web", "config", "--format", "json"]))
    services = config["services"]
    variables = services["backend"]["environment"]
    url = urlparse(variables["APP_PUBLIC_URL"])
    ports = services["frontend"].get("ports", [])
    require(url.scheme == "https" and url.hostname and url.port == 64430 and not url.username and not url.query and not url.fragment and url.path in ("", "/"), "APP_PUBLIC_URL debe ser una URL HTTPS del servidor en el puerto 64430.")
    require("example.invalid" not in url.hostname, "Configure el nombre real del servidor HTTPS.")
    require(len(ports) == 1 and str(ports[0]["published"]) == "64430" and ports[0]["target"] == 443, "Producción debe publicar únicamente HTTPS en 64430.")
    for service in ("backend", "db", "mail"):
        require(not services.get(service, {}).get("ports"), "No publique backend, DB ni Mailpit en producción.")
    require(str(variables["COOKIE_SECURE"]).lower() == "true" and str(variables["SIGNING_ALLOW_HTTP"]).lower() == "false", "Cookies/firma deben requerir HTTPS.")
    if variables.get("BOOTSTRAP_ADMIN_PASSWORD"):
        require(allow_bootstrap and len(variables["BOOTSTRAP_ADMIN_PASSWORD"]) >= 12 and "@" in variables.get("BOOTSTRAP_ADMIN_EMAIL", ""), "El primer arranque requiere --allow-bootstrap y credenciales temporales válidas; retírelas después del alta inicial.")
    require(not variables.get("SPRING_PROFILES_ACTIVE") and not variables.get("UTAPED_SEED_PASSWORD"), "Producción no debe habilitar las semillas de desarrollo.")
    password = variables.get("DB_PASSWORD", "")
    require(len(password) >= 20 and "replace" not in password.lower(), "Configure una contraseña privada de DB de al menos 20 caracteres.")
    require(variables["SMTP_HOST"] not in ("mail", "localhost") and "example.invalid" not in variables["SMTP_HOST"], "Configure el SMTP institucional.")
    require("example.invalid" not in variables["MAIL_FROM"] and "@" in variables["MAIL_FROM"], "Configure el remitente autorizado.")
    tls = str(variables.get("SMTP_STARTTLS", "false")).lower() == "true"
    ssl = str(variables.get("SMTP_SSL", "false")).lower() == "true"
    require(tls != ssl, "Elija STARTTLS o SSL implícito para SMTP.")
    if str(variables.get("SMTP_AUTH", "false")).lower() == "true":
        require(bool(variables.get("SMTP_USERNAME")) and bool(variables.get("SMTP_PASSWORD")), "SMTP_AUTH requiere usuario y clave privada.")
    if str(variables.get("AI_WRITING_ENABLED", "false")).lower() == "true":
        provider = variables.get("AI_WRITING_PROVIDER")
        require(provider in ("GROQ", "OPENAI"), "Proveedor de redacción inválido.")
        require(bool(variables.get("AI_WRITING_MODEL")) and bool(variables.get("OPENAI_API_KEY" if provider == "OPENAI" else "GROQ_API_KEY")), "Redacción habilitada requiere modelo y clave del proveedor elegido.")
    mounts = {volume["target"]: volume for volume in services["frontend"]["volumes"]}
    certificate = Path(mounts["/etc/nginx/tls/fullchain.pem"]["source"])
    private_key = Path(mounts["/etc/nginx/tls/private.key"]["source"])
    for target in ("/etc/nginx/tls/fullchain.pem", "/etc/nginx/tls/private.key"):
        require(mounts[target].get("read_only") is True and Path(mounts[target]["source"]).is_file(), "Certificado y clave TLS deben ser archivos existentes montados en solo lectura.")
    public_cert = run(["openssl", "x509", "-in", str(certificate), "-pubkey", "-noout"])
    public_key = run(["openssl", "pkey", "-in", str(private_key), "-pubout"], data=b"")
    require(public_cert.strip() == public_key.strip(), "El certificado HTTPS y la clave privada no corresponden.")
    run(["openssl", "x509", "-in", str(certificate), "-checkend", "604800", "-noout"])
    try:
        ipaddress.ip_address(url.hostname)
        flag = "-checkip"
    except ValueError:
        flag = "-checkhost"
    identity = run(["openssl", "x509", "-in", str(certificate), flag, url.hostname, "-noout"])
    require(b"does NOT match" not in identity, "El nombre del servidor no coincide con el certificado HTTPS.")
    return {"configurationValid": True, "publicUrl": variables["APP_PUBLIC_URL"], "onlyPublishedPort": 64430,
            "smtpTransport": "STARTTLS" if tls else "SSL", "certificateKeyMatches": True,
            "scope": "Configuración y certificado local; no acredita DNS, firewall, confianza CA, entrega SMTP ni restauración."}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, default=ROOT / ".env.production")
    parser.add_argument("--allow-bootstrap", action="store_true", help="Solo durante el alta controlada de la primera cuenta administrativa.")
    args = parser.parse_args()
    try:
        print(json.dumps(preflight(args.env_file.resolve(), args.allow_bootstrap)))
    except (RuntimeError, KeyError, ValueError, OSError) as error:
        parser.exit(1, "No pasó la validación: " + (str(error) if isinstance(error, RuntimeError) else type(error).__name__) + "\n")

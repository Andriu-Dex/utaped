"""Paired PostgreSQL/private-volume backups and restoration into a fresh isolated project."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import secrets
import socket
import subprocess
import sys
import tarfile
import time
import uuid
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent.parent
METADATA_SQL = """SELECT json_build_object(
  'migration',(SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1),
  'users',(SELECT count(*) FROM app_user), 'documents',(SELECT count(*) FROM work_plan),
  'reviews',(SELECT count(*) FROM document_review), 'auditEvents',(SELECT count(*) FROM audit_event),
  'files',COALESCE((SELECT json_agg(json_build_object('id',id,'key',storage_key,'sha256',sha256,'bytes',size_bytes) ORDER BY id) FROM stored_file),'[]'::json)
)"""


def run(arguments, *, data=None, env=None):
    result = subprocess.run(arguments, input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=env, cwd=ROOT)
    if result.returncode:
        message = result.stderr.decode(errors="replace").lower()
        if "port is already allocated" in message or "address already in use" in message:
            raise RuntimeError("El puerto de recuperación ya está ocupado. Use --web-port con un puerto libre.")
        if "no such image" in message:
            raise RuntimeError("Una imagen de aplicación no está disponible. Verifique el archivo de imágenes del respaldo.")
        raise RuntimeError("La operación externa falló; no se imprime su salida privada. Revise el servicio local antes de reintentar.")
    return result.stdout


def inspect(container):
    return json.loads(run(["docker", "inspect", container]))[0]


def containers(project, service=None):
    command = ["docker", "ps", "-aq", "--filter", "label=com.docker.compose.project=" + project]
    if service:
        command += ["--filter", "label=com.docker.compose.service=" + service]
    return run(command).decode().split()


def one(project, service):
    items = containers(project, service)
    if len(items) != 1:
        raise RuntimeError("Se requiere exactamente un contenedor por servicio; no se admite un respaldo parcial de réplicas.")
    return inspect(items[0])


def environment(container):
    return dict(value.split("=", 1) for value in container["Config"]["Env"] if "=" in value)


def sql(container, statement):
    variables = environment(container)
    return run(["docker", "exec", "-i", container["Id"], "psql", "-X", "-v", "ON_ERROR_STOP=1", "-U",
                variables["POSTGRES_USER"], "-d", variables["POSTGRES_DB"], "-t", "-A", "-c", statement]).decode().strip()


def checksum(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def storage_prefix(value):
    path = PurePosixPath(value)
    if path.is_absolute() or ".." in path.parts or not re.fullmatch(r"[A-Za-z0-9_./-]+", value):
        raise RuntimeError("Ruta de almacenamiento no compatible con la herramienta de respaldo.")
    return str(path)


def validate_metadata(metadata):
    for item in metadata["files"]:
        uuid.UUID(item["id"])
        uuid.UUID(item["key"])
        if not re.fullmatch(r"[0-9a-f]{64}", item["sha256"]) or item["bytes"] < 1:
            raise RuntimeError("Metadata de archivo inválida.")


def verify_backup(directory):
    manifest = json.loads((directory / "manifest.json").read_text(encoding="utf-8"))
    if manifest["schemaVersion"] != 1:
        raise RuntimeError("Versión de respaldo no compatible.")
    prefix = storage_prefix(manifest["storagePrefix"])
    validate_metadata(manifest["metadata"])
    for name in ("database.dump", "documents.tar.gz", *(["application-images.tar"] if "application-images.tar" in manifest["checksums"] else [])):
        if checksum(directory / name) != manifest["checksums"][name]:
            raise RuntimeError("El respaldo no supera la verificación de integridad.")
    with tarfile.open(directory / "documents.tar.gz", "r:gz") as archive:
        entries = {}
        for member in archive:
            path = PurePosixPath(member.name)
            if path.is_absolute() or ".." in path.parts or not (member.isdir() or member.isfile()):
                raise RuntimeError("Archivo de volumen inseguro o no compatible.")
            if member.isfile():
                if str(path) in entries:
                    raise RuntimeError("El archivo de volumen contiene entradas duplicadas.")
                entries[str(path)] = member
        for item in manifest["metadata"]["files"]:
            member = entries.get(prefix + "/" + item["key"])
            if not member or member.size != item["bytes"]:
                raise RuntimeError("La DB y los archivos no corresponden al mismo conjunto.")
            with archive.extractfile(member) as stream:
                if hashlib.file_digest(stream, "sha256").hexdigest() != item["sha256"]:
                    raise RuntimeError("El contenido documental no coincide con la DB.")
    return manifest


def backup(args):
    backend, db = one(args.project, "backend"), one(args.project, "db")
    frontend = one(args.project, "frontend")
    variables = environment(backend)
    mounts = [mount for mount in backend["Mounts"] if mount["Destination"] == "/app/runtime-data" and mount["Type"] == "volume"]
    if len(mounts) != 1 or not variables.get("DOCUMENT_STORAGE_ROOT", "").startswith("/app/runtime-data/"):
        raise RuntimeError("Se necesita el volumen privado estándar de UTAPED.")
    prefix = storage_prefix(variables["DOCUMENT_STORAGE_ROOT"][len("/app/runtime-data/"):])
    if not 180000 <= int(sql(db, "SHOW server_version_num")) < 190000:
        raise RuntimeError("Este procedimiento requiere PostgreSQL 18.")
    directory = args.destination.resolve()
    if "," in str(directory) or directory.exists():
        raise RuntimeError("Use un directorio nuevo para cada respaldo, sin comas en su ruta.")
    directory.mkdir(parents=True, mode=0o700)
    running = [item["Id"] for item in (frontend, backend) if item["State"]["Running"]]
    temporary = "/tmp/utaped-backup-" + str(uuid.uuid4()) + ".dump"
    try:
        print("Deteniendo los servicios de escritura para respaldar DB y documentos juntos…", flush=True)
        if running:
            run(["docker", "stop", "--time", "60", *running])
        metadata = json.loads(sql(db, METADATA_SQL))
        validate_metadata(metadata)
        db_env = environment(db)
        run(["docker", "exec", db["Id"], "pg_dump", "-U", db_env["POSTGRES_USER"], "-d", db_env["POSTGRES_DB"], "-Fc", "-f", temporary])
        run(["docker", "cp", db["Id"] + ":" + temporary, str(directory / "database.dump")])
        run(["docker", "run", "--rm", "--mount", "type=volume,source=" + mounts[0]["Name"] + ",target=/source,readonly",
             "--mount", "type=bind,source=" + str(directory) + ",target=/backup", "postgres:18", "tar", "-czf", "/backup/documents.tar.gz", "-C", "/source", "."])
        run(["docker", "image", "save", "-o", str(directory / "application-images.tar"), backend["Image"], frontend["Image"]])
        manifest = {"schemaVersion": 1, "createdAt": datetime.now(timezone.utc).isoformat(), "sourceProject": args.project,
                    "storagePrefix": prefix, "backendImage": backend["Image"], "frontendImage": frontend["Image"],
                    "metadata": metadata, "checksums": {name: checksum(directory / name) for name in ("database.dump", "documents.tar.gz", "application-images.tar")}}
        (directory / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        verify_backup(directory)
        print(json.dumps({"backup": str(directory), "filesVerified": len(metadata["files"]), "documents": metadata["documents"], "migration": metadata["migration"]}))
    except Exception:
        # A partial backup remains available for diagnosis but is never accepted as a complete backup.
        (directory / "manifest.json").unlink(missing_ok=True)
        raise
    finally:
        try:
            run(["docker", "exec", db["Id"], "rm", "-f", temporary])
        finally:
            if running:
                run(["docker", "start", *reversed(running)])


def restore(args):
    if not re.fullmatch(r"utaped-restore-[a-z0-9][a-z0-9-]{0,40}", args.project):
        raise RuntimeError("El destino debe llamarse utaped-restore-*; no se restaura sobre el proyecto principal.")
    if containers(args.project) or run(["docker", "volume", "ls", "-q", "--filter", "label=com.docker.compose.project=" + args.project]).strip():
        raise RuntimeError("El proyecto de destino ya existe. Elija uno nuevo; no se borrará ni sobrescribirá.")
    if args.start_application:
        try:
            with socket.socket() as probe:
                probe.bind(("127.0.0.1", args.web_port))
        except OSError:
            raise RuntimeError("El puerto de recuperación ya está ocupado. Use --web-port con un puerto libre.")
    directory = args.backup.resolve()
    manifest = verify_backup(directory)
    if "application-images.tar" in manifest["checksums"]:
        run(["docker", "image", "load", "-i", str(directory / "application-images.tar")])
    for image in (manifest["backendImage"], manifest["frontendImage"]):
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", image):
            raise RuntimeError("Imagen del respaldo inválida.")
        run(["docker", "image", "inspect", image])
    env = dict(os.environ, RESTORE_DB_PASSWORD=secrets.token_urlsafe(32), RESTORE_BACKEND_IMAGE=manifest["backendImage"],
               RESTORE_FRONTEND_IMAGE=manifest["frontendImage"], RESTORE_STORAGE_PREFIX=manifest["storagePrefix"], RESTORE_WEB_PORT=str(args.web_port))
    command = ["docker", "compose", "-p", args.project, "-f", str(ROOT / "compose.restore.yml")]
    print("Creando un destino de recuperación independiente…", flush=True)
    run([*command, "up", "-d", "db"], env=env)
    db = one(args.project, "db")
    deadline = time.monotonic() + 90
    while inspect(db["Id"])["State"].get("Health", {}).get("Status") != "healthy":
        if time.monotonic() > deadline:
            raise RuntimeError("El PostgreSQL de recuperación no quedó disponible.")
        time.sleep(1)
    if sql(db, "SELECT current_database()") != "utaped_restore":
        raise RuntimeError("El destino de recuperación no es la DB aislada esperada.")
    temporary = "/tmp/utaped-restore-" + str(uuid.uuid4()) + ".dump"
    try:
        run(["docker", "cp", str(directory / "database.dump"), db["Id"] + ":" + temporary])
        run(["docker", "exec", db["Id"], "pg_restore", "--exit-on-error", "--single-transaction", "--no-owner", "--no-acl", "-U", "utaped", "-d", "utaped_restore", temporary])
    finally:
        run(["docker", "exec", db["Id"], "rm", "-f", temporary])
    restored = json.loads(sql(db, METADATA_SQL))
    if restored != manifest["metadata"]:
        raise RuntimeError("La metadata restaurada no coincide con el respaldo.")
    # Create the backend without starting writers, then inspect its project-owned volume.
    run([*command, "--profile", "web", "create", "backend"], env=env)
    backend = one(args.project, "backend")
    volume = next(mount["Name"] for mount in backend["Mounts"] if mount["Destination"] == "/app/runtime-data")
    run(["docker", "run", "--rm", "--mount", "type=volume,source=" + volume + ",target=/restore",
         "--mount", "type=bind,source=" + str(directory) + ",target=/backup,readonly", "postgres:18", "tar", "-xzf", "/backup/documents.tar.gz", "-C", "/restore"])
    lines = "".join(item["sha256"] + "  ./" + manifest["storagePrefix"] + "/" + item["key"] + "\n" for item in restored["files"])
    if lines:
        run(["docker", "run", "--rm", "-i", "--mount", "type=volume,source=" + volume + ",target=/source,readonly", "postgres:18", "sh", "-c", "cd /source && sha256sum -c -"], data=lines.encode())
    if args.start_application:
        run([*command, "--profile", "web", "up", "-d"], env=env)
    result = {"project": args.project, "filesVerified": len(restored["files"]), "documents": restored["documents"], "migration": restored["migration"],
              "applicationStarted": args.start_application, "webUrl": "http://127.0.0.1:" + str(args.web_port) if args.start_application else None}
    (directory / (args.project + "-verification.json")).write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="action", required=True)
    backup_parser = commands.add_parser("backup")
    backup_parser.add_argument("--project", required=True)
    backup_parser.add_argument("--destination", required=True, type=Path)
    restore_parser = commands.add_parser("restore")
    restore_parser.add_argument("--project", required=True)
    restore_parser.add_argument("--backup", required=True, type=Path)
    restore_parser.add_argument("--start-application", action="store_true")
    restore_parser.add_argument("--web-port", type=int, default=64432)
    verify_parser = commands.add_parser("verify")
    verify_parser.add_argument("--backup", required=True, type=Path)
    args = parser.parse_args()
    if args.action == "backup":
        if not re.fullmatch(r"[a-z0-9][a-z0-9_-]{0,60}", args.project):
            raise RuntimeError("Nombre de proyecto inválido.")
        backup(args)
    elif args.action == "restore":
        if not 1024 <= args.web_port <= 65535:
            raise RuntimeError("Puerto de recuperación inválido.")
        restore(args)
    else:
        manifest = verify_backup(args.backup.resolve())
        print(json.dumps({"verified": True, "filesVerified": len(manifest["metadata"]["files"])}))


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, ValueError, KeyError, tarfile.TarError) as error:
        # Configuration and provider/container bodies are never printed.
        print("No se completó la operación: " + (str(error) if isinstance(error, RuntimeError) else type(error).__name__), file=sys.stderr)
        sys.exit(1)

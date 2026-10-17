"""Reset development examples from official reference seeds, retaining the seven account credentials."""
import argparse
import hashlib
import json
from pathlib import Path
import uuid
from operations import ROOT, METADATA_SQL, environment, one, sql, run, verify_backup

PROJECT = "utaped-sample"
ROLES = ("admin", "principal", "coordinator", "reviewer", "approver", "member", "outsider")


def seed_id(key):
    return str(uuid.UUID(bytes=hashlib.md5(("utaped-development:" + key).encode()).digest(), version=3))


def clean(backup_directory, apply=False):
    db, backend, frontend = one(PROJECT, "db"), one(PROJECT, "backend"), one(PROJECT, "frontend")
    if environment(db).get("POSTGRES_DB") != "utaped_dev" or sql(db, "SELECT current_database()") != "utaped_dev":
        raise RuntimeError("Cleanup requires the manual development database utaped_dev.")
    if environment(backend).get("SPRING_PROFILES_ACTIVE") != "development-seed":
        raise RuntimeError("Cleanup requires the development-seed profile.")
    # Validate the curated reference before changing any records.
    reference = json.loads((ROOT / "backend/src/main/resources/development/institutional-reference.json").read_text(encoding="utf-8"))
    if reference["periodName"] != "Julio–Diciembre 2026" or not reference["groups"] or not reference["careers"]:
        raise RuntimeError("Expected institutional reference is missing.")
    ids = ",".join("'" + seed_id(role) + "'" for role in ROLES)
    before = json.loads(sql(db, METADATA_SQL))
    if sql(db, "SELECT count(*) FROM app_user WHERE id IN (" + ids + ")") != "7":
        raise RuntimeError("All seven development accounts must already exist; no credentials will be regenerated.")
    result = {"project": PROJECT, "usersBefore": before["users"], "documentsBefore": before["documents"],
              "accountsRetained": 7, "apply": apply}
    if not apply:
        print(json.dumps(result))
        return
    manifest = verify_backup(backup_directory.resolve())
    if manifest["sourceProject"] != PROJECT:
        raise RuntimeError("Backup is not from the manual development project.")
    running = [item["Id"] for item in (frontend, backend) if item["State"]["Running"]]
    if running:
        run(["docker", "stop", *running])
    try:
        if json.loads(sql(db, METADATA_SQL)) != manifest["metadata"]:
            raise RuntimeError("Data changed after backup; create another backup before cleaning.")
        credentials = sql(db, "SELECT json_agg(json_build_array(id,password_hash) ORDER BY id) FROM app_user WHERE id IN (" + ids + ")")
        # Only this explicitly checked development DB is rebuilt. App users and Flyway survive TRUNCATE.
        sql(db, """BEGIN;
DO $$
DECLARE targets text;
BEGIN
    IF current_database() <> 'utaped_dev' THEN RAISE EXCEPTION 'Wrong cleanup database'; END IF;
    SELECT string_agg(format('public.%I',tablename),',') INTO targets FROM pg_tables
      WHERE schemaname='public' AND tablename NOT IN ('app_user','flyway_schema_history');
    EXECUTE 'TRUNCATE TABLE ' || targets || ' CASCADE';
END;
$$;
DELETE FROM app_user WHERE id NOT IN (""" + ids + "); COMMIT;")
        if sql(db, "SELECT json_agg(json_build_array(id,password_hash) ORDER BY id) FROM app_user WHERE id IN (" + ids + ")") != credentials:
            raise RuntimeError("Development account credentials changed unexpectedly.")
        result["credentialsPreserved"] = True
        result["documentsAfterCleanup"] = json.loads(sql(db, METADATA_SQL))["documents"]
        print(json.dumps(result))
    finally:
        if running:
            run(["docker", "start", *reversed(running)])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backup", type=Path, required=True)
    parser.add_argument("--apply", action="store_true", help="Remove example records after verifying the paired backup; otherwise preview only")
    args = parser.parse_args()
    clean(args.backup, args.apply)

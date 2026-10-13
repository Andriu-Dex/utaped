import hashlib
import io
import json
from pathlib import Path
import sys
import tarfile
import tempfile
import unittest
from unittest.mock import patch
from types import SimpleNamespace

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import operations


class PairedBackupTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="utaped-ops-test-")
        self.directory = Path(self.temporary.name).resolve()
        assert self.directory.parent == Path(tempfile.gettempdir()).resolve() and self.directory.name.startswith("utaped-ops-test-")
        self.addCleanup(self.temporary.cleanup)
        self.key = "01234567-89ab-cdef-0123-456789abcdef"
        self.content = b"test-only-document-bytes"
        (self.directory / "database.dump").write_bytes(b"test-only-dump")
        self.manifest = {"schemaVersion": 1, "storagePrefix": "documents", "metadata": {"files": [{"id": self.key, "key": self.key, "sha256": hashlib.sha256(self.content).hexdigest(), "bytes": len(self.content)}]}, "checksums": {}}
        self.archive("./documents/" + self.key, self.content)

    def archive(self, name, content, link=False):
        with tarfile.open(self.directory / "documents.tar.gz", "w:gz") as archive:
            member = tarfile.TarInfo(name)
            if link:
                member.type = tarfile.SYMTYPE
                member.linkname = "/etc/passwd"
                archive.addfile(member)
            else:
                member.size = len(content)
                archive.addfile(member, io.BytesIO(content))
        self.manifest["checksums"] = {name: operations.checksum(self.directory / name) for name in ("database.dump", "documents.tar.gz")}
        (self.directory / "manifest.json").write_text(json.dumps(self.manifest), encoding="utf-8")

    def test_matching_database_metadata_and_document_archive(self):
        self.assertEqual(self.manifest, operations.verify_backup(self.directory))

    def test_archive_corruption_is_rejected_before_restore(self):
        with (self.directory / "database.dump").open("ab") as stream:
            stream.write(b"tampered")
        with self.assertRaises(RuntimeError):
            operations.verify_backup(self.directory)

    def test_missing_and_mismatched_document_bytes_are_rejected(self):
        for name, content in [("documents/other", self.content), ("documents/" + self.key, b"incorrect-document")]:
            self.archive(name, content)
            with self.assertRaises(RuntimeError):
                operations.verify_backup(self.directory)

    def test_traversal_and_links_never_reach_extraction(self):
        for name, link in [("../escape", False), ("/absolute", False), ("documents/link", True)]:
            self.archive(name, self.content, link)
            with self.assertRaises(RuntimeError):
                operations.verify_backup(self.directory)

    def test_restore_refuses_main_and_existing_projects_without_mutating_them(self):
        with patch.object(operations, "run") as execute:
            with self.assertRaises(RuntimeError):
                operations.restore(SimpleNamespace(project="utaped", backup=self.directory))
            execute.assert_not_called()
        with patch.object(operations, "containers", return_value=["existing"]), patch.object(operations, "run") as execute:
            with self.assertRaises(RuntimeError):
                operations.restore(SimpleNamespace(project="utaped-restore-existing", backup=self.directory))
            execute.assert_not_called()

    def test_failed_backup_restarts_only_writers_that_were_running(self):
        backend = {"Id": "backend-test", "State": {"Running": True}, "Config": {"Env": ["DOCUMENT_STORAGE_ROOT=/app/runtime-data/documents"]}, "Mounts": [{"Destination": "/app/runtime-data", "Type": "volume", "Name": "test-volume"}]}
        frontend = {"Id": "frontend-test", "State": {"Running": False}}
        db = {"Id": "db-test", "Config": {"Env": ["POSTGRES_USER=test", "POSTGRES_DB=test"]}}
        def execute(args, **kwargs):
            if "pg_dump" in args:
                raise RuntimeError("test-only dump failure")
            return b""
        with patch.object(operations, "one", side_effect=[backend, db, frontend]), patch.object(operations, "sql", side_effect=["180000", json.dumps({"files": []})]), patch.object(operations, "run", side_effect=execute) as process:
            with self.assertRaises(RuntimeError):
                operations.backup(SimpleNamespace(project="test-source", destination=self.directory / "failed-backup"))
            self.assertTrue(any(call.args[0] == ["docker", "start", "backend-test"] for call in process.call_args_list))
            self.assertFalse((self.directory / "failed-backup/manifest.json").exists())


if __name__ == "__main__":
    unittest.main()

import importlib.util
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
spec = importlib.util.spec_from_file_location("development_cleanup", Path(__file__).resolve().parents[1] / "clean-development-data.py")
cleanup = importlib.util.module_from_spec(spec)
spec.loader.exec_module(cleanup)


class CleanupGuards(unittest.TestCase):
    def setUp(self):
        self.target = {"Id": "fixture", "State": {"Running": False}}
        self.one = patch.object(cleanup, "one", return_value=self.target).start()
        self.environment = patch.object(cleanup, "environment", return_value={"POSTGRES_DB": "utaped_dev", "SPRING_PROFILES_ACTIVE": "development-seed"}).start()
        self.sql = patch.object(cleanup, "sql", side_effect=["utaped_dev", json.dumps({"users": 46, "documents": 122}), "7"]).start()
        self.run = patch.object(cleanup, "run").start()
        self.addCleanup(patch.stopall)

    def test_rejects_non_development_database_before_mutation(self):
        self.environment.return_value = {"POSTGRES_DB": "utaped"}
        with self.assertRaisesRegex(RuntimeError, "development database"):
            cleanup.clean(Path("unused"), True)
        self.sql.assert_not_called()
        self.run.assert_not_called()

    def test_rejects_missing_seed_profile_before_mutation(self):
        self.environment.return_value = {"POSTGRES_DB": "utaped_dev"}
        with self.assertRaisesRegex(RuntimeError, "development-seed"):
            cleanup.clean(Path("unused"), True)
        self.assertEqual(self.sql.call_count, 1)
        self.run.assert_not_called()

    def test_invalid_backup_blocks_cleanup(self):
        with patch.object(cleanup, "verify_backup", side_effect=RuntimeError("Invalid backup")):
            with self.assertRaisesRegex(RuntimeError, "Invalid backup"):
                cleanup.clean(Path("unused"), True)
        self.assertEqual(self.sql.call_count, 3)
        self.run.assert_not_called()

    def test_data_changed_since_backup_blocks_truncate(self):
        self.sql.side_effect = ["utaped_dev", json.dumps({"users": 46, "documents": 122}), "7", json.dumps({"users": 46, "documents": 123})]
        with patch.object(cleanup, "verify_backup", return_value={"sourceProject": "utaped-sample", "metadata": {"users": 46, "documents": 122}}):
            with self.assertRaisesRegex(RuntimeError, "Data changed"):
                cleanup.clean(Path("unused"), True)
        self.assertEqual(self.sql.call_count, 4)
        self.assertTrue(all("TRUNCATE" not in call.args[1] for call in self.sql.call_args_list))


if __name__ == "__main__":
    unittest.main()

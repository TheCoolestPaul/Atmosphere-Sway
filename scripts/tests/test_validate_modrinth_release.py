import importlib.util
import json
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).parents[1] / "validate_modrinth_release.py"
SPEC = importlib.util.spec_from_file_location("validate_modrinth_release", MODULE_PATH)
validator = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(validator)


class ManifestValidationTests(unittest.TestCase):
    def write_manifest(self, payload):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        path = Path(temporary.name) / "dependencies.json"
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def valid_payload(self):
        return {
            "atmos_version": "1.5.0",
            "dependencies": [
                {"name": "Required", "project_id": "Gj6Yce3v", "dependency_type": "required"},
                {"name": "Optional", "project_id": "fFEIiSDQ", "dependency_type": "optional"},
            ],
        }

    def test_valid_manifest(self):
        dependencies = validator.load_manifest(self.write_manifest(self.valid_payload()), "1.5.0")
        self.assertEqual(2, len(dependencies))

    def test_version_must_be_reviewed(self):
        with self.assertRaisesRegex(validator.ValidationError, "has not been reviewed"):
            validator.load_manifest(self.write_manifest(self.valid_payload()), "1.6.0")

    def test_duplicate_project_is_rejected(self):
        payload = self.valid_payload()
        payload["dependencies"].append(
            {"name": "Duplicate", "project_id": "Gj6Yce3v", "dependency_type": "optional"}
        )
        with self.assertRaisesRegex(validator.ValidationError, "Duplicate"):
            validator.load_manifest(self.write_manifest(payload), "1.5.0")

    def test_unknown_relationship_is_rejected(self):
        payload = self.valid_payload()
        payload["dependencies"][1]["dependency_type"] = "recommended"
        with self.assertRaisesRegex(validator.ValidationError, "Unsupported"):
            validator.load_manifest(self.write_manifest(payload), "1.5.0")


class ExistingVersionValidationTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.artifact = Path(temporary.name) / "atmossway-1.5.0-NEO-1.21.1.jar"
        self.artifact.write_bytes(b"release jar")
        self.dependencies = [
            {"name": "Required", "project_id": "Gj6Yce3v", "dependency_type": "required"},
            {"name": "Optional", "project_id": "fFEIiSDQ", "dependency_type": "optional"},
        ]

    def version_payload(self):
        return {
            "name": "Atmospheric Wind Sway 1.5.0",
            "version_type": "release",
            "loaders": ["neoforge"],
            "game_versions": ["1.21.1"],
            "files": [
                {
                    "primary": True,
                    "filename": self.artifact.name,
                    "hashes": {"sha512": validator.sha512(self.artifact)},
                }
            ],
            "dependencies": [
                {"project_id": "Gj6Yce3v", "version_id": None, "dependency_type": "required"},
                {"project_id": "fFEIiSDQ", "version_id": None, "dependency_type": "optional"},
            ],
        }

    def validate(self, payload):
        validator.validate_existing_version(
            payload,
            "Atmospheric Wind Sway 1.5.0",
            self.artifact,
            "neoforge",
            "1.21.1",
            self.dependencies,
        )

    def test_matching_release_is_safe_to_reuse(self):
        self.validate(self.version_payload())

    def test_conflicting_jar_is_rejected(self):
        payload = self.version_payload()
        payload["files"][0]["hashes"]["sha512"] = "0" * 128
        with self.assertRaisesRegex(validator.ValidationError, "differs"):
            self.validate(payload)

    def test_exact_dependency_pin_is_rejected(self):
        payload = self.version_payload()
        payload["dependencies"][0]["version_id"] = "version1"
        with self.assertRaisesRegex(validator.ValidationError, "exact-version"):
            self.validate(payload)

    def test_changed_dependency_set_is_rejected(self):
        payload = self.version_payload()
        payload["dependencies"].pop()
        with self.assertRaisesRegex(validator.ValidationError, "dependencies differ"):
            self.validate(payload)


if __name__ == "__main__":
    unittest.main()

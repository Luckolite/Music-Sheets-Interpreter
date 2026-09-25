"""Original packaging checks for the licensed single-glyph recognition resources."""
import importlib.util
import json
import shutil
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("glyph_build_java", ROOT / "scripts/build_java.py")
build_java = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build_java)
RESOURCE = Path("java/src/main/resources")
GLYPHS = Path("io/github/luckolite/interpreter/glyphs")


class GlyphResourceTest(unittest.TestCase):
    def test_manifest_accounts_for_exactly_twenty_seven_templates_and_two_notices(self):
        rows = build_java.resource_snapshots()
        self.assertEqual(27, sum(path.suffix == ".png" for path, _ in rows))
        self.assertEqual(2, sum(path.name.endswith("-OFL.txt") for path, _ in rows))
        self.assertEqual(1, sum(path.name == "manifest.json" for path, _ in rows))

    def test_changed_glyph_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            shutil.copytree(ROOT / RESOURCE, root / RESOURCE)
            path = root / RESOURCE / GLYPHS / "dynamic_templates/p.png"
            path.write_bytes(path.read_bytes() + b"changed")
            with self.assertRaisesRegex(ValueError, "checksum mismatch"):
                build_java.resource_snapshots(root)

    def test_unreviewed_extra_file_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            shutil.copytree(ROOT / RESOURCE, root / RESOURCE)
            (root / RESOURCE / GLYPHS / "extra.png").write_bytes(b"unreviewed")
            with self.assertRaisesRegex(ValueError, "account for every"):
                build_java.resource_snapshots(root)

    def test_jar_contains_verified_bytes_and_notices(self):
        jar = ROOT / "src/sheet_interpreter/interpreter.jar"
        if not jar.is_file():
            self.skipTest("Build the standalone JAR before checking packaged resources")
        with zipfile.ZipFile(jar) as bundle:
            for path, data in build_java.resource_snapshots():
                self.assertEqual(data, bundle.read(path.relative_to(ROOT / RESOURCE).as_posix()))


if __name__ == "__main__":
    unittest.main()

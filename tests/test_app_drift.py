# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
import hashlib
import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    'check_app_drift', Path(__file__).resolve().parents[1] / 'scripts/check_app_drift.py')
drift = importlib.util.module_from_spec(spec)
spec.loader.exec_module(drift)


class AppDriftTest(unittest.TestCase):
    def test_line_endings_do_not_hide_real_changes(self):
        for baseline in (b'first\nsecond\n', b'first\r\nsecond\r\n'):
            expected = hashlib.sha256(baseline).hexdigest()
            self.assertTrue(drift.matches_source(b'first\nsecond\n', expected))
            self.assertTrue(drift.matches_source(b'first\r\nsecond\r\n', expected))
            self.assertFalse(drift.matches_source(b'first\nchanged\n', expected))

    def test_missing_and_modified_sources_require_review(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'Decoder.java'
            source.write_bytes(b'original\n')
            provenance = {'files': {'Decoder.java': {
                'source_path': 'Decoder.java',
                'source_sha256': hashlib.sha256(source.read_bytes()).hexdigest()}}}
            self.assertEqual((1, []), drift.check(root, provenance))
            source.write_bytes(b'fixed\n')
            self.assertEqual((1, ['Decoder.java']), drift.check(root, provenance))
            source.unlink()
            self.assertEqual((1, ['Decoder.java']), drift.check(root, provenance))

    def test_invalid_mapping_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError):
                drift.check(Path(directory), {'files': {'bad': {
                    'source_path': '../outside.java', 'source_sha256': 'unused'}}})
            with self.assertRaises(ValueError):
                drift.check(Path(directory), {'files': {}})

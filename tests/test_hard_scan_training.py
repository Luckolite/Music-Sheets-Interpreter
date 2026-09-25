# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Include original hard-scan regressions in the normal CPU test command."""
from pathlib import Path
from importlib.util import find_spec
import sys
import unittest

if find_spec('cv2') is None:
    raise unittest.SkipTest('OpenCV is an optional training dependency')

TRAINING = Path(__file__).resolve().parents[1] / 'training'
sys.path.insert(0, str(TRAINING))


def load_tests(loader, tests, pattern):
    # A nested discover mutates its loader's top-level directory. Keep the
    # outer tests/ loader intact so later standalone tests remain discoverable.
    return unittest.TestLoader().discover(str(TRAINING), pattern='test_hard_scan_*.py', top_level_dir=str(TRAINING))

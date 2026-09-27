"""Public packaging must exclude local assistant instructions and session state."""

import importlib.util
from pathlib import Path
import unittest


spec = importlib.util.spec_from_file_location(
    "verify_package", Path(__file__).resolve().parents[1] / "scripts/verify_package.py"
)
verification = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verification)


class PackageBoundaryTests(unittest.TestCase):
    def test_local_instructions_are_not_public_artifacts(self):
        for path in (
            "AGENTS.md",
            "java/AGENTS.md",
            "CLAUDE.md",
            "skills/read/SKILL.md",
        ):
            with self.subTest(path=path):
                self.assertTrue(verification.is_assistant_artifact(path))

    def test_agent_state_and_scratch_are_not_public_artifacts(self):
        for path in (
            ".codex/state.json",
            "tmp/resume.json",
            "scratch/crop.png",
            "docs/resume-latest.json",
            ".claude/settings.json",
            r"java\.agents\notes.md",
            "MEMORY.md",
        ):
            with self.subTest(path=path):
                self.assertTrue(verification.is_assistant_artifact(path))

    def test_origin_records_and_real_developer_tools_remain_allowed(self):
        for path in (
            "docs/source-provenance.json",
            "models/MODEL_CARD.md",
            "training/audit_corpus.py",
            "docs/validation-v0.1.0.json",
            "java/src/ScoreNavigationPlan.java",
            "CONTRIBUTING.md",
        ):
            with self.subTest(path=path):
                self.assertFalse(verification.is_assistant_artifact(path))


if __name__ == "__main__":
    unittest.main()

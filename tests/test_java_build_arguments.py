# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Exercise javac with source paths exceeding Windows' process argument limit."""
import importlib.util
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('argument_build_java', ROOT / 'scripts/build_java.py')
build_java = importlib.util.module_from_spec(spec)
spec.loader.exec_module(build_java)


class JavaBuildArgumentsTest(unittest.TestCase):
    def test_long_source_list_with_spaces_compiles_every_fingerprinted_source(self):
        with tempfile.TemporaryDirectory(prefix='javac arguments ') as folder:
            root = Path(folder)
            source = root / ('source snapshots # quoted ' + 'a' * 65)
            source.mkdir()
            classes = root / 'compiled classes # output'
            classes.mkdir()
            sources = []
            for index in range(300):
                path = source / ('GeneratedControl%03d.java' % index)
                path.write_text('public class %s {}\n' % path.stem, encoding='utf-8')
                sources.append(path)
            # The original inline command cannot be created by Windows.
            inline = [build_java.jdk_tool('javac'), '--release', '17', '-encoding',
                      'UTF-8', '-d', str(classes), *map(str, sources)]
            self.assertGreater(len(subprocess.list2cmdline(inline)), 32767)
            build_java.compile_sources(sources, classes, root / 'compiler arguments.txt')
            self.assertEqual({path.stem for path in sources},
                             {path.stem for path in classes.glob('*.class')})
            for path in classes.glob('*.class'):
                self.assertEqual(path.read_bytes()[:8], b'\xca\xfe\xba\xbe\x00\x00\x00\x3d')


if __name__ == '__main__':
    unittest.main()

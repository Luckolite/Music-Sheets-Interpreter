#!/usr/bin/env python3
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Run generated-geometry regression tests, verifying downloaded test dependencies."""
import hashlib
import json
import os
import subprocess
import urllib.request
from pathlib import Path
from build_java import ROOT, jdk_tool


def main():
    dependencies = json.loads((ROOT / 'scripts/test-dependencies.json').read_text())
    jars = []
    for name, info in dependencies.items():
        path = ROOT / 'build/test-deps' / (name + '.jar')
        path.parent.mkdir(parents=True, exist_ok=True)
        if not path.exists():
            path.write_bytes(urllib.request.urlopen(info['url'], timeout=60).read())
        if hashlib.sha256(path.read_bytes()).hexdigest() != info['sha256']:
            raise SystemExit('Test dependency checksum mismatch: ' + name)
        jars.append(path)
    sources = sorted((ROOT / 'java/src/test/java').rglob('*Test.java'))
    classes = ROOT / 'build/test-classes'
    classes.mkdir(exist_ok=True)
    cp = os.pathsep.join(map(str, [ROOT / 'build/classes', *jars]))
    source_list = ROOT / 'build/test-sources.txt'
    source_list.write_text('\n'.join('"' + p.relative_to(ROOT).as_posix() + '"' for p in sources),
                           encoding='utf-8')
    subprocess.run([jdk_tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-cp', cp,
                    '-d', str(classes), '@' + str(source_list)], cwd=ROOT, check=True)
    subprocess.run([jdk_tool('java'), '-cp', os.pathsep.join([str(classes), cp]),
                    'org.junit.runner.JUnitCore', *['io.github.luckolite.interpreter.' + p.stem for p in sources]], check=True)


if __name__ == '__main__':
    main()

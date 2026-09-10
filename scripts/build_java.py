#!/usr/bin/env python3
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Build the standalone Java 17 core with the JDK only; no Gradle, Maven or Android SDK."""
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def jdk_tool(name):
    suffix = '.exe' if os.name == 'nt' else ''
    if os.environ.get('JAVA_HOME'):
        path = Path(os.environ['JAVA_HOME']) / 'bin' / (name + suffix)
        if path.is_file():
            return str(path)
    found = shutil.which(name)
    if not found:
        raise SystemExit('Install JDK 17 or newer and set JAVA_HOME (missing ' + name + ')')
    return found


def main():
    sources = sorted((ROOT / 'java/src/main/java').rglob('*.java'))
    classes = ROOT / 'build/classes'
    classes.mkdir(parents=True, exist_ok=True)
    subprocess.run([jdk_tool('javac'), '--release', '17', '-encoding', 'UTF-8', '-d', str(classes),
                    *map(str, sources)], check=True)
    target = ROOT / 'src/sheet_interpreter/interpreter.jar'
    subprocess.run([jdk_tool('jar'), '--create', '--file', str(target), '--main-class',
                    'io.github.luckolite.interpreter.Main', '-C', str(classes), '.',
                    '-C', str(ROOT), 'LICENSE', '-C', str(ROOT), 'NOTICE'], check=True)
    data = ROOT / 'src/sheet_interpreter/data'
    data.mkdir(exist_ok=True)
    for source in (ROOT / 'models/music_sheets_v3_float16.tflite', ROOT / 'LICENSE', ROOT / 'NOTICE'):
        shutil.copyfile(source, data / source.name)
    print(target)


if __name__ == '__main__':
    main()

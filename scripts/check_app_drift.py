#!/usr/bin/env python3
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Flag app source changes since the last reviewed standalone extraction.

Read-only: this never copies private app files or updates provenance hashes.
It checks mapped source files, not semantic parity or newly introduced helpers.
"""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def matches_source(data, expected):
    # The original extraction recorded a mix of LF and CRLF source files.
    # Accept either checkout convention while preserving every other byte.
    lf = data.replace(b'\r\n', b'\n')
    return expected in {hashlib.sha256(value).hexdigest()
                        for value in (data, lf, lf.replace(b'\n', b'\r\n'))}


def check(app, provenance):
    app = app.resolve()
    changed = []
    checked = 0
    for name, entry in provenance['files'].items():
        if 'source_path' not in entry:
            continue
        checked += 1
        source = (app / entry['source_path']).resolve()
        if not source.is_relative_to(app):
            raise ValueError('Source mapping escapes the app root: ' + name)
        if not source.is_file() or not matches_source(source.read_bytes(), entry['source_sha256']):
            changed.append(entry['source_path'])
    if not checked:
        raise ValueError('No mapped sources to check')
    return checked, changed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--app', required=True, type=Path, help='Active Music Sheets app worktree')
    args = parser.parse_args()
    provenance = json.loads((ROOT / 'docs/source-provenance.json').read_text(encoding='utf-8'))
    checked, changed = check(args.app, provenance)
    if changed:
        print('Review and port these app changes before completing the interpreter fix:')
        for path in changed:
            print('  ' + path)
        return 1
    print(f'All {checked} mapped app sources match their reviewed provenance.')
    print('Also review new helpers, adapters, training code and model assets when applicable.')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())

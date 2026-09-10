#!/usr/bin/env python3
# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Check public assets, source boundary and license material before packaging."""
import hashlib
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
    tracked = [p for p in tracked if p]
    if not tracked:
        raise SystemExit('Stage the intended public files before running this verification')
    banned = re.compile(r'(^|/)(?:\.venv[^/]*|build|dist|tmp|\.signing|backups|diagnostic-export)(/|$)|\.(?:apk|keystore|jks|bundle|page\.gz)$', re.I)
    credential = re.compile(r'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,}|-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----)')
    for relative in tracked:
        if banned.search(relative):
            raise SystemExit('Private/build artifact in public staging: ' + relative)
        path = ROOT / relative
        if path.stat().st_size >= 100 * 1024 * 1024:
            raise SystemExit('Oversize Git object: ' + relative)
        if path.suffix in ('.py', '.java', '.json', '.md', '.toml', '.yml'):
            text = path.read_text(encoding='utf-8')
            if credential.search(text) or re.search(r'[A-Za-z]:[\\/]+Users[\\/]', text):
                raise SystemExit('Credential or personal machine path found: ' + relative)
            if path.suffix == '.java' and 'android.' in text:
                raise SystemExit('Android dependency in standalone Java: ' + relative)
    artifacts = json.loads((ROOT / 'models/artifacts.json').read_text())
    for name, expected in artifacts.items():
        path = ROOT / 'models' / name
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected['sha256']:
            raise SystemExit('Model checksum mismatch: ' + name)
    for name in ('LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md', 'models/MODEL_CARD.md',
                 'training/fonts/NOTO-SERIF-OFL.txt', 'training/fonts/BRAVURA-OFL.txt', 'training/fonts/LELAND-OFL.txt'):
        if name not in tracked:
            raise SystemExit('Required public license/document is not staged: ' + name)
    print('Verified', len(tracked), 'public files, independent model checksums and license notices')


if __name__ == '__main__':
    main()

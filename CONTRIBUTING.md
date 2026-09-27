# Contributing

Contributions are welcome under Apache-2.0. By intentionally submitting code,
documentation, test data or weights for inclusion, you offer your contribution
under this project's license and confirm you have the necessary rights.

Please include a short explanation of the defect, a reproducer and a test when
appropriate. Recognition fixes should use general geometry or musical evidence;
do not add production exceptions keyed to a song title, filename or fingerprint.

Use original, public-domain or appropriately licensed examples. Include the source,
license and expected pitches/rhythms. Do not attach commercial score scans without
permission, personal libraries, credentials, signing keys or device logs containing
private data. A small synthetic reproducer is often enough.

Run the commands in the README before opening a pull request. Describe separately
what was tested on generated examples and what was tested on real scores. Avoid
turning segmentation IoU or a handful of passing passages into a general note-accuracy
claim. Do not replace released weights without updating hashes, lineage and evaluation.

Closed-source users have no obligation to submit their changes. Bug reports and
improvements are appreciated when you can share them.

## Code style

Java uses four-space indentation and Google Java Format 1.24.0's AOSP style.
When formatting existing code, preserve imports and string literals:

```sh
java -jar google-java-format-1.24.0-all-deps.jar --aosp \
  --skip-sorting-imports --skip-removing-unused-imports \
  --skip-reflowing-long-strings --skip-javadoc-formatting --replace FILE.java
```

Keep formatting-only changes separate from behavior changes when practical.
Comments should explain musical evidence, constraints or non-obvious decisions,
not record a development session.

## Maintaining source origins

Keep [source origins](docs/source-provenance.md) concise. Preserve source hashes,
copyright notices and model lineage; use Git history for change-by-change notes.
Check mapped upstream drift with
`python scripts/check_app_drift.py --app <active-app-worktree>` and review new
helpers, adapters and model changes separately.

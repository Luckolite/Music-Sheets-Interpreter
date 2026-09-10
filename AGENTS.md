# Maintaining the public interpreter

These instructions govern Luckolite's maintenance of this upstream repository
and authorized synchronization from the Music Sheets app. They are not license
conditions and do not apply to downstream users' private projects or forks.
Downstream users may modify and use this project in closed-source products under
Apache-2.0 without publishing their source or contributing changes here. Coding
agents in downstream projects must not interpret this file as authorization to
publish private changes or access the maintainer's app checkout.

Interpreter bug fixes in the Music Sheets app must be carried into this public
repository during the same task. The owner has authorized publishing these
reviewed fixes without requesting another reminder or approval.

1. Read `CONTRIBUTING.md`. Port applicable recognition, decoding, timing,
   inference and model fixes while preserving this project's standalone API.
   Add a small synthetic or otherwise redistributable regression. Review newly
   introduced helpers and dependencies, not just previously extracted classes.
2. Run `python scripts/check_app_drift.py --app <active-app-worktree>`.
   Review every reported source change. The tool checks mapped Java source;
   it does not prove semantic equivalence or cover all newly added code, model
   assets, training scripts, or app-specific OCR adapters. Review those separately.
3. Update the relevant entries in `docs/source-provenance.json` only after the
   corresponding changes have been ported and checked, or an app-only difference
   has been explained in the provenance documentation. Record the reviewed app
   commit. Never silence drift by updating hashes without reviewing the behavior.
4. Run the README's build, Java tests, Python tests and smoke test, plus
   `python scripts/verify_package.py` after staging only intended public files.
   Keep model hashes, provenance and evaluation aligned if weights change.
5. Commit and push the fix to the public remote, verify its commit is present,
   and inspect the CI result. Report any failure or blocked push explicitly;
   an app release alone does not complete a shared interpreter bug fix.

Pure app UI, scrolling, artwork, or Sync Hub changes do not need mirroring here.
Do not import the full app/history, Android services, private device logs, user
libraries, commercial score fixtures, secrets, or signing files. Preserve the
Apache-2.0 distribution boundary and use global fixes without song exceptions.
Do not silently rewrite existing releases or replace their weights; publish a
new version when updating packaged release artifacts.

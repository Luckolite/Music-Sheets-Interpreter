# Using this in a closed-source project

The original code, documentation, synthetic examples and model weights are licensed
under **Apache License 2.0**. This includes `models/best.pt`, `models/weights.npz` and
`models/music_sheets_v3_float16.tflite`, and the copies distributed in release packages.

You can embed the decoder or weights in a commercial application, run them locally
or on your own servers, charge for your product and keep your application's source
and your modifications private. Contributions back are welcome but not required.
Apache 2.0 also includes a contributor patent grant with the scope and termination
conditions stated in section 3.

When you redistribute covered material:

- Provide a copy of [LICENSE](../LICENSE).
- Retain applicable copyright and attribution notices, including [NOTICE](../NOTICE).
- Mark files you modify with a prominent notice that you changed them.
- Follow the separate licenses of any dependencies or font files you distribute.

These points summarize the license; its full text controls. See the Apache Software
Foundation's [license](https://www.apache.org/licenses/LICENSE-2.0) and
[FAQ](https://www.apache.org/foundation/license-faq.html). No special commercial
permission, license fee or source-code release is required by this project's license.

## Scope and third-party material

The Apache license covers the material we control, not third-party software generally.
`training/fonts/NotoSerif.ttf` retains SIL OFL 1.1 with its notice. Bravura and Leland
glyphs used during training come from the separately installed Verovio package and
retain their upstream notices. Verovio is an optional external training/rendering tool
under LGPL-3.0; it is not linked into or bundled with this project's runtime JAR or wheel.

The runtime dependencies are installed separately. Their source and bundled binary
components can have additional notices and conditions. See [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)
before redistributing a complete runtime environment.

This repository does not contain HOMR, Andromr or oemer weights or code. Those projects'
licenses and their authors' rights remain unchanged. It does not grant rights in the
music scores a user supplies, or permission to redistribute someone else's score.

The recognition model emits class labels and musical events; it is not a font model.
The OFL FAQ explains ordinary [graphical use and AI design use](https://openfontlicense.org/ofl-faq/)
separately from creating a derivative font. The OFL font files themselves are kept
under OFL and are not relabeled as Apache-2.0.

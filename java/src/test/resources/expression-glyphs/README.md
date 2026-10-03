These sprites contain individual font glyphs only. The tests construct original
synthetic rails, written columns and pulse relations; no score scan is included.

Source: [Bravura](https://github.com/steinbergmedia/bravura), SIL OFL 1.1.
The complete notice is bundled at
`java/src/main/resources/io/github/luckolite/interpreter/glyphs/BRAVURA-OFL.txt`.
The font checksum and production template codepoints are recorded in
`java/src/main/resources/io/github/luckolite/interpreter/glyphs/expression_templates/glyphs.json`.

Regenerate with `scripts/build_expression_glyphs.py --font PATH_TO_BRAVURA
--output java/src/main/resources/io/github/luckolite/interpreter/glyphs/expression_templates/glyphs.bin
--fixture-dir java/src/test/resources/expression-glyphs` using Pillow.
The independent test sprites have a 96-pixel height; production templates have a
64-pixel height. Tests resize them again, including negative text and bow controls.

Expected relations: quarter = half has ratio 2; dotted eighth = quarter has
ratio 4/3; sixteenth = eighth has ratio 2; half-down = quarter-down has ratio
1/2; whole = quarter has ratio 1/4. Both breath shapes belong to the preceding
written note's release. Digits, isolated letters, text punctuation, an upbow,
numeric tempo and a page without printed rails must not create these events.

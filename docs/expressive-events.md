# Expressive event data

`ScoreAnchor` identifies a logical measure and musical quarter-beat offset, not
an engraving fraction. Canonicalization checks the meter and score bounds and
maps a bar's exact end to the next bar's zero, including the terminal boundary.

`ScoreExpressiveEvent` separates printed semantics from playback realization.
It retains source identity, staff context, raw evidence, qualifiers and optional
resolved start/end anchors. Unresolved timing is explicit. A resolved fermata
requires an owned target and written release, including silent rest targets.
Offsets preserve source evidence and identity; navigation must assign separate
performance occurrence identities.

`ExpressiveDirectionText` parses supplied text, not page geometry. It keeps
gradual slowing distinct from ritenuto, retains qualified restorations and
same-tempo instructions, distinguishes attack dynamics from persistent levels,
and can emit both crescendo and slowing semantics from one compound phrase.
`a poco a poco` remains progression wording, not a standalone POCO-strength
shortcut. Curated unsupported expressive wording remains explicit; arbitrary
prose is not treated as a direction. No BPM, pause duration or scope is invented.

The page model and its copy methods preserve these events. Supported recognition,
navigation and exports use the evidence and timing rules described in
[the preview policy](expressive-performance.md). Other semantic kinds remain
explicit data until their own recognition and realization are implemented.
Do not infer support or reinterpret old tempo positions from the existence of a
kind alone.

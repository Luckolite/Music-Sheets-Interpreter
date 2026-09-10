// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** A printed key-signature change at the start of a logical measure. */
public record ScoreKeyChange(int measureIndex, int fifths) {
    public ScoreKeyChange {
        if (measureIndex < 0) throw new IllegalArgumentException("Negative measure index");
        if (fifths < -7 || fifths > 7) throw new IllegalArgumentException("Invalid key signature");
    }
}

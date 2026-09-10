// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** A printed quarter-note tempo change inside a logical measure. */
public record ScoreTempoChange(int measureIndex, float positionInMeasure, int bpm) {
    public ScoreTempoChange {
        if (measureIndex < 0) throw new IllegalArgumentException("Negative measure index");
        if (!Float.isFinite(positionInMeasure) || positionInMeasure < 0f || positionInMeasure > 1f)
            throw new IllegalArgumentException("Invalid measure position");
        if (bpm < 15 || bpm > 1600) throw new IllegalArgumentException("Invalid quarter-note tempo");
    }
}

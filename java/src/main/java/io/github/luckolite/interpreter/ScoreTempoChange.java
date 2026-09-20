// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** A tempo change: bpm is in quarter notes/minute; beatUnit is the printed pulse in quarters. */
public record ScoreTempoChange(int measureIndex, float positionInMeasure, double bpm, double beatUnit) {
    public ScoreTempoChange(int measureIndex, float positionInMeasure, double bpm) {
        this(measureIndex, positionInMeasure, bpm, 1);
    }
    public ScoreTempoChange {
        if (measureIndex < 0) throw new IllegalArgumentException("Negative measure index");
        if (!Float.isFinite(positionInMeasure) || positionInMeasure < 0f || positionInMeasure > 1f)
            throw new IllegalArgumentException("Invalid measure position");
        if (!Double.isFinite(bpm) || bpm < 15 || bpm > 1600) throw new IllegalArgumentException("Invalid quarter-note tempo");
        if (!Double.isFinite(beatUnit) || beatUnit <= 0 || beatUnit > 4)
            throw new IllegalArgumentException("Invalid printed beat unit");
    }
}

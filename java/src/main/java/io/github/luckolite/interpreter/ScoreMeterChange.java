// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** Printed time signature, effective at the beginning of a logical measure. */
public record ScoreMeterChange(int measureIndex, int numerator, int denominator) {
    public ScoreMeterChange {
        if (measureIndex < 0 || numerator < 1 || numerator > 32
                || denominator < 1 || denominator > 32
                || (denominator & (denominator - 1)) != 0)
            throw new IllegalArgumentException("Invalid score meter");
    }

    public float quarterBeats() { return numerator * 4f / denominator; }
}

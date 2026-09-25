// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** A direction at a boundary BEFORE a source measure, not a playback occurrence.
 * A direction after the final bar has boundary == measure count. */
public record ScorePlaybackDirection(int measureBoundary, Kind kind) {
    public enum Kind { SEGNO, TO_CODA, DAL_SEGNO_AL_CODA, CODA }
    public ScorePlaybackDirection {
        if(measureBoundary<0||kind==null)throw new IllegalArgumentException("Invalid playback direction");
    }
    public ScorePlaybackDirection offset(int measureOffset) {
        return new ScorePlaybackDirection(Math.addExact(measureBoundary,measureOffset),kind);
    }
}

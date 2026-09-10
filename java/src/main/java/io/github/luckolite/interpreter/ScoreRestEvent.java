// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

/** A positively identified printed short rest, kept separate from sounding notes. */
public record ScoreRestEvent(int measureIndex, float positionInMeasure, float pageY,
                             float pageHeight, int staffIndex, int staffCount, double durationBeats) {
    public ScoreRestEvent(int measureIndex,float positionInMeasure,float pageY,float pageHeight,int staffIndex,int staffCount) {
        this(measureIndex,positionInMeasure,pageY,pageHeight,staffIndex,staffCount,.25);
    }
}

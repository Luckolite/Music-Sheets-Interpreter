// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** A musical quarter-beat offset, never an engraving fraction. */
public record ScoreAnchor(int measureIndex, double quarterBeatOffset)
        implements Comparable<ScoreAnchor> {
    public ScoreAnchor {
        if (measureIndex < 0
                || !Double.isFinite(quarterBeatOffset)
                || quarterBeatOffset < 0
                || quarterBeatOffset > 128)
            throw new IllegalArgumentException("Invalid musical anchor");
    }

    public ScoreAnchor canonical(ScoreMeterMap meter, int measureCount) {
        if (measureCount < 0
                || measureIndex > measureCount
                || measureIndex == measureCount && quarterBeatOffset != 0)
            throw new IllegalArgumentException("Anchor outside score");
        if (measureIndex == measureCount) return this;
        double length = meter.beatsInMeasure(measureIndex);
        if (quarterBeatOffset > length)
            throw new IllegalArgumentException("Anchor beyond bar duration");
        return quarterBeatOffset == length ? new ScoreAnchor(measureIndex + 1, 0) : this;
    }

    public double absoluteBeat(ScoreMeterMap meter) {
        if (quarterBeatOffset > meter.beatsInMeasure(measureIndex))
            throw new IllegalArgumentException("Anchor beyond bar duration");
        return meter.startBeat(measureIndex) + quarterBeatOffset;
    }

    public ScoreAnchor offset(int measures) {
        return new ScoreAnchor(Math.addExact(measureIndex, measures), quarterBeatOffset);
    }

    @Override
    public int compareTo(ScoreAnchor other) {
        int order = Integer.compare(measureIndex, other.measureIndex);
        return order != 0 ? order : Double.compare(quarterBeatOffset, other.quarterBeatOffset);
    }
}

// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Converts logical bars to cumulative quarter-note beats without assuming equal bar lengths. */
public final class ScoreMeterMap {
    private final float opening;
    private final List<ScoreMeterChange> changes;

    public ScoreMeterMap(float opening, List<ScoreMeterChange> changes) {
        this.opening = Float.isFinite(opening) ? Math.max(.125f, Math.min(128, opening)) : 4;
        ArrayList<ScoreMeterChange> ordered = new ArrayList<>();
        if (changes != null) for (ScoreMeterChange change : changes) if (change != null) ordered.add(change);
        ordered.sort(Comparator.comparingInt(ScoreMeterChange::measureIndex));
        this.changes = List.copyOf(ordered);
    }

    public float beatsInMeasure(int measure) {
        float beats = opening;
        for (ScoreMeterChange change : changes) {
            if (change.measureIndex() > measure) break;
            beats = change.quarterBeats();
        }
        return beats;
    }

    public double startBeat(int measure) {
        int target = Math.max(0, measure), previous = 0;
        double total = 0;
        float beats = opening;
        for (ScoreMeterChange change : changes) {
            if (change.measureIndex() > target) break;
            total += (change.measureIndex() - previous) * (double) beats;
            previous = change.measureIndex();
            beats = change.quarterBeats();
        }
        return total + (target - previous) * (double) beats;
    }

    public double beatAt(int measure, double fraction) {
        return startBeat(measure) + Math.max(0, Math.min(1, fraction)) * beatsInMeasure(measure);
    }

    /** Integer part is the logical measure; fractional part is progress within that measure. */
    public double measurePosition(double beat) {
        double target = Double.isFinite(beat) ? Math.max(0, beat) : 0;
        int previous = 0;
        double total = 0;
        float beats = opening;
        for (ScoreMeterChange change : changes) {
            double next = total + (change.measureIndex() - previous) * (double) beats;
            if (target < next) return previous + (target - total) / beats;
            total = next;
            previous = change.measureIndex();
            beats = change.quarterBeats();
        }
        return previous + (target - total) / beats;
    }
}

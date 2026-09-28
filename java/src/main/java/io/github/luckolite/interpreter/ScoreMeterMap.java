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
    private final double[] performedStarts;

    public ScoreMeterMap(float opening, List<ScoreMeterChange> changes) {
        this.opening = Float.isFinite(opening) ? Math.max(.125f, Math.min(128, opening)) : 4;
        ArrayList<ScoreMeterChange> ordered = new ArrayList<>();
        if (changes != null)
            for (ScoreMeterChange change : changes) if (change != null) ordered.add(change);
        ordered.sort(Comparator.comparingInt(ScoreMeterChange::measureIndex));
        this.changes = List.copyOf(ordered);
        this.performedStarts = null;
    }

    private ScoreMeterMap(double[] starts) {
        this.opening = starts.length > 1 ? (float) (starts[1] - starts[0]) : 4;
        this.changes = List.of();
        this.performedStarts = starts;
    }

    /** Exact performed segment lengths, not newly recognized printed time signatures. */
    public static ScoreMeterMap fromPerformedDurations(List<Double> durations) {
        if (durations.size() > 1000000)
            throw new IllegalArgumentException("Too many performed segments");
        var starts = new double[durations.size() + 1];
        for (int i = 0; i < durations.size(); i++) {
            double value = durations.get(i);
            if (!Double.isFinite(value) || value <= 0 || value > 128)
                throw new IllegalArgumentException("Invalid performed segment length");
            starts[i + 1] = starts[i] + value;
            if (!Double.isFinite(starts[i + 1]) || starts[i + 1] <= starts[i])
                throw new IllegalArgumentException("Performed segment loses clock precision");
        }
        return new ScoreMeterMap(starts);
    }

    public java.util.OptionalInt performedMeasureCount() {
        return performedStarts == null
                ? java.util.OptionalInt.empty()
                : java.util.OptionalInt.of(performedStarts.length - 1);
    }

    public double quarterBeatsInMeasure(int measure) {
        if (performedStarts == null) return beatsInMeasure(measure);
        if (performedStarts.length == 1) return 4;
        int index = Math.max(0, Math.min(performedStarts.length - 2, measure));
        return performedStarts[index + 1] - performedStarts[index];
    }

    public float beatsInMeasure(int measure) {
        if (performedStarts != null)
            return Math.max(Float.MIN_VALUE, (float) quarterBeatsInMeasure(measure));
        float beats = opening;
        for (ScoreMeterChange change : changes) {
            if (change.measureIndex() > measure) break;
            beats = change.quarterBeats();
        }
        return beats;
    }

    public double startBeat(int measure) {
        if (performedStarts != null) {
            int target = Math.max(0, measure), last = performedStarts.length - 1;
            return target <= last
                    ? performedStarts[target]
                    : performedStarts[last] + (target - last) * quarterBeatsInMeasure(last);
        }
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
        return startBeat(measure)
                + Math.max(0, Math.min(1, fraction)) * quarterBeatsInMeasure(measure);
    }

    /** Integer part is the logical measure; fractional part is progress within that measure. */
    public double measurePosition(double beat) {
        double target = Double.isFinite(beat) ? Math.max(0, beat) : 0;
        if (performedStarts != null) {
            int last = performedStarts.length - 1;
            if (target >= performedStarts[last])
                return last + (target - performedStarts[last]) / quarterBeatsInMeasure(last);
            int index = java.util.Arrays.binarySearch(performedStarts, target);
            if (index >= 0) return index;
            index = -index - 2;
            return index + (target - performedStarts[index]) / quarterBeatsInMeasure(index);
        }
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

// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Finite white-key attacks inside the source note; the target keeps its ordinary attack. */
public final class GlissandoPerformance {
    public record Step(double offset, double duration, int midi) {}

    private GlissandoPerformance() {}

    public static List<Step> whiteKeys(int source, int target, double duration) {
        if (source < 0
                || source > 127
                || target < 0
                || target > 127
                || !Double.isFinite(duration)
                || duration <= 0)
            throw new IllegalArgumentException("Invalid gliss endpoints or duration");
        var pitches = new ArrayList<Integer>();
        int direction = Integer.compare(target, source);
        for (int midi = source + direction; direction != 0 && midi != target; midi += direction)
            if (switch (Math.floorMod(midi, 12)) {
                case 0, 2, 4, 5, 7, 9, 11 -> true;
                default -> false;
            }) pitches.add(midi);
        if (pitches.isEmpty()) return List.of(new Step(0, duration, source));
        double hold = duration * 2 / 3, unit = (duration - hold) / pitches.size();
        var result = new ArrayList<Step>();
        result.add(new Step(0, hold, source));
        for (int i = 0; i < pitches.size(); i++)
            result.add(
                    new Step(
                            hold + i * unit,
                            i == pitches.size() - 1 ? duration - (hold + i * unit) : unit,
                            pitches.get(i)));
        return List.copyOf(result);
    }
}

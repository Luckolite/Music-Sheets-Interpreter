// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;

/** Source-to-performance lookup backed by region-owned, bounded navigation traversal. */
public final class ScoreNavigationPlan {
    private final List<Integer> sourceMeasures;
    private final ScoreNavigationTraversal.Result traversal;
    private final boolean navigationApplied;
    private final int sourceMeasureCount;

    private ScoreNavigationPlan(int sourceCount, ScoreNavigationTraversal.Result traversal) {
        this.traversal = traversal;
        this.sourceMeasures = traversal.sourceMeasures();
        this.sourceMeasureCount = sourceCount;
        boolean changed = sourceMeasures.size() != sourceCount;
        for (int i = 0; i < sourceMeasures.size(); i++)
            if (sourceMeasures.get(i) != i) changed = true;
        this.navigationApplied = changed;
    }

    public int measureCount() {
        return sourceMeasures.size();
    }

    public int sourceMeasure(int playbackMeasure) {
        return sourceMeasures.get(playbackMeasure);
    }

    public List<Integer> sourceMeasures() {
        return sourceMeasures;
    }

    /** Compatibility alias; callers should use navigationApplied for all route kinds. */
    public boolean dalSegnoApplied() {
        return navigationApplied;
    }

    public boolean navigationApplied() {
        return navigationApplied;
    }

    public ScoreNavigationTraversal.Result traversal() {
        return traversal;
    }

    public int sourceMeasureCount() {
        return sourceMeasureCount;
    }

    /** Source clicks select the nearest occurrence to the active performance cursor. */
    public int playbackMeasure(int sourceMeasure, int preferredPlaybackMeasure) {
        int best = -1;
        long distance = Long.MAX_VALUE;
        for (int i = 0; i < sourceMeasures.size(); i++)
            if (sourceMeasures.get(i) == sourceMeasure) {
                long d = Math.abs((long) i - preferredPlaybackMeasure);
                if (d < distance) {
                    distance = d;
                    best = i;
                }
            }
        return best;
    }

    public static ScoreNavigationPlan create(
            int sourceMeasureCount, List<ScorePlaybackDirection> directions) {
        return create(sourceMeasureCount, directions, new ScoreMeterMap(4, List.of()));
    }

    public static ScoreNavigationPlan create(
            int sourceMeasureCount, List<ScorePlaybackDirection> directions, ScoreMeterMap meter) {
        return new ScoreNavigationPlan(
                sourceMeasureCount,
                ScoreNavigationTraversal.traverse(sourceMeasureCount, meter, directions));
    }
}

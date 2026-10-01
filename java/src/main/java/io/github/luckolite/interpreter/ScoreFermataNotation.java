// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;

/** One printed symbol per proved held attack, on its surviving outer chord member. */
public final class ScoreFermataNotation {
    public record Symbol(String eventId, int sourceIndex, ScoreAnchor release, boolean inverted) {}

    private ScoreFermataNotation() {}

    public static List<Symbol> plan(ScorePageInterpretation score, Collection<Integer> available) {
        var indices = new HashSet<>(available);
        for (int index : indices)
            if (index < 0 || index >= score.notes().size())
                throw new IllegalArgumentException("Engraved source index is outside the score");
        var result = new ArrayList<Symbol>();
        var resolved = ScoreFermataDetector.resolve(score, Float.NaN);
        for (var event : resolved.expressiveEvents()) {
            if (event.scope() != ScoreExpressiveEvent.Scope.NOTE
                    && event.scope() != ScoreExpressiveEvent.Scope.VOICE) continue;
            if (event.start().isEmpty() || event.end().isEmpty()) continue;
            boolean inverted = event.qualifierText().equals("fermata inverted");
            int selected = -1, extreme = inverted ? Integer.MAX_VALUE : Integer.MIN_VALUE;
            for (int index : ScoreFermataDetector.targetIndices(resolved, event)) {
                if (!indices.contains(index)) continue;
                var note = resolved.notes().get(index);
                int diatonic = note.clefBottomDiatonic() + note.staffStep();
                if (selected < 0 || (inverted ? diatonic < extreme : diatonic > extreme)) {
                    selected = index;
                    extreme = diatonic;
                }
            }
            if (selected >= 0)
                result.add(new Symbol(event.eventId(), selected, event.end().get(), inverted));
        }
        return List.copyOf(result);
    }
}

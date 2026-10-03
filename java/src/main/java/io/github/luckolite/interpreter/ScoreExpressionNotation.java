// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;

/** Written symbols derived from explicit pulse values and proved note identities, never preview time. */
public final class ScoreExpressionNotation {
    public record Pulse(String unit, int dots) {}

    public record Release(
            String eventId, int sourceIndex, ScoreAnchor release, String element, String value) {}

    private ScoreExpressionNotation() {}

    public static Pulse pulse(double quarterBeats) {
        String[] names = {"whole", "half", "quarter", "eighth", "16th", "32nd"};
        double[] factors = {1, 1.5, 1.75};
        for (int i = 0; i < names.length; i++)
            for (int dots = 0; dots < 3; dots++)
                if (quarterBeats == Math.scalb(4, -i) * factors[dots])
                    return new Pulse(names[i], dots);
        throw new IllegalArgumentException("Unsupported written metric pulse");
    }

    public static List<Release> releases(
            ScorePageInterpretation score, Collection<Integer> available, float openingBeats) {
        var indices = new HashSet<>(available);
        for (int index : indices)
            if (index < 0 || index >= score.notes().size())
                throw new IllegalArgumentException("Engraved source index is outside the score");
        var resolved = ScoreExpressionDetector.resolve(score, openingBeats);
        var result = new ArrayList<Release>();
        for (var event : resolved.expressiveEvents()) {
            if ((event.kind() != ScoreExpressiveEvent.Kind.BREATH
                            && event.kind() != ScoreExpressiveEvent.Kind.CAESURA)
                    || event.scope() == ScoreExpressiveEvent.Scope.UNRESOLVED
                    || event.start().isEmpty()) continue;
            int selected = -1, highest = Integer.MIN_VALUE;
            for (int index : ScoreExpressionDetector.targetIndices(resolved, event)) {
                if (!indices.contains(index)) continue;
                var note = resolved.notes().get(index);
                int pitch = note.clefBottomDiatonic() + note.staffStep();
                if (selected < 0 || pitch > highest) {
                    selected = index;
                    highest = pitch;
                }
            }
            if (selected >= 0)
                result.add(
                        new Release(
                                event.eventId(),
                                selected,
                                event.start().get(),
                                event.kind() == ScoreExpressiveEvent.Kind.BREATH
                                        ? "breath-mark"
                                        : "caesura",
                                event.kind() == ScoreExpressiveEvent.Kind.BREATH
                                        ? (event.qualifierText().contains("tick")
                                                ? "tick"
                                                : "comma")
                                        : ""));
        }
        return List.copyOf(result);
    }

    public static String attack(ScoreExpressiveEvent.Kind kind) {
        return switch (kind) {
            case SFORZANDO -> "sf";
            case SFORZATO -> "sfz";
            case SFORZANDO_PIANO -> "sfp";
            default -> "";
        };
    }
}

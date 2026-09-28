// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Printed navigation semantics. Beat offsets are musical quarter beats, never page fractions. */
public record ScorePlaybackDirection(int measureBoundary, Kind kind, Details details) {
    /** Stable wire IDs: the first four retain the legacy record meanings. */
    public enum Kind {
        SEGNO(0),
        TO_CODA(1),
        DAL_SEGNO_AL_CODA(2),
        CODA(3),
        REPEAT_START(4),
        REPEAT_END(5),
        ENDING(6),
        DA_CAPO(7),
        DAL_SEGNO(8),
        DA_CAPO_AL_FINE(9),
        DAL_SEGNO_AL_FINE(10),
        DA_CAPO_AL_CODA(11),
        FINE(12),
        MEASURE_REPEAT(13);

        private final int wireId;

        Kind(int wireId) {
            this.wireId = wireId;
        }

        public int wireId() {
            return wireId;
        }

        public static Kind fromWireId(int wireId) {
            for (var kind : values()) if (kind.wireId == wireId) return kind;
            throw new IllegalArgumentException("Unknown navigation kind " + wireId);
        }

        public boolean outgoing() {
            return switch (this) {
                case TO_CODA,
                                DAL_SEGNO_AL_CODA,
                                REPEAT_END,
                                DA_CAPO,
                                DAL_SEGNO,
                                DA_CAPO_AL_FINE,
                                DAL_SEGNO_AL_FINE,
                                DA_CAPO_AL_CODA,
                                FINE ->
                        true;
                default -> false;
            };
        }

        public boolean jump() {
            return switch (this) {
                case DAL_SEGNO_AL_CODA,
                                DA_CAPO,
                                DAL_SEGNO,
                                DA_CAPO_AL_FINE,
                                DAL_SEGNO_AL_FINE,
                                DA_CAPO_AL_CODA ->
                        true;
                default -> false;
            };
        }
    }

    /** Ordinary DC/DS returns omit repeats; an explicit instruction can override that policy. */
    public enum AfterJumpRepeats {
        DEFAULT,
        SKIP,
        PLAY,
        UNKNOWN
    }

    public record Details(
            double quarterBeatOffset,
            String eventId,
            String targetId,
            String codaTargetId,
            String repeatGroupId,
            int totalPlays,
            List<Integer> passes,
            Optional<ScoreAnchor> end,
            AfterJumpRepeats afterJumpRepeats,
            String printedText,
            List<ScoreExpressiveEvent.Evidence> evidence) {
        public Details {
            Objects.requireNonNull(end);
            Objects.requireNonNull(afterJumpRepeats);
            eventId = bounded(eventId, 256);
            targetId = bounded(targetId, 256);
            codaTargetId = bounded(codaTargetId, 256);
            repeatGroupId = bounded(repeatGroupId, 256);
            printedText = bounded(printedText, 4096);
            passes = List.copyOf(passes);
            evidence = List.copyOf(evidence);
            if (!Double.isFinite(quarterBeatOffset)
                    || quarterBeatOffset < 0
                    || quarterBeatOffset > 128
                    || totalPlays < 1
                    || totalPlays > 128
                    || passes.size() > 128
                    || evidence.size() > 64)
                throw new IllegalArgumentException("Invalid navigation details");
            for (int pass : passes)
                if (pass < 1 || pass > 128)
                    throw new IllegalArgumentException("Invalid ending pass");
            if (passes.stream().distinct().count() != passes.size())
                throw new IllegalArgumentException("Duplicate ending pass");
        }

        public static Details legacy() {
            return new Details(
                    0,
                    "",
                    "",
                    "",
                    "",
                    2,
                    List.of(),
                    Optional.empty(),
                    AfterJumpRepeats.DEFAULT,
                    "",
                    List.of());
        }

        private static String bounded(String value, int limit) {
            Objects.requireNonNull(value);
            if (value.length() > limit)
                throw new IllegalArgumentException("Navigation text too long");
            return value;
        }

        Details offset(int measures) {
            return new Details(
                    quarterBeatOffset,
                    eventId,
                    targetId,
                    codaTargetId,
                    repeatGroupId,
                    totalPlays,
                    passes,
                    end.map(a -> a.offset(measures)),
                    afterJumpRepeats,
                    printedText,
                    evidence);
        }
    }

    public ScorePlaybackDirection(int measureBoundary, Kind kind) {
        this(measureBoundary, kind, Details.legacy());
    }

    public ScorePlaybackDirection {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(details);
        if (measureBoundary < 0) throw new IllegalArgumentException("Invalid playback direction");
        if (!details.equals(Details.legacy()) && details.eventId().isBlank())
            throw new IllegalArgumentException(
                    "Rich navigation record needs an explicit source identity");
        if (details.end().isPresent()
                && details.end()
                                .get()
                                .compareTo(
                                        new ScoreAnchor(
                                                measureBoundary, details.quarterBeatOffset()))
                        <= 0)
            throw new IllegalArgumentException("Navigation span must have positive length");
        if (!details.passes().isEmpty() && kind != Kind.ENDING && !kind.jump())
            throw new IllegalArgumentException(
                    "Pass selectors are not supported for this navigation kind");
        if (kind == Kind.ENDING && (details.end().isEmpty() || details.passes().isEmpty()))
            throw new IllegalArgumentException("Ending needs an endpoint and pass selector");
    }

    public ScoreAnchor anchor() {
        return new ScoreAnchor(measureBoundary, details.quarterBeatOffset());
    }

    public ScorePlaybackDirection offset(int measureOffset) {
        return new ScorePlaybackDirection(
                Math.addExact(measureBoundary, measureOffset), kind, details.offset(measureOffset));
    }
}

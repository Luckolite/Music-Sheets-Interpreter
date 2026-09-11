// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
// Adapted from Music Sheets: standalone package and platform-independent diagnostics.
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;

/** Finds the interpreted note or chord sounding at the current fixed score-clock position. */
final class ActiveScoreNotes {
    private static final double SAME_ONSET_EPSILON = .001;
    private ActiveScoreNotes() { }

    static List<ScoreNoteEvent> at(List<ScoreNoteEvent> notes, int measureIndex,
                                   float beatInMeasure, float beatsPerMeasure) {
        return prepare(notes, measureIndex, beatsPerMeasure).at(beatInMeasure);
    }

    private record TimedNote(ScoreNoteEvent note, double onset, double duration, boolean held) { }

    static Prepared prepare(List<ScoreNoteEvent> notes, int measureIndex, float beatsPerMeasure) {
        List<TimedNote> timed = new ArrayList<>();
        if (notes != null && measureIndex >= 0) for (ScoreNoteEvent note : notes) {
            if (note == null || note.measureIndex() != measureIndex) continue;
            timed.add(new TimedNote(note, ScoreNoteTiming.beatInMeasure(note, notes, beatsPerMeasure),
                    ScoreNoteTiming.resolvedWrittenDurationBeats(note, notes, beatsPerMeasure),
                    ScoreNoteTiming.hasIndependentSustain(note)));
        }
        return new Prepared(List.copyOf(timed), beatsPerMeasure);
    }

    /** Resolve rhythm once per measure, then select against the advancing audio clock. */
    static final class Prepared {
        private final List<TimedNote> timed;
        private final float beatsPerMeasure;
        private Prepared(List<TimedNote> timed, float beatsPerMeasure) {
            this.timed = timed;
            this.beatsPerMeasure = beatsPerMeasure;
        }

        List<ScoreNoteEvent> at(float beatInMeasure) {
            double clock = Math.max(0, beatInMeasure);
            double activeOnset = Double.NEGATIVE_INFINITY;
            List<TimedNote> active = new ArrayList<>();
            for (TimedNote note : timed) {
                double onset = note.onset;
                if (onset > clock + .03125) continue;
                if (onset > activeOnset + SAME_ONSET_EPSILON) {
                    activeOnset = onset;
                    active.clear();
                }
                if (Math.abs(onset - activeOnset) <= SAME_ONSET_EPSILON) active.add(note);
            }
            if (!active.isEmpty()) {
                double written = Double.NaN;
                for (TimedNote note : active) {
                    double duration = note.duration;
                    if (Double.isFinite(duration)) written = Double.isFinite(written)
                            ? Math.max(written, duration) : duration;
                }
                if (Double.isFinite(written) && clock > activeOnset + written + .03125) active.clear();
            }
            for(TimedNote note:timed) {
                if(!note.held||active.contains(note))continue;
                double onset=note.onset;
                double duration=note.duration *NoteArticulation.gate(note.note.articulations());
                if(clock+.03125>=onset&&clock<Math.min(beatsPerMeasure,onset+duration))active.add(note);
            }
            List<ScoreNoteEvent> result = new ArrayList<>(active.size());
            for (TimedNote note : active) result.add(note.note);
            return List.copyOf(result);
        }
    }
}

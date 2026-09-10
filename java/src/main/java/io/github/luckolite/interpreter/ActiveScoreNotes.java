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
        if (notes == null || notes.isEmpty() || measureIndex < 0) return List.of();
        double clock = Math.max(0, beatInMeasure);
        double activeOnset = Double.NEGATIVE_INFINITY;
        List<ScoreNoteEvent> active = new ArrayList<>();
        for (ScoreNoteEvent note : notes) {
            if (note == null || note.measureIndex() != measureIndex) continue;
            double onset = ScoreNoteTiming.beatInMeasure(note, notes, beatsPerMeasure);
            if (onset > clock + .03125) continue;
            if (onset > activeOnset + SAME_ONSET_EPSILON) {
                activeOnset = onset;
                active.clear();
            }
            if (Math.abs(onset - activeOnset) <= SAME_ONSET_EPSILON) active.add(note);
        }
        if (!active.isEmpty()) {
            double written = Double.NaN;
            for (ScoreNoteEvent note : active) {
                double duration = ScoreNoteTiming.resolvedWrittenDurationBeats(
                        note, notes, beatsPerMeasure);
                if (Double.isFinite(duration)) written = Double.isFinite(written)
                        ? Math.max(written, duration) : duration;
            }
            if (Double.isFinite(written) && clock > activeOnset + written + .03125) active.clear();
        }
        for(ScoreNoteEvent note:notes) {
            if(note==null||note.measureIndex()!=measureIndex||!ScoreNoteTiming.hasIndependentSustain(note)
                    ||active.contains(note))continue;
            double onset=ScoreNoteTiming.beatInMeasure(note,notes,beatsPerMeasure);
            double duration=ScoreNoteTiming.resolvedWrittenDurationBeats(note,notes,beatsPerMeasure)
                    *NoteArticulation.gate(note.articulations());
            if(clock+.03125>=onset&&clock<Math.min(beatsPerMeasure,onset+duration))active.add(note);
        }
        return List.copyOf(active);
    }
}

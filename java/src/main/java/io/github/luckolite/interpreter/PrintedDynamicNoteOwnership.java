// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;

/** A beamed note head inside a purported letter box is independent notation evidence. */
final class PrintedDynamicNoteOwnership {
    private PrintedDynamicNoteOwnership() {}

    static boolean containsBeamedHead(
            PlayingTechniqueDetector.Word word,
            List<MeasureRegion> measures,
            List<ScoreNoteEvent> notes) {
        if (word.right() <= word.left() || word.bottom() <= word.top()) return false;
        for (var note : notes) {
            if (note.beamCount() <= 0
                    || note.measureIndex() < 0
                    || note.measureIndex() >= measures.size()) continue;
            var measure = measures.get(note.measureIndex());
            float x =
                    measure.left() + (measure.right() - measure.left()) * note.positionInMeasure();
            if (x >= word.left()
                    && x <= word.right()
                    && note.pageY() >= word.top()
                    && note.pageY() <= word.bottom()) return true;
        }
        return false;
    }
}

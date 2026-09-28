// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original symbolic fixtures; no score scans or song-specific coordinates. */
public class RestAwareTimingTest {
    private List<ScoreNoteEvent> phrase(boolean chord) {
        int[] flags = {1, 2, 2, 1, 1, 2};
        float[] silence = {.25f, .5f, .25f, .25f, .5f, .25f};
        var notes = new ArrayList<ScoreNoteEvent>();
        for (int i = 0; i < flags.length; i++) {
            var note =
                    new ScoreNoteEvent(
                            0,
                            .08f + i * .16f,
                            i,
                            0,
                            1,
                            .4f,
                            false,
                            0,
                            flags[i],
                            ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                            0,
                            1,
                            silence[i]);
            notes.add(note);
            if (chord)
                notes.add(
                        new ScoreNoteEvent(
                                0,
                                note.positionInMeasure(),
                                i + 7,
                                0,
                                1,
                                .4f,
                                false,
                                0,
                                flags[i],
                                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                                0,
                                1,
                                silence[i]));
        }
        return notes;
    }

    @Test
    public void silenceDoesNotVoteToLengthenAlreadyRecognizedFlags() {
        var notes = phrase(false);
        double[] expected = {.5, .25, .25, .5, .5, .25};
        for (int i = 0; i < notes.size(); i++)
            assertEquals(
                    expected[i],
                    ScoreNoteTiming.resolvedWrittenDurationBeats(notes.get(i), notes, 4),
                    .0001);
    }

    @Test
    public void chordSilenceIsCountedOncePerAttackNotOncePerHead() {
        var notes = phrase(true);
        double[] expected = {.5, .25, .25, .5, .5, .25};
        for (int i = 0; i < notes.size(); i++)
            assertEquals(
                    expected[i / 2],
                    ScoreNoteTiming.resolvedWrittenDurationBeats(notes.get(i), notes, 4),
                    .0001);
    }
}

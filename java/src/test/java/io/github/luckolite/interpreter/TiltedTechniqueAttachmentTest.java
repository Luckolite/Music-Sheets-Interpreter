// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class TiltedTechniqueAttachmentTest {
    private static ScoreNoteEvent note(float p, int step, float floor) {
        return new ScoreNoteEvent(
                0,
                p,
                step,
                0,
                1,
                (floor - step * 5) / 1000,
                false,
                0,
                0,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,
                1,
                0,
                0,
                0,
                ScoreNoteEvent.CLEF_TREBLE);
    }

    private static List<ScoreTechniqueChange> detect(List<ScoreNoteEvent> notes) {
        return PlayingTechniqueDetector.detect(
                List.of(new PlayingTechniqueDetector.Word("pizz.", .155f, .199f, .21f, .214f)),
                List.of(new PlayingTechniqueDetector.Staff(200, 240, 10, 0, 1)),
                List.of(new MeasureRegion(.1f, .8f, .19f, .31f)),
                notes,
                1000,
                1000);
    }

    @Test
    public void labelAboveLocalRulesAttachesBeforeFirstAffectedHead() {
        var result = detect(List.of(note(.1f, 2, 263), note(.4f, 5, 258.8f), note(.7f, 8, 254.6f)));
        assertEquals(1, result.size());
        assertEquals(ScoreTechniqueChange.PIZZICATO, result.get(0).technique());
        assertEquals(0, result.get(0).measureIndex());
        assertTrue(result.get(0).positionInMeasure() <= .1f);
    }

    @Test
    public void inconsistentOrTooFewWrittenHeadsDoNotMoveALabelIntoOwnership() {
        assertTrue(detect(List.of(note(.1f, 2, 263), note(.7f, 8, 254.6f))).isEmpty());
        assertTrue(
                detect(List.of(note(.1f, 2, 248), note(.4f, 5, 259), note(.7f, 8, 248))).isEmpty());
    }
}
